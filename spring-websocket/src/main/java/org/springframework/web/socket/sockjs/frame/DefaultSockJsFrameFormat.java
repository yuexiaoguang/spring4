package org.springframework.web.socket.sockjs.frame;

import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.socket.sockjs.frame.SockJsFrameFormat}的默认实现,
 * 依赖于{@link java.lang.String#format(String, Object...)}.
 */
public class DefaultSockJsFrameFormat implements SockJsFrameFormat {

	private final String format;


	public DefaultSockJsFrameFormat(String format) {
		Assert.notNull(format, "format must not be null");
		this.format = format;
	}


	@Override
	public String format(SockJsFrame frame) {
		return String.format(this.format, preProcessContent(frame.getContent()));
	}

	protected String preProcessContent(String content) {
		return content;
	}
}
