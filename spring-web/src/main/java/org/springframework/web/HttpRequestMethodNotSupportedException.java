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
 * 当请求处理器不支持特定请求方法时抛出的异常.
 */
@SuppressWarnings("serial")
public class HttpRequestMethodNotSupportedException extends ServletException {

	private String method;

	private String[] supportedMethods;


	/**
	 * @param method 不支持的HTTP请求方法
	 */
	public HttpRequestMethodNotSupportedException(String method) {
		this(method, (String[]) null);
	}

	/**
	 * @param method 不支持的HTTP请求方法
	 * @param msg 详细信息
	 */
	public HttpRequestMethodNotSupportedException(String method, String msg) {
		this(method, null, msg);
	}

	/**
	 * @param method 不支持的HTTP请求方法
	 * @param supportedMethods 实际支持的HTTP方法 (may be {@code null})
	 */
	public HttpRequestMethodNotSupportedException(String method, Collection<String> supportedMethods) {
		this(method, StringUtils.toStringArray(supportedMethods));
	}

	/**
	 * @param method 不受支持的HTTP请求方法
	 * @param supportedMethods 实际支持的HTTP方法 (may be {@code null})
	 */
	public HttpRequestMethodNotSupportedException(String method, String[] supportedMethods) {
		this(method, supportedMethods, "Request method '" + method + "' not supported");
	}

	/**
	 * @param method 不受支持的HTTP请求方法
	 * @param supportedMethods 实际支持的HTTP方法
	 * @param msg 详细信息
	 */
	public HttpRequestMethodNotSupportedException(String method, String[] supportedMethods, String msg) {
		super(msg);
		this.method = method;
		this.supportedMethods = supportedMethods;
	}


	/**
	 * 返回导致失败的HTTP请求方法.
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * 返回实际支持的HTTP方法, 或{@code null}.
	 */
	public String[] getSupportedMethods() {
		return this.supportedMethods;
	}

	/**
	 * 返回实际支持的HTTP方法, 或{@code null}.
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
