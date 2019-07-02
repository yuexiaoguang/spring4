package org.springframework.mock.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

/**
 * {@link HttpOutputMessage}的模拟实现.
 */
public class MockHttpOutputMessage implements HttpOutputMessage {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private final HttpHeaders headers = new HttpHeaders();

	private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);


	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.body;
	}

	public byte[] getBodyAsBytes() {
		return this.body.toByteArray();
	}

	/**
	 * 返回转换为UTF-8字符串的正文内容.
	 */
	public String getBodyAsString() {
		return getBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * 返回正文内容.
	 * 
	 * @param charset 用于将body内容转换为String的字符集
	 */
	public String getBodyAsString(Charset charset) {
		byte[] bytes = getBodyAsBytes();
		try {
			return new String(bytes, charset.name());
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new IllegalStateException(ex);
		}
	}

}
