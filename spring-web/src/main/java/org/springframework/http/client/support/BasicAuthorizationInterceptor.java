package org.springframework.http.client.support;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

/**
 * {@link ClientHttpRequestInterceptor} to apply a BASIC authorization header.
 */
public class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private final String username;

	private final String password;


	/**
	 * Create a new interceptor which adds a BASIC authorization header
	 * for the given username and password.
	 * @param username the username to use
	 * @param password the password to use
	 */
	public BasicAuthorizationInterceptor(String username, String password) {
		Assert.hasLength(username, "Username must not be empty");
		this.username = username;
		this.password = (password != null ? password : "");
	}


	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {

		String token = Base64Utils.encodeToString((this.username + ":" + this.password).getBytes(UTF_8));
		request.getHeaders().add("Authorization", "Basic " + token);
		return execution.execute(request, body);
	}

}
