package org.springframework.mock.web;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

/**
 * {@link javax.servlet.jsp.tagext.BodyContent}类的模拟实现.
 *
 * <p>用于测试Web框架; 仅在测试自定义JSP标记时测试应用程序所必需的.
 */
public class MockBodyContent extends BodyContent {

	private final String content;


	/**
	 * @param content 要公开的主体内容
	 * @param response 要包装的servlet响应
	 */
	public MockBodyContent(String content, HttpServletResponse response) {
		this(content, response, null);
	}

	/**
	 * @param content 要公开的主体内容
	 * @param targetWriter 要包装的目标Writer
	 */
	public MockBodyContent(String content, Writer targetWriter) {
		this(content, null, targetWriter);
	}

	/**
	 * @param content 要公开的主体内容
	 * @param response 要包装的servlet响应
	 * @param targetWriter 要包装的目标Writer
	 */
	public MockBodyContent(String content, HttpServletResponse response, Writer targetWriter) {
		super(adaptJspWriter(targetWriter, response));
		this.content = content;
	}

	private static JspWriter adaptJspWriter(Writer targetWriter, HttpServletResponse response) {
		if (targetWriter instanceof JspWriter) {
			return (JspWriter) targetWriter;
		}
		else {
			return new MockJspWriter(response, targetWriter);
		}
	}


	@Override
	public Reader getReader() {
		return new StringReader(this.content);
	}

	@Override
	public String getString() {
		return this.content;
	}

	@Override
	public void writeOut(Writer writer) throws IOException {
		writer.write(this.content);
	}


	//---------------------------------------------------------------------
	// Delegating implementations of JspWriter's abstract methods
	//---------------------------------------------------------------------

	@Override
	public void clear() throws IOException {
		getEnclosingWriter().clear();
	}

	@Override
	public void clearBuffer() throws IOException {
		getEnclosingWriter().clearBuffer();
	}

	@Override
	public void close() throws IOException {
		getEnclosingWriter().close();
	}

	@Override
	public int getRemaining() {
		return getEnclosingWriter().getRemaining();
	}

	@Override
	public void newLine() throws IOException {
		getEnclosingWriter().println();
	}

	@Override
	public void write(char[] value, int offset, int length) throws IOException {
		getEnclosingWriter().write(value, offset, length);
	}

	@Override
	public void print(boolean value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(char value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(char[] value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(double value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(float value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(int value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(long value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(Object value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void print(String value) throws IOException {
		getEnclosingWriter().print(value);
	}

	@Override
	public void println() throws IOException {
		getEnclosingWriter().println();
	}

	@Override
	public void println(boolean value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(char value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(char[] value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(double value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(float value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(int value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(long value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(Object value) throws IOException {
		getEnclosingWriter().println(value);
	}

	@Override
	public void println(String value) throws IOException {
		getEnclosingWriter().println(value);
	}

}
