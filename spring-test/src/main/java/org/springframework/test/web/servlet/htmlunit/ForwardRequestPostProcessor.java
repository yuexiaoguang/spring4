package org.springframework.test.web.servlet.htmlunit;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;

final class ForwardRequestPostProcessor implements RequestPostProcessor {

	private final String forwardUrl;


	public ForwardRequestPostProcessor(String forwardUrl) {
		Assert.hasText(forwardUrl, "Forward URL must not be null or empty");
		this.forwardUrl = forwardUrl;
	}

	@Override
	public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
		request.setServletPath(this.forwardUrl);
		return request;
	}

}
