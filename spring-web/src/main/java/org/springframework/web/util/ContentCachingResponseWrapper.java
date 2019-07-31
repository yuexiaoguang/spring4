package org.springframework.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.util.FastByteArrayOutputStream;

/**
 * {@link javax.servlet.http.HttpServletResponse}包装器,
 * 用于缓存写入{@linkplain #getOutputStream() 输出流}和{@linkplain #getWriter() writer}的所有内容,
 * 并允许通过{@link #getContentAsByteArray() 字节数组}检索此内容.
 *
 * <p>由{@link org.springframework.web.filter.ShallowEtagHeaderFilter}使用.
 */
public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

	private final FastByteArrayOutputStream content = new FastByteArrayOutputStream(1024);

	private final ServletOutputStream outputStream = new ResponseServletOutputStream();

	private PrintWriter writer;

	private int statusCode = HttpServletResponse.SC_OK;

	private Integer contentLength;


	/**
	 * @param response 原始的servlet响应
	 */
	public ContentCachingResponseWrapper(HttpServletResponse response) {
		super(response);
	}


	@Override
	public void setStatus(int sc) {
		super.setStatus(sc);
		this.statusCode = sc;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setStatus(int sc, String sm) {
		super.setStatus(sc, sm);
		this.statusCode = sc;
	}

	@Override
	public void sendError(int sc) throws IOException {
		copyBodyToResponse(false);
		try {
			super.sendError(sc);
		}
		catch (IllegalStateException ex) {
			// 可能在Tomcat上调用太晚了: 回到沉默的setStatus
			super.setStatus(sc);
		}
		this.statusCode = sc;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void sendError(int sc, String msg) throws IOException {
		copyBodyToResponse(false);
		try {
			super.sendError(sc, msg);
		}
		catch (IllegalStateException ex) {
			// 可能在Tomcat上调用太晚了: 回到沉默的setStatus
			super.setStatus(sc, msg);
		}
		this.statusCode = sc;
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		copyBodyToResponse(false);
		super.sendRedirect(location);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.writer == null) {
			String characterEncoding = getCharacterEncoding();
			this.writer = (characterEncoding != null ? new ResponsePrintWriter(characterEncoding) :
					new ResponsePrintWriter(WebUtils.DEFAULT_CHARACTER_ENCODING));
		}
		return this.writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		// 不要将底层响应刷新为尚未复制到其中的内容
	}

	@Override
	public void setContentLength(int len) {
		if (len > this.content.size()) {
			this.content.resize(len);
		}
		this.contentLength = len;
	}

	// Overrides Servlet 3.1 setContentLengthLong(long) at runtime
	public void setContentLengthLong(long len) {
		if (len > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Content-Length exceeds ContentCachingResponseWrapper's maximum (" +
					Integer.MAX_VALUE + "): " + len);
		}
		int lenInt = (int) len;
		if (lenInt > this.content.size()) {
			this.content.resize(lenInt);
		}
		this.contentLength = lenInt;
	}

	@Override
	public void setBufferSize(int size) {
		if (size > this.content.size()) {
			this.content.resize(size);
		}
	}

	@Override
	public void resetBuffer() {
		this.content.reset();
	}

	@Override
	public void reset() {
		super.reset();
		this.content.reset();
	}

	/**
	 * 返回响应中指定的状态码.
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	/**
	 * 返回缓存的响应内容.
	 */
	public byte[] getContentAsByteArray() {
		return this.content.toByteArray();
	}

	/**
	 * 将{@link InputStream}返回到缓存的内容.
	 */
	public InputStream getContentInputStream() {
		return this.content.getInputStream();
	}

	/**
	 * 返回缓存内容的当前大小.
	 */
	public int getContentSize() {
		return this.content.size();
	}

	/**
	 * 将完整的缓存正文内容复制到响应中.
	 */
	public void copyBodyToResponse() throws IOException {
		copyBodyToResponse(true);
	}

	/**
	 * 将缓存的正文内容复制到响应中.
	 * 
	 * @param complete 是否为完整的缓存正文内容设置相应的内容长度
	 */
	protected void copyBodyToResponse(boolean complete) throws IOException {
		if (this.content.size() > 0) {
			HttpServletResponse rawResponse = (HttpServletResponse) getResponse();
			if ((complete || this.contentLength != null) && !rawResponse.isCommitted()) {
				rawResponse.setContentLength(complete ? this.content.size() : this.contentLength);
				this.contentLength = null;
			}
			this.content.writeTo(rawResponse.getOutputStream());
			this.content.reset();
			if (complete) {
				super.flushBuffer();
			}
		}
	}


	private class ResponseServletOutputStream extends ServletOutputStream {

		@Override
		public void write(int b) throws IOException {
			content.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			content.write(b, off, len);
		}
	}


	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
			super(new OutputStreamWriter(content, characterEncoding));
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
		}
	}

}
