package org.springframework.mock.web.portlet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.RenderRequest;
import javax.portlet.ResourceRequest;

/**
 * Mock implementation of the {@link javax.portlet.ResourceRequest} interface.
 */
public class MockResourceRequest extends MockClientDataRequest implements ResourceRequest {

	private String resourceID;

	private String cacheability;

	private final Map<String, String[]> privateRenderParameterMap = new LinkedHashMap<String, String[]>();


	/**
	 * Create a new MockResourceRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 */
	public MockResourceRequest() {
		super();
	}

	/**
	 * Create a new MockResourceRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param resourceID the resource id for this request
	 */
	public MockResourceRequest(String resourceID) {
		super();
		this.resourceID = resourceID;
	}

	/**
	 * Create a new MockResourceRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param url the resource URL for this request
	 */
	public MockResourceRequest(MockResourceURL url) {
		super();
		this.resourceID = url.getResourceID();
		this.cacheability = url.getCacheability();
	}

	/**
	 * Create a new MockResourceRequest with a default {@link MockPortalContext}.
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockResourceRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * Create a new MockResourceRequest.
	 * @param portalContext the PortalContext that the request runs in
	 * @param portletContext the PortletContext that the request runs in
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
