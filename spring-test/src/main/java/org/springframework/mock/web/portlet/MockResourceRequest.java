package org.springframework.mock.web.portlet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.RenderRequest;
import javax.portlet.ResourceRequest;

/**
 * {@link javax.portlet.ResourceRequest}接口的模拟实现.
 */
public class MockResourceRequest extends MockClientDataRequest implements ResourceRequest {

	private String resourceID;

	private String cacheability;

	private final Map<String, String[]> privateRenderParameterMap = new LinkedHashMap<String, String[]>();


	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 */
	public MockResourceRequest() {
		super();
	}

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 * 
	 * @param resourceID the resource id for this request
	 */
	public MockResourceRequest(String resourceID) {
		super();
		this.resourceID = resourceID;
	}

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 * 
	 * @param url 此请求的资源URL
	 */
	public MockResourceRequest(MockResourceURL url) {
		super();
		this.resourceID = url.getResourceID();
		this.cacheability = url.getCacheability();
	}

	/**
	 * 使用默认的{@link MockPortalContext}.
	 * 
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockResourceRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * @param portalContext 运行请求的PortalContext
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockResourceRequest(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
	}


	@Override
	protected String getLifecyclePhase() {
		return RESOURCE_PHASE;
	}

	public void setResourceID(String resourceID) {
		this.resourceID = resourceID;
	}

	@Override
	public String getResourceID() {
		return this.resourceID;
	}

	public void setCacheability(String cacheLevel) {
		this.cacheability = cacheLevel;
	}

	@Override
	public String getCacheability() {
		return this.cacheability;
	}

	@Override
	public String getETag() {
		return getProperty(RenderRequest.ETAG);
	}

	public void addPrivateRenderParameter(String key, String value) {
		this.privateRenderParameterMap.put(key, new String[] {value});
	}

	public void addPrivateRenderParameter(String key, String[] values) {
		this.privateRenderParameterMap.put(key, values);
	}

	@Override
	public Map<String, String[]> getPrivateRenderParameterMap() {
		return Collections.unmodifiableMap(this.privateRenderParameterMap);
	}

}
