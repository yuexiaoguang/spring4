package org.springframework.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;

/**
 * Generic callback interface used by {@link RestTemplate}'s retrieval methods
 * Implementations of this interface perform the actual work of extracting data
 * from a {@link ClientHttpResponse}, but don't need to worry about exception
 * handling or closing resources.
 *
 * <p>Used internally by the {@link RestTemplate}, but also useful for application code.
 */
public interface ResponseExtractor<T> {

	/**
	 * Extract data from the given {@code ClientHttpResponse} and return it.
	 * @param response the HTTP response
	 * @return the extracted data
	 * @throws IOException in case of I/O errors
	 */
	T extractData(ClientHttpResponse response) throws IOException;

}