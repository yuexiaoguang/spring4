package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpStatus;

/**
 * {@link ClientHttpResponse}的抽象类.
 */
public abstract class AbstractClientHttpResponse implements ClientHttpResponse {

	@Override
	public HttpStatus getStatusCode() throws IOException {
		return HttpStatus.valueOf(getRawStatusCode());
	}

}
