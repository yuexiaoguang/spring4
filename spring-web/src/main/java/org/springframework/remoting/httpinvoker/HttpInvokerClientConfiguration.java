package org.springframework.remoting.httpinvoker;

/**
 * Configuration interface for executing HTTP invoker requests.
 */
public interface HttpInvokerClientConfiguration {

	/**
	 * Return the HTTP URL of the target service.
	 */
	String getServiceUrl();

	/**
	 * Return the codebase URL to download classes from if not found locally.
	 * Can consist of multiple URLs, separated by spaces.
	 * @return the codebase URL, or {@code null} if none
	 */
	String getCodebaseUrl();

}
