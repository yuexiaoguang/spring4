package org.springframework.mock.web.portlet;

import java.util.Collection;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

/**
 * {@link javax.portlet.RenderResponse}接口的模拟实现.
 */
public class MockRenderResponse extends MockMimeResponse implements RenderResponse {

	private String title;

	private Collection<PortletMode> nextPossiblePortletModes;


	/**
	 * 使用默认的{@link MockPortalContext}.
	 */
	public MockRenderResponse() {
		super();
	}

	/**
	 * @param portalContext 定义支持的PortletMode和WindowState的PortalContext
	 */
	public MockRenderResponse(PortalContext portalContext) {
		super(portalContext);
	}

	/**
	 * @param portalContext 定义支持的PortletMode和WindowState的PortalContext
	 * @param request 生成此响应的相应请求
	 */
	public MockRenderResponse(PortalContext portalContext, RenderRequest request) {
		super(portalContext, request);
	}


	//---------------------------------------------------------------------
	// RenderResponse methods
	//---------------------------------------------------------------------

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return this.title;
	}

	@Override
	public void setNextPossiblePortletModes(Collection<PortletMode> portletModes) {
		this.nextPossiblePortletModes = portletModes;
	}

	public Collection<PortletMode> getNextPossiblePortletModes() {
		return this.nextPossiblePortletModes;
	}

}
