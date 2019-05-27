package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpStatus;

/**
 * Abstract base for {@link ClientHttpResponse}.
 */
public abstract class AbstractClientHttpResponse implements ClientHttpResponse {

	@Override
	public HttpStatus getStatusCode() throws IOException {
		return HttpStatus.valueOf(getRawStatusCode());
	}

}
