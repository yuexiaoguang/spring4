package org.springframework.messaging.simp.stomp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * 用于STOMP帧的编码器.
 */
public class StompEncoder  {

	private static final byte LF = '\n';

	private static final byte COLON = ':';

	private static final Log logger = LogFactory.getLog(StompEncoder.class);

	private static final int HEADER_KEY_CACHE_LIMIT = 32;


	private final Map<String, byte[]> headerKeyAccessCache =
			new ConcurrentHashMap<String, byte[]>(HEADER_KEY_CACHE_LIMIT);

	@SuppressWarnings("serial")
	private final Map<String, byte[]> headerKeyUpdateCache =
			new LinkedHashMap<String, byte[]>(HEADER_KEY_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
					if (size() > HEADER_KEY_CACHE_LIMIT) {
						headerKeyAccessCache.remove(eldest.getKey());
						return true;
					}
					else {
						return false;
					}
				}
			};


	/**
	 * 将给定的STOMP {@code message}编码为{@code byte[]}
	 * 
	 * @param message 要编码的消息
	 * 
	 * @return 已编码的消息
	 */
	public byte[] encode(Message<byte[]> message) {
		return encode(message.getHeaders(), message.getPayload());
	}

	/**
	 * 将给定的有效负载和header编码为{@code byte[]}.
	 * 
	 * @param headers header
	 * @param payload 有效负载
	 * 
	 * @return 已编码的消息
	 */
	public byte[] encode(Map<String, Object> headers, byte[] payload) {
		Assert.notNull(headers, "'headers' is required");
		Assert.notNull(payload, "'payload' is required");

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(128 + payload.length);
			DataOutputStream output = new DataOutputStream(baos);

			if (SimpMessageType.HEARTBEAT.equals(SimpMessageHeaderAccessor.getMessageType(headers))) {
				logger.trace("Encoding heartbeat");
				output.write(StompDecoder.HEARTBEAT_PAYLOAD);
			}

			else {
				StompCommand command = StompHeaderAccessor.getCommand(headers);
				if (command == null) {
					throw new IllegalStateException("Missing STOMP command: " + headers);
				}

				output.write(command.toString().getBytes(StompDecoder.UTF8_CHARSET));
				output.write(LF);
				writeHeaders(command, headers, payload, output);
				output.write(LF);
				writeBody(payload, output);
				output.write((byte) 0);
			}

			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw new StompConversionException("Failed to encode STOMP frame, headers=" + headers,  ex);
		}
	}

	private void writeHeaders(StompCommand command, Map<String, Object> headers, byte[] payload,
			DataOutputStream output) throws IOException {

		@SuppressWarnings("unchecked")
		Map<String,List<String>> nativeHeaders =
				(Map<String, List<String>>) headers.get(NativeMessageHeaderAccessor.NATIVE_HEADERS);

		if (logger.isTraceEnabled()) {
			logger.trace("Encoding STOMP " + command + ", headers=" + nativeHeaders);
		}

		if (nativeHeaders == null) {
			return;
		}

		boolean shouldEscape = (command != StompCommand.CONNECT && command != StompCommand.CONNECTED);

		for (Entry<String, List<String>> entry : nativeHeaders.entrySet()) {
			if (command.requiresContentLength() && "content-length".equals(entry.getKey())) {
				continue;
			}

			List<String> values = entry.getValue();
			if (StompCommand.CONNECT.equals(command) &&
					StompHeaderAccessor.STOMP_PASSCODE_HEADER.equals(entry.getKey())) {
				values = Collections.singletonList(StompHeaderAccessor.getPasscode(headers));
			}

			byte[] encodedKey = encodeHeaderKey(entry.getKey(), shouldEscape);
			for (String value : values) {
				output.write(encodedKey);
				output.write(COLON);
				output.write(encodeHeaderValue(value, shouldEscape));
				output.write(LF);
			}
		}

		if (command.requiresContentLength()) {
			int contentLength = payload.length;
			output.write("content-length:".getBytes(StompDecoder.UTF8_CHARSET));
			output.write(Integer.toString(contentLength).getBytes(StompDecoder.UTF8_CHARSET));
			output.write(LF);
		}
	}

	private byte[] encodeHeaderKey(String input, boolean escape) {
		String inputToUse = (escape ? escape(input) : input);
		if (this.headerKeyAccessCache.containsKey(inputToUse)) {
			return this.headerKeyAccessCache.get(inputToUse);
		}
		synchronized (this.headerKeyUpdateCache) {
			byte[] bytes = this.headerKeyUpdateCache.get(inputToUse);
			if (bytes == null) {
				bytes = inputToUse.getBytes(StompDecoder.UTF8_CHARSET);
				this.headerKeyAccessCache.put(inputToUse, bytes);
				this.headerKeyUpdateCache.put(inputToUse, bytes);
			}
			return bytes;
		}
	}

	private byte[] encodeHeaderValue(String input, boolean escape) {
		String inputToUse = (escape ? escape(input) : input);
		return inputToUse.getBytes(StompDecoder.UTF8_CHARSET);
	}

	/**
	 * See STOMP Spec 1.2:
	 * <a href="http://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"Value Encoding"</a>.
	 */
	private String escape(String inString) {
		StringBuilder sb = null;
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (c == '\\') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\\\");
			}
			else if (c == ':') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\c");
			}
			else if (c == '\n') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\n");
			}
			else if (c == '\r') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\r");
			}
			else if (sb != null){
				sb.append(c);
			}
		}
		return (sb != null ? sb.toString() : inString);
	}

	private StringBuilder getStringBuilder(StringBuilder sb, String inString, int i) {
		if (sb == null) {
			sb = new StringBuilder(inString.length());
			sb.append(inString.substring(0, i));
		}
		return sb;
	}

	private void writeBody(byte[] payload, DataOutputStream output) throws IOException {
		output.write(payload);
	}

}
