package org.springframework.remoting.support;

import org.springframework.beans.factory.InitializingBean;

/**
 * 通过URL访问远程服务的类的抽象基类.
 * 提供"serviceUrl" bean属性, 该属性被视为必需.
 */
public abstract class UrlBasedRemoteAccessor extends RemoteAccessor implements InitializingBean {

	private String serviceUrl;


	/**
	 * 设置此远程访问器的目标服务的URL.
	 * URL必须与特定远程处理提供程序的规则兼容.
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * 返回此远程访问器的目标服务的URL.
	 */
	public String getServiceUrl() {
		return this.serviceUrl;
	}


	@Override
	public void afterPropertiesSet() {
		if (getServiceUrl() == null) {
			throw new IllegalArgumentException("Property 'serviceUrl' is required");
		}
	}

}
