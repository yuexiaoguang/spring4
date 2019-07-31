package org.springframework.http.client.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.web.client.RestTemplate}的基类和其他HTTP访问网关助手,
 * 为{@link HttpAccessor}的常用属性添加拦截器相关属性.
 *
 * <p>不打算直接使用. See {@link org.springframework.web.client.RestTemplate}.
 */
public abstract class InterceptingHttpAccessor extends HttpAccessor {

	private List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();

	/**
	 * 设置此访问者应使用的请求拦截器.
	 */
	public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * 返回此访问者使用的请求拦截器.
	 */
	public List<ClientHttpRequestInterceptor> getInterceptors() {
		return interceptors;
	}

	@Override
	public ClientHttpRequestFactory getRequestFactory() {
		ClientHttpRequestFactory delegate = super.getRequestFactory();
		if (!CollectionUtils.isEmpty(getInterceptors())) {
			return new InterceptingClientHttpRequestFactory(delegate, getInterceptors());
		}
		else {
			return delegate;
		}
	}

}
