package org.springframework.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpRequest} implementation that uses standard JDK facilities to
 * execute streaming requests. Created via the {@link SimpleClientHttpRequestFactory}.
 */
final class SimpleStreamingClientHttpRequest extends AbstractClientHttpRequest {

	private final HttpURLConnection connection;

	private final int chunkSize;

	private OutputStream body;

	private final boolean outputStreaming;


	SimpleStreamingClientHttpRequest(HttpURLConnection connection, int chunkSize, boolean outputStreaming) {
		this.connection = connection;
		this.chunkSize = chunkSize;
		this.outputStreaming = outputStreaming;
	}


	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.connection.getRequestMethod());
	}

	@Override
	public URI getURI() {
		try {
			return this.connection.getURL().toURI();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		if (this.body == null) {
			if (this.outputStreaming) {
				int contentLength = (int) headers.getContentLength();
				if (contentLength >= 0) {
					this.connection.setFixedLengthStreamingMode(contentLength);
				}
				else {
					this.connection.setChunkedStreamingMode(this.chunkSize);
				}
			}
			SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);
			this.connection.connect();
			this.body = this.connection.getOutputStream();
		}
		return StreamUtils.nonClosing(this.body);
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		try {
			if (this.body != null) {
				this.body.close();
			}
			else {
				SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);
				this.connection.connect();
				// Immediately trigger the request in a no-output scenario as well
				this.connection.getResponseCode();
			}
		}
		catch (IOException ex) {
			// ignore
		}
		return new SimpleClientHttpResponse(this.connection);
	}

}
