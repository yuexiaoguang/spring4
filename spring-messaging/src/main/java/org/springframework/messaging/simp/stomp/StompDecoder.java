package org.springframework.messaging.simp.stomp;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MultiValueMap;

/**
 * 解码{@link ByteBuffer}中包含的一个或多个STOMP帧.
 *
 * <p>尝试从缓冲区读取所有完整的STOMP帧, 其可以是零, 一个或多个.
 * 如果存在任何遗留内容, i.e. 不完整的STOMP帧, 则在结束时重置缓冲区以指向部分内容的开头.
 * 然后, 调用者负责通过缓冲处理这个不完整的内容, 直到有更多可用输入为止.
 */
public class StompDecoder {

	static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	static final byte[] HEARTBEAT_PAYLOAD = new byte[] {'\n'};

	private static final Log logger = LogFactory.getLog(StompDecoder.class);

	private MessageHeaderInitializer headerInitializer;


	/**
	 * 配置{@link MessageHeaderInitializer}以从解码的STOMP帧应用于{@link Message}的header.
	 */
	public void setHeaderInitializer(MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回配置的{@code MessageHeaderInitializer}.
	 */
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	/**
	 * 将给定{@code ByteBuffer}中的一个或多个STOMP帧解码为{@link Message}.
	 * 如果输入缓冲区包含部分STOMP帧内容, 或具有部分STOMP帧的其他内容, 则重置缓冲区并返回{@code null}.
	 * 
	 * @param buffer 从中解码STOMP帧的缓冲区
	 * 
	 * @return 已解码的消息, 或空列表
	 * @throws StompConversionException 解码出错
	 */
	public List<Message<byte[]>> decode(ByteBuffer buffer) {
		return decode(buffer, null);
	}

	/**
	 * 从给定的{@code buffer}解码一个或多个STOMP帧, 并返回{@link Message}列表.
	 * <p>如果给定的ByteBuffer仅包含部分STOMP帧内容且没有完整的STOMP帧, 则返回一个空列表, 并将缓冲区重置为原来的位置.
	 * <p>如果缓冲区包含一个或多个STOMP帧, 则返回这些帧并重置缓冲区以指向未使用的部分内容的开头.
	 * <p>输出partialMessageHeader映射用于在部分内容的情况下成功存储已解析的header.
	 * 然后, 调用者可以检查是否读取了"content-length" header, 这有助于确定在下一次尝试解码之前需要多少内容.
	 * 
	 * @param buffer 从中解码STOMP帧的缓冲区
	 * @param partialMessageHeaders 空输出Map,
	 * 在部分消息内容的情况下, 如果部分缓冲区以部分STOMP帧结束, 将存储最后成功解析的partialMessageHeaders
	 * 
	 * @return 已解码的消息, 或空列表
	 * @throws StompConversionException 解码出错
	 */
	public List<Message<byte[]>> decode(ByteBuffer buffer, MultiValueMap<String, String> partialMessageHeaders) {
		List<Message<byte[]>> messages = new ArrayList<Message<byte[]>>();
		while (buffer.hasRemaining()) {
			Message<byte[]> message = decodeMessage(buffer, partialMessageHeaders);
			if (message != null) {
				messages.add(message);
			}
			else {
				break;
			}
		}
		return messages;
	}

	/**
	 * 将给定{@code buffer}中的单个STOMP帧解码为{@link Message}.
	 */
	private Message<byte[]> decodeMessage(ByteBuffer buffer, MultiValueMap<String, String> headers) {
		Message<byte[]> decodedMessage = null;
		skipLeadingEol(buffer);
		buffer.mark();

		String command = readCommand(buffer);
		if (command.length() > 0) {
			StompHeaderAccessor headerAccessor = null;
			byte[] payload = null;
			if (buffer.remaining() > 0) {
				StompCommand stompCommand = StompCommand.valueOf(command);
				headerAccessor = StompHeaderAccessor.create(stompCommand);
				initHeaders(headerAccessor);
				readHeaders(buffer, headerAccessor);
				payload = readPayload(buffer, headerAccessor);
			}
			if (payload != null) {
				if (payload.length > 0) {
					StompCommand stompCommand = headerAccessor.getCommand();
					if (stompCommand != null && !stompCommand.isBodyAllowed()) {
						throw new StompConversionException(stompCommand +
								" shouldn't have a payload: length=" + payload.length + ", headers=" + headers);
					}
				}
				headerAccessor.updateSimpMessageHeadersFromStompHeaders();
				headerAccessor.setLeaveMutable(true);
				decodedMessage = MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
				if (logger.isTraceEnabled()) {
					logger.trace("Decoded " + headerAccessor.getDetailedLogMessage(payload));
				}
			}
			else {
				logger.trace("Incomplete frame, resetting input buffer...");
				if (headers != null && headerAccessor != null) {
					String name = NativeMessageHeaderAccessor.NATIVE_HEADERS;
					@SuppressWarnings("unchecked")
					MultiValueMap<String, String> map = (MultiValueMap<String, String>) headerAccessor.getHeader(name);
					if (map != null) {
						headers.putAll(map);
					}
				}
				buffer.reset();
			}
		}
		else {
			StompHeaderAccessor headerAccessor = StompHeaderAccessor.createForHeartbeat();
			initHeaders(headerAccessor);
			headerAccessor.setLeaveMutable(true);
			decodedMessage = MessageBuilder.createMessage(HEARTBEAT_PAYLOAD, headerAccessor.getMessageHeaders());
			if (logger.isTraceEnabled()) {
				logger.trace("Decoded " + headerAccessor.getDetailedLogMessage(null));
			}
		}

		return decodedMessage;
	}

	private void initHeaders(StompHeaderAccessor headerAccessor) {
		MessageHeaderInitializer initializer = getHeaderInitializer();
		if (initializer != null) {
			initializer.initHeaders(headerAccessor);
		}
	}

	/**
	 * 在给定的ByteBuffer的开头跳过一个或多个EOL字符.
	 * 那些是STOMP心跳帧.
	 */
	protected void skipLeadingEol(ByteBuffer buffer) {
		while (true) {
			if (!tryConsumeEndOfLine(buffer)) {
				break;
			}
		}
	}

	private String readCommand(ByteBuffer buffer) {
		ByteArrayOutputStream command = new ByteArrayOutputStream(256);
		while (buffer.remaining() > 0 && !tryConsumeEndOfLine(buffer)) {
			command.write(buffer.get());
		}
		return new String(command.toByteArray(), UTF8_CHARSET);
	}

	private void readHeaders(ByteBuffer buffer, StompHeaderAccessor headerAccessor) {
		while (true) {
			ByteArrayOutputStream headerStream = new ByteArrayOutputStream(256);
			boolean headerComplete = false;
			while (buffer.hasRemaining()) {
				if (tryConsumeEndOfLine(buffer)) {
					headerComplete = true;
					break;
				}
				headerStream.write(buffer.get());
			}
			if (headerStream.size() > 0 && headerComplete) {
				String header = new String(headerStream.toByteArray(), UTF8_CHARSET);
				int colonIndex = header.indexOf(':');
				if (colonIndex <= 0) {
					if (buffer.remaining() > 0) {
						throw new StompConversionException("Illegal header: '" + header +
								"'. A header must be of the form <name>:[<value>].");
					}
				}
				else {
					String headerName = unescape(header.substring(0, colonIndex));
					String headerValue = unescape(header.substring(colonIndex + 1));
					try {
						headerAccessor.addNativeHeader(headerName, headerValue);
					}
					catch (InvalidMimeTypeException ex) {
						if (buffer.remaining() > 0) {
							throw ex;
						}
					}
				}
			}
			else {
				break;
			}
		}
	}

	/**
	 * See STOMP Spec 1.2:
	 * <a href="http://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"Value Encoding"</a>.
	 */
	private String unescape(String inString) {
		StringBuilder sb = new StringBuilder(inString.length());
		int pos = 0;  // position in the old string
		int index = inString.indexOf('\\');

		while (index >= 0) {
			sb.append(inString.substring(pos, index));
			if (index + 1 >= inString.length()) {
				throw new StompConversionException("Illegal escape sequence at index " + index + ": " + inString);
			}
			Character c = inString.charAt(index + 1);
			if (c == 'r') {
				sb.append('\r');
			}
			else if (c == 'n') {
				sb.append('\n');
			}
			else if (c == 'c') {
				sb.append(':');
			}
			else if (c == '\\') {
				sb.append('\\');
			}
			else {
				// should never happen
				throw new StompConversionException("Illegal escape sequence at index " + index + ": " + inString);
			}
			pos = index + 2;
			index = inString.indexOf('\\', pos);
		}

		sb.append(inString.substring(pos));
		return sb.toString();
	}

	private byte[] readPayload(ByteBuffer buffer, StompHeaderAccessor headerAccessor) {
		Integer contentLength;
		try {
			contentLength = headerAccessor.getContentLength();
		}
		catch (NumberFormatException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Ignoring invalid content-length: '" + headerAccessor);
			}
			contentLength = null;
		}

		if (contentLength != null && contentLength >= 0) {
			if (buffer.remaining() > contentLength) {
				byte[] payload = new byte[contentLength];
				buffer.get(payload);
				if (buffer.get() != 0) {
					throw new StompConversionException("Frame must be terminated with a null octet");
				}
				return payload;
			}
			else {
				return null;
			}
		}
		else {
			ByteArrayOutputStream payload = new ByteArrayOutputStream(256);
			while (buffer.remaining() > 0) {
				byte b = buffer.get();
				if (b == 0) {
					return payload.toByteArray();
				}
				else {
					payload.write(b);
				}
			}
		}
		return null;
	}

	/**
	 * 如果成功, 尝试读取EOL递增缓冲区位置.
	 * 
	 * @return 是否消费了EOL
	 */
	private boolean tryConsumeEndOfLine(ByteBuffer buffer) {
		if (buffer.remaining() > 0) {
			byte b = buffer.get();
			if (b == '\n') {
				return true;
			}
			else if (b == '\r') {
				if (buffer.remaining() > 0 && buffer.get() == '\n') {
					return true;
				}
				else {
					throw new StompConversionException("'\\r' must be followed by '\\n'");
				}
			}
			buffer.position(buffer.position() - 1);
		}
		return false;
	}

}
