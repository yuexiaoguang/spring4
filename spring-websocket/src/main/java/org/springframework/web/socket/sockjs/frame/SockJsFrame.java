package org.springframework.web.socket.sockjs.frame;

import java.nio.charset.Charset;

import org.springframework.util.Assert;

/**
 * 表示SockJS帧. 提供工厂方法来创建SockJS帧.
 */
public class SockJsFrame {

	public static final Charset CHARSET = Charset.forName("UTF-8");

	private static final SockJsFrame OPEN_FRAME = new SockJsFrame("o");

	private static final SockJsFrame HEARTBEAT_FRAME = new SockJsFrame("h");

	private static final SockJsFrame CLOSE_GO_AWAY_FRAME = closeFrame(3000, "Go away!");

	private static final SockJsFrame CLOSE_ANOTHER_CONNECTION_OPEN_FRAME =
			closeFrame(2010, "Another connection still open");


	private final SockJsFrameType type;

	private final String content;


	/**
	 * @param content 内容 (必须是非空的, 并代表有效的SockJS帧)
	 */
	public SockJsFrame(String content) {
		Assert.hasText(content, "Content must not be empty");
		if ("o".equals(content)) {
			this.type = SockJsFrameType.OPEN;
			this.content = content;
		}
		else if ("h".equals(content)) {
			this.type = SockJsFrameType.HEARTBEAT;
			this.content = content;
		}
		else if (content.charAt(0) == 'a') {
			this.type = SockJsFrameType.MESSAGE;
			this.content = (content.length() > 1 ? content : "a[]");
		}
		else if (content.charAt(0) == 'm') {
			this.type = SockJsFrameType.MESSAGE;
			this.content = (content.length() > 1 ? content : "null");
		}
		else if (content.charAt(0) == 'c') {
			this.type = SockJsFrameType.CLOSE;
			this.content = (content.length() > 1 ? content : "c[]");
		}
		else {
			throw new IllegalArgumentException("Unexpected SockJS frame type in content \"" + content + "\"");
		}
	}


	/**
	 * 返回SockJS帧类型.
	 */
	public SockJsFrameType getType() {
		return this.type;
	}

	/**
	 * 返回SockJS帧内容 (never {@code null}).
	 */
	public String getContent() {
		return this.content;
	}

	/**
	 * 返回SockJS帧内容.
	 */
	public byte[] getContentBytes() {
		return this.content.getBytes(CHARSET);
	}

	/**
	 * 返回SockJS "message" 和 "close"帧中包含的数据.
	 * 否则, 对于不包含数据的SockJS "open"和"close"帧, 返回{@code null}.
	 */
	public String getFrameData() {
		if (getType() == SockJsFrameType.OPEN || getType() == SockJsFrameType.HEARTBEAT) {
			return null;
		}
		else {
			return getContent().substring(1);
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SockJsFrame)) {
			return false;
		}
		SockJsFrame otherFrame = (SockJsFrame) other;
		return (this.type.equals(otherFrame.type) && this.content.equals(otherFrame.content));
	}

	@Override
	public int hashCode() {
		return this.content.hashCode();
	}

	@Override
	public String toString() {
		String result = this.content;
		if (result.length() > 80) {
			result = result.substring(0, 80) + "...(truncated)";
		}
		return "SockJsFrame content='" + result.replace("\n", "\\n").replace("\r", "\\r") + "'";
	}


	public static SockJsFrame openFrame() {
		return OPEN_FRAME;
	}

	public static SockJsFrame heartbeatFrame() {
		return HEARTBEAT_FRAME;
	}

	public static SockJsFrame messageFrame(SockJsMessageCodec codec, String... messages) {
		String encoded = codec.encode(messages);
		return new SockJsFrame(encoded);
	}

	public static SockJsFrame closeFrameGoAway() {
		return CLOSE_GO_AWAY_FRAME;
	}

	public static SockJsFrame closeFrameAnotherConnectionOpen() {
		return CLOSE_ANOTHER_CONNECTION_OPEN_FRAME;
	}

	public static SockJsFrame closeFrame(int code, String reason) {
		return new SockJsFrame("c[" + code + ",\"" + reason + "\"]");
	}

}
