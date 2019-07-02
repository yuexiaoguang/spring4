package org.springframework.mock.web.portlet;

import javax.portlet.EventRequest;
import javax.portlet.EventResponse;

/**
 * {@link javax.portlet.EventResponse}接口的模拟实现.
 */
public class MockEventResponse extends MockStateAwareResponse implements EventResponse {

	@Override
	public void setRenderParameters(EventRequest request) {
		setRenderParameters(request.getParameterMap());
	}

}
