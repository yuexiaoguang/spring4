package org.springframework.mock.web.portlet;

import java.util.Collection;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

/**
 * Mock implementation of the {@link javax.portlet.RenderResponse} interface.
 */
public class MockRenderResponse extends MockMimeResponse implements RenderResponse {

	private String title;

	private Collection<PortletMode> nextPossiblePortletModes;


	/**
	 * Create a new MockRenderResponse with a default {@link MockPortalContext}.
	 * @see MockPortalContext
	 */
	public MockRenderResponse() {
		super();
	}

	/**
	 * Create a new MockRenderResponse.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 */
	public MockRenderResponse(PortalContext portalContext) {
		super(portalContext);
	}

	/**
	 * Create a new MockRenderResponse.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 * @param request the corresponding render request that this response
	 * is generated for
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
