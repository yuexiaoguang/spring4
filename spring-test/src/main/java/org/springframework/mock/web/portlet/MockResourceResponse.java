package org.springframework.mock.web.portlet;

import javax.portlet.ResourceResponse;

/**
 * Mock implementation of the {@link javax.portlet.ResourceResponse} interface.
 */
public class MockResourceResponse extends MockMimeResponse implements ResourceResponse {

	private int contentLength = 0;


	@Override
	public void setContentLength(int len) {
		this.contentLength = len;
	}

	public int getContentLength() {
		return this.contentLength;
	}

}
