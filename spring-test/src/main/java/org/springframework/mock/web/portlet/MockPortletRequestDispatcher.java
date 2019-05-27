package org.springframework.mock.web.portlet;

import java.io.IOException;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.portlet.PortletRequestDispatcher} interface.
 */
public class MockPortletRequestDispatcher implements PortletRequestDispatcher {

	private final Log logger = LogFactory.getLog(getClass());

	private final String url;


	/**
	 * Create a new MockPortletRequestDispatcher for the given URL.
	 * @param url the URL to dispatch to.
	 */
	public MockPortletRequestDispatcher(String url) {
		Assert.notNull(url, "URL must not be null");
		this.url = url;
	}


	@Override
	public void include(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		include((PortletRequest) request, (PortletResponse) response);
	}

	@Override
	public void include(PortletRequest request, PortletResponse response) throws PortletException, IOException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		if (!(response instanceof MockMimeResponse)) {
			throw new IllegalArgumentException("MockPortletRequestDispatcher requires MockMimeResponse");
		}
		((MockMimeResponse) response).setIncludedUrl(this.url);
		if (logger.isDebugEnabled()) {
			logger.debug("MockPortletRequestDispatcher: including URL [" + this.url + "]");
		}
	}

	@Override
	public void forward(PortletRequest request, PortletResponse response) throws PortletException, IOException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		if (!(response instanceof MockMimeResponse)) {
			throw new IllegalArgumentException("MockPortletRequestDispatcher requires MockMimeResponse");
		}
		((MockMimeResponse) response).setForwardedUrl(this.url);
		if (logger.isDebugEnabled()) {
			logger.debug("MockPortletRequestDispatcher: forwarding to URL [" + this.url + "]");
		}
	}

}
