package org.springframework.web;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;

import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when a request handler does not support a
 * specific request method.
 */
@SuppressWarnings("serial")
public class HttpRequestMethodNotSupportedException extends ServletException {

	private String method;

	private String[] supportedMethods;


	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 */
	public HttpRequestMethodNotSupportedException(String method) {
		this(method, (String[]) null);
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param msg the detail message
	 */
	public HttpRequestMethodNotSupportedException(String method, String msg) {
		this(method, null, msg);
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods (may be {@code null})
	 */
	public HttpRequestMethodNotSupportedException(String method, Collection<String> supportedMethods) {
		this(method, StringUtils.toStringArray(supportedMethods));
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods (may be {@code null})
	 */
	public HttpRequestMethodNotSupportedException(String method, String[] supportedMethods) {
		this(method, supportedMethods, "Request method '" + method + "' not supported");
	}

	/**
	 * Create a new HttpRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods
	 * @param msg the detail message
	 */
	public HttpRequestMethodNotSupportedException(String method, String[] supportedMethods, String msg) {
		super(msg);
		this.method = method;
		this.supportedMethods = supportedMethods;
	}


	/**
	 * Return the HTTP request method that caused the failure.
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * Return the actually supported HTTP methods, or {@code null} if not known.
	 */
	public String[] getSupportedMethods() {
		return this.supportedMethods;
	}

	/**
	 * Return the actually supported HTTP methods as {@link HttpMethod} instances,
	 * or {@code null} if not known.
	 * @since 3.2
	 */
	public Set<HttpMethod> getSupportedHttpMethods() {
		if (this.supportedMethods == null) {
			return null;
		}
		List<HttpMethod> supportedMethods = new LinkedList<HttpMethod>();
		for (String value : this.supportedMethods) {
			HttpMethod resolved = HttpMethod.resolve(value);
			if (resolved != null) {
				supportedMethods.add(resolved);
			}
		}
		return EnumSet.copyOf(supportedMethods);
	}

}
