package org.springframework.mock.web;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletInputStream;

import org.springframework.util.Assert;

/**
 * {@link javax.servlet.ServletInputStream}的委托实现.
 *
 * <p>由{@link MockHttpServletRequest}使用; 通常不直接用于测试应用程序控制器.
 */
public class DelegatingServletInputStream extends ServletInputStream {

	private final InputStream sourceStream;


	/**
	 * @param sourceStream 源流 (never {@code null})
	 */
	public DelegatingServletInputStream(InputStream sourceStream) {
		Assert.notNull(sourceStream, "Source InputStream must not be null");
		this.sourceStream = sourceStream;
	}

	/**
	 * 返回底层源流 (never {@code null}).
	 */
	public final InputStream getSourceStream() {
		return this.sourceStream;
	}


	@Override
	public int read() throws IOException {
		return this.sourceStream.read();
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.sourceStream.close();
	}

}
