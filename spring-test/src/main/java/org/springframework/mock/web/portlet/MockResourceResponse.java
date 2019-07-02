package org.springframework.mock.web.portlet;

import javax.portlet.ResourceResponse;

/**
 * {@link javax.portlet.ResourceResponse}接口的模拟实现.
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
