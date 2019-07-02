package org.springframework.mock.web;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletOutputStream;

import org.springframework.util.Assert;

/**
 * {@link javax.servlet.ServletOutputStream}的委托实现.
 *
 * <p>由{@link MockHttpServletResponse}使用; 通常不直接用于测试应用程序控制器.
 */
public class DelegatingServletOutputStream extends ServletOutputStream {

	private final OutputStream targetStream;


	/**
	 * @param targetStream 目标流(never {@code null})
	 */
	public DelegatingServletOutputStream(OutputStream targetStream) {
		Assert.notNull(targetStream, "Target OutputStream must not be null");
		this.targetStream = targetStream;
	}

	/**
	 * 返回底层目标流 (never {@code null}).
	 */
	public final OutputStream getTargetStream() {
		return this.targetStream;
	}


	@Override
	public void write(int b) throws IOException {
		this.targetStream.write(b);
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		this.targetStream.flush();
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.targetStream.close();
	}

}
