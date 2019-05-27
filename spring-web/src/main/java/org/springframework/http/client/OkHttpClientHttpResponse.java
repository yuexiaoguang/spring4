package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;

import com.squareup.okhttp.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpResponse} implementation based on OkHttp 2.x.
 */
class OkHttpClientHttpResponse extends AbstractClientHttpResponse {

	private final Response response;

	private HttpHeaders headers;


	public OkHttpClientHttpResponse(Response response) {
		Assert.notNull(response, "Response must not be null");
		this.response = response;
	}


	@Override
	public int getRawStatusCode() {
		return this.response.code();
	}

	@Override
	public String getStatusText() {
		return this.response.message();
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.response.body().byteStream();
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			HttpHeaders headers = new HttpHeaders();
			for (String headerName : this.response.headers().names()) {
				for (String headerValue : this.response.headers(headerName)) {
					headers.add(headerName, headerValue);
				}
			}
			this.headers = headers;
		}
		return this.headers;
	}

	@Override
	public void close() {
		try {
			this.response.body().close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

}
