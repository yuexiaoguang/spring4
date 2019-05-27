package org.springframework.mock.web.portlet;

import javax.portlet.EventRequest;
import javax.portlet.EventResponse;

/**
 * Mock implementation of the {@link javax.portlet.EventResponse} interface.
 */
public class MockEventResponse extends MockStateAwareResponse implements EventResponse {

	@Override
	public void setRenderParameters(EventRequest request) {
		setRenderParameters(request.getParameterMap());
	}

}
