package org.springframework.util;

import java.io.Writer;

import org.apache.commons.logging.Log;

/**
 * 用于Commons Logging {@code Log}的{@code java.io.Writer}适配器.
 */
public class CommonsLogWriter extends Writer {

	private final Log logger;

	private final StringBuilder buffer = new StringBuilder();


	/**
	 * @param logger 要写入的Commons Logging记录器
	 */
	public CommonsLogWriter(Log logger) {
		Assert.notNull(logger, "Logger must not be null");
		this.logger = logger;
	}


	public void write(char ch) {
		if (ch == '\n' && this.buffer.length() > 0) {
			this.logger.debug(this.buffer.toString());
			this.buffer.setLength(0);
		}
		else {
			this.buffer.append(ch);
		}
	}

	@Override
	public void write(char[] buffer, int offset, int length) {
		for (int i = 0; i < length; i++) {
			char ch = buffer[offset + i];
			if (ch == '\n' && this.buffer.length() > 0) {
				this.logger.debug(this.buffer.toString());
				this.buffer.setLength(0);
			}
			else {
				this.buffer.append(ch);
			}
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}

}
