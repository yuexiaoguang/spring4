package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * {@link UriTemplateHandler}实现的抽象基类.
 *
 * <p>支持{@link #setBaseUrl}和{@link #setDefaultUriVariables}属性,
 * 无论URI模板扩展和子类中使用的编码机制如何都应该相关.
 */
public abstract class AbstractUriTemplateHandler implements UriTemplateHandler {

	private String baseUrl;

	private final Map<String, Object> defaultUriVariables = new HashMap<String, Object>();


	/**
	 * 配置基础URL 前缀.
	 * 基本URL必须具有scheme和主机, 但可以选择包含端口和路径.
	 * 基本URL必须完全展开和编码, 可以通过{@link UriComponentsBuilder}完成.
	 * 
	 * @param baseUrl 基本URL.
	 */
	public void setBaseUrl(String baseUrl) {
		if (baseUrl != null) {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUrl).build();
			Assert.hasText(uriComponents.getScheme(), "'baseUrl' must have a scheme");
			Assert.hasText(uriComponents.getHost(), "'baseUrl' must have a host");
			Assert.isNull(uriComponents.getQuery(), "'baseUrl' cannot have a query");
			Assert.isNull(uriComponents.getFragment(), "'baseUrl' cannot have a fragment");
		}
		this.baseUrl = baseUrl;
	}

	/**
	 * 返回配置的基本URL.
	 */
	public String getBaseUrl() {
		return this.baseUrl;
	}

	/**
	 * 配置默认URI变量值以用于每个扩展的URI模板.
	 * 这些默认值仅在使用Map进行扩展时适用, 而不适用于数组, 其中提供给{@link #expand(String, Map)}的Map可以覆盖默认值.
	 * 
	 * @param defaultUriVariables 默认的URI变量值
	 */
	public void setDefaultUriVariables(Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables.clear();
		if (defaultUriVariables != null) {
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * 返回配置的默认URI变量的只读副本.
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}


	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		if (!getDefaultUriVariables().isEmpty()) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.putAll(getDefaultUriVariables());
			map.putAll(uriVariables);
			uriVariables = map;
		}
		URI url = expandInternal(uriTemplate, uriVariables);
		return insertBaseUrl(url);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariables) {
		URI url = expandInternal(uriTemplate, uriVariables);
		return insertBaseUrl(url);
	}


	/**
	 * 实际扩展和编码URI模板.
	 */
	protected abstract URI expandInternal(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * 实际扩展和编码URI模板.
	 */
	protected abstract URI expandInternal(String uriTemplate, Object... uriVariables);


	/**
	 * 插入基本URL (如果已配置), 除非给定的URL已有主机.
	 */
	private URI insertBaseUrl(URI url) {
		try {
			String baseUrl = getBaseUrl();
			if (baseUrl != null && url.getHost() == null) {
				url = new URI(baseUrl + url.toString());
			}
			return url;
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("Invalid URL after inserting base URL: " + url, ex);
		}
	}

}
