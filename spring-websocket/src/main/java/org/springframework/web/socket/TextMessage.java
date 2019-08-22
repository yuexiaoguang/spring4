package org.springframework.web.socket;

import java.nio.charset.Charset;

/**
 * 文本WebSocket消息.
 */
public final class TextMessage extends AbstractWebSocketMessage<String> {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private final byte[] bytes;


	/**
	 * @param payload 非null有效负载
	 */
	public TextMessage(CharSequence payload) {
		super(payload.toString(), true);
		this.bytes = null;
	}

	/**
	 * 假设字节数组可以编码为UTF-8字符串.
	 * 
	 * @param payload 非null有效负载
	 */
	public TextMessage(byte[] payload) {
		super(new String(payload, UTF8_CHARSET));
		this.bytes = payload;
	}

	/**
	 * 创建一个新的文本WebSocket消息, 其中给定的有效负载表示完整或部分消息内容.
	 * 当{@code isLast} boolean标志设置为{@code false}时, 消息将作为部分内容发送,
	 * 并且在设置为{@code true}之前, 将会出现更多部分消息.
	 * 
	 * @param payload 非null有效负载
	 * @param isLast 是否是一系列部分消息的最后一部分
	 */
	public TextMessage(CharSequence payload, boolean isLast) {
		super(payload.toString(), isLast);
		this.bytes = null;
	}


	@Override
	public int getPayloadLength() {
		return asBytes().length;
	}

	public byte[] asBytes() {
		return (this.bytes != null ? this.bytes : getPayload().getBytes(UTF8_CHARSET));
	}

	@Override
	protected String toStringPayload() {
		String payload = getPayload();
		return (payload.length() > 10 ? payload.substring(0, 10) + ".." : payload);
	}

}
