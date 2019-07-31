package org.springframework.http.client.support;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.client.AsyncRestTemplate}的基类和其他HTTP访问网关助手,
 * 定义常用属性, 例如要操作的{@link AsyncClientHttpRequestFactory}.
 *
 * <p>不打算直接使用.See {@link org.springframework.web.client.AsyncRestTemplate}.
 */
public class AsyncHttpAccessor {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private AsyncClientHttpRequestFactory asyncRequestFactory;


	/**
	 * 设置此访问者用于获取{@link org.springframework.http.client.ClientHttpRequest HttpRequests}的请求工厂.
	 */
	public void setAsyncRequestFactory(AsyncClientHttpRequestFactory asyncRequestFactory) {
		Assert.notNull(asyncRequestFactory, "AsyncClientHttpRequestFactory must not be null");
		this.asyncRequestFactory = asyncRequestFactory;
	}

	/**
	 * 返回此访问者用于获取{@link org.springframework.http.client.ClientHttpRequest HttpRequests}的请求工厂.
	 */
	public AsyncClientHttpRequestFactory getAsyncRequestFactory() {
		return this.asyncRequestFactory;
	}

	/**
	 * 通过此模板的{@link AsyncClientHttpRequestFactory}创建一个新的{@link AsyncClientHttpRequest}.
	 * 
	 * @param url 要连接到的URL
	 * @param method 要执行的HTTP方法 (GET, POST, etc.)
	 * 
	 * @return 创建的请求
	 * @throws IOException
	 */
	protected AsyncClientHttpRequest createAsyncRequest(URI url, HttpMethod method) throws IOException {
		AsyncClientHttpRequest request = getAsyncRequestFactory().createAsyncRequest(url, method);
		if (logger.isDebugEnabled()) {
			logger.debug("Created asynchronous " + method.name() + " request for \"" + url + "\"");
		}
		return request;
	}

}
