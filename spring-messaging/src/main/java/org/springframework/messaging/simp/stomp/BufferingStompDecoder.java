package org.springframework.messaging.simp.stomp;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link org.springframework.messaging.simp.stomp.StompDecoder}的扩展,
 * 在父类从中读取所有 (完整) STOMP帧之后, 缓冲输入ByteBuffer中剩余的内容.
 * 剩余内容表示不完整的STOMP帧.
 * 当使用额外的数据重复调用时, decode方法返回一个或多个消息, 或者如果仍然没有足够的数据, 则继续缓冲.
 *
 * <p>只要解码没有失败, 就可以重复调用此解码器的单个实例以从单个流 (e.g. WebSocket会话)读取所有消息.
 * 如果存在异常, 则不应再使用StompDecoder实例, 因为其内部状态不保证一致.
 * 预计底层会话将在此时关闭.
 */
public class BufferingStompDecoder {

	private final StompDecoder stompDecoder;

	private final int bufferSizeLimit;

	private final Queue<ByteBuffer> chunks = new LinkedBlockingQueue<ByteBuffer>();

	private volatile Integer expectedContentLength;


	/**
	 * @param stompDecoder 要包装的目标解码器
	 * @param bufferSizeLimit 缓冲区大小限制
	 */
	public BufferingStompDecoder(StompDecoder stompDecoder, int bufferSizeLimit) {
		Assert.notNull(stompDecoder, "StompDecoder is required");
		Assert.isTrue(bufferSizeLimit > 0, "Buffer size limit must be greater than 0");
		this.stompDecoder = stompDecoder;
		this.bufferSizeLimit = bufferSizeLimit;
	}


	/**
	 * 返回包装的{@link StompDecoder}.
	 */
	public final StompDecoder getStompDecoder() {
		return this.stompDecoder;
	}

	/**
	 * 返回配置的缓冲区大小限制.
	 */
	public final int getBufferSizeLimit() {
		return this.bufferSizeLimit;
	}


	/**
	 * 将给定{@code ByteBuffer}中的一个或多个STOMP帧解码为{@link Message}的列表.
	 * <p>如果有足够的数据来解析"content-length" header, 则该值用于确定在进行新的解码尝试之前需要多少数据.
	 * <p>如果没有足够的数据来解析"content-length", 或者存在"content-length" header,
	 * 则每次后续解码调用都会尝试使用所有可用数据再次解析.
	 * 因此"content-length" header的存在有助于优化大消息的解码.
	 * 
	 * @param newBuffer 包含要解码的新数据的缓冲区
	 * 
	 * @return 已解码的消息或空列表
	 * @throws StompConversionException 解码出错
	 */
	public List<Message<byte[]>> decode(ByteBuffer newBuffer) {
		this.chunks.add(newBuffer);
		checkBufferLimits();

		if (this.expectedContentLength != null && getBufferSize() < this.expectedContentLength) {
			return Collections.<Message<byte[]>>emptyList();
		}

		ByteBuffer bufferToDecode = assembleChunksAndReset();
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		List<Message<byte[]>> messages = this.stompDecoder.decode(bufferToDecode, headers);

		if (bufferToDecode.hasRemaining()) {
			this.chunks.add(bufferToDecode);
			this.expectedContentLength = StompHeaderAccessor.getContentLength(headers);
		}

		return messages;
	}

	private ByteBuffer assembleChunksAndReset() {
		ByteBuffer result;
		if (this.chunks.size() == 1) {
			result = this.chunks.remove();
		}
		else {
			result = ByteBuffer.allocate(getBufferSize());
			for (ByteBuffer partial : this.chunks) {
				result.put(partial);
			}
			result.flip();
		}
		this.chunks.clear();
		this.expectedContentLength = null;
		return result;
	}

	private void checkBufferLimits() {
		if (this.expectedContentLength != null) {
			if (this.expectedContentLength > this.bufferSizeLimit) {
				throw new StompConversionException(
						"STOMP 'content-length' header value " + this.expectedContentLength +
						"  exceeds configured buffer size limit " + this.bufferSizeLimit);
			}
		}
		if (getBufferSize() > this.bufferSizeLimit) {
			throw new StompConversionException("The configured STOMP buffer size limit of " +
					this.bufferSizeLimit + " bytes has been exceeded");
		}
	}

	/**
	 * 计算当前缓冲区大小.
	 */
	public int getBufferSize() {
		int size = 0;
		for (ByteBuffer buffer : this.chunks) {
			size = size + buffer.remaining();
		}
		return size;
	}

	/**
	 * 获取当前缓冲的不完整STOMP帧的预期内容长度.
	 */
	public Integer getExpectedContentLength() {
		return this.expectedContentLength;
	}

}
