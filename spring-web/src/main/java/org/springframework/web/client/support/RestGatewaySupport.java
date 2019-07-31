package org.springframework.web.client.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * 方便的超类, 适用于需要REST访问的应用程序类.
 *
 * <p>需要设置{@link ClientHttpRequestFactory}或{@link RestTemplate}实例.
 */
public class RestGatewaySupport {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;


	public RestGatewaySupport() {
		this.restTemplate = new RestTemplate();
	}

	public RestGatewaySupport(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "'requestFactory' must not be null");
		this.restTemplate = new RestTemplate(requestFactory);
	}


	public void setRestTemplate(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		this.restTemplate = restTemplate;
	}

	public RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

}
