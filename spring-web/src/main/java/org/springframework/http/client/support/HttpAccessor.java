package org.springframework.http.client.support;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.client.RestTemplate}的基类和其他HTTP访问网关助手,
 * 定义常用属性, 例如要操作的{@link ClientHttpRequestFactory}.
 *
 * <p>不打算直接使用. See {@link org.springframework.web.client.RestTemplate}.
 */
public abstract class HttpAccessor {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();


	/**
	 * 设置此访问者用于获取客户端请求句柄的请求工厂.
	 * <p>默认值是基于JDK自己的HTTP库的{@link SimpleClientHttpRequestFactory} ({@link java.net.HttpURLConnection}).
	 * <p><b>请注意, 标准JDK HTTP库不支持HTTP PATCH方法.
	 * 配置Apache HttpComponents或OkHttp请求工厂以启用PATCH.</b>
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
		this.requestFactory = requestFactory;
	}

	/**
	 * 返回此访问者用于获取客户端请求句柄的请求工厂.
	 */
	public ClientHttpRequestFactory getRequestFactory() {
		return this.requestFactory;
	}


	/**
	 * 通过此模板的{@link ClientHttpRequestFactory}创建一个新的{@link ClientHttpRequest}.
	 * 
	 * @param url 要连接的URL
	 * @param method 要执行的HTTP方法 (GET, POST, etc)
	 * 
	 * @return 创建的请求
	 * @throws IOException
	 */
	protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
		ClientHttpRequest request = getRequestFactory().createRequest(url, method);
		if (logger.isDebugEnabled()) {
			logger.debug("Created " + method.name() + " request for \"" + url + "\"");
		}
		return request;
	}

}
