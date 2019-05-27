package org.springframework.mock.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

/**
 * Mock implementation of {@link HttpOutputMessage}.
 */
public class MockHttpOutputMessage implements HttpOutputMessage {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private final HttpHeaders headers = new HttpHeaders();

	private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);


	/**
	 * Return the headers.
	 */
	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Return the body content.
	 */
	@Override
	public OutputStream getBody() throws IOException {
		return this.body;
	}

	/**
	 * Return body content as a byte array.
	 */
	public byte[] getBodyAsBytes() {
		return this.body.toByteArray();
	}

	/**
	 * Return the body content interpreted as a UTF-8 string.
	 */
	public String getBodyAsString() {
		return getBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * Return the body content as a string.
	 * @param charset the charset to use to turn the body content to a String
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
