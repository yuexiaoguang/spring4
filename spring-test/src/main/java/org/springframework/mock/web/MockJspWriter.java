package org.springframework.mock.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

/**
 * {@link javax.servlet.jsp.JspWriter}类的模拟实现.
 *
 * <p>用于测试Web框架; 仅在测试自定义JSP标记时测试应用程序所必需的.
 */
public class MockJspWriter extends JspWriter {

	private final HttpServletResponse response;

	private PrintWriter targetWriter;


	/**
	 * @param response 要包装的servlet响应
	 */
	public MockJspWriter(HttpServletResponse response) {
		this(response, null);
	}

	/**
	 * @param targetWriter 要包装的目标Writer
	 */
	public MockJspWriter(Writer targetWriter) {
		this(null, targetWriter);
	}

	/**
	 * @param response 要包装的servlet响应
	 * @param targetWriter 要包装的目标Writer
	 */
	public MockJspWriter(HttpServletResponse response, Writer targetWriter) {
		super(DEFAULT_BUFFER, true);
		this.response = (response != null ? response : new MockHttpServletResponse());
		if (targetWriter instanceof PrintWriter) {
			this.targetWriter = (PrintWriter) targetWriter;
		}
		else if (targetWriter != null) {
			this.targetWriter = new PrintWriter(targetWriter);
		}
	}

	/**
	 * 延迟初始化目标Writer.
	 */
	protected PrintWriter getTargetWriter() throws IOException {
		if (this.targetWriter == null) {
			this.targetWriter = this.response.getWriter();
		}
		return this.targetWriter;
	}


	@Override
	public void clear() throws IOException {
		if (this.response.isCommitted()) {
			throw new IOException("Response already committed");
		}
		this.response.resetBuffer();
	}

	@Override
	public void clearBuffer() throws IOException {
	}

	@Override
	public void flush() throws IOException {
		this.response.flushBuffer();
	}

	@Override
	public void close() throws IOException {
		flush();
	}

	@Override
	public int getRemaining() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void newLine() throws IOException {
		getTargetWriter().println();
	}

	@Override
	public void write(char[] value, int offset, int length) throws IOException {
		getTargetWriter().write(value, offset, length);
	}

	@Override
	public void print(boolean value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(char value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(char[] value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(double value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(float value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(int value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(long value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(Object value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void print(String value) throws IOException {
		getTargetWriter().print(value);
	}

	@Override
	public void println() throws IOException {
		getTargetWriter().println();
	}

	@Override
	public void println(boolean value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(char value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(char[] value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(double value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(float value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(int value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(long value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(Object value) throws IOException {
		getTargetWriter().println(value);
	}

	@Override
	public void println(String value) throws IOException {
		getTargetWriter().println(value);
	}

}
