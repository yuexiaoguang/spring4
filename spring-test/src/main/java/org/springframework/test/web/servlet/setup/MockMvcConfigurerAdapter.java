package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

/**
 * An empty method implementation of {@link MockMvcConfigurer}.
 */
public abstract class MockMvcConfigurerAdapter implements MockMvcConfigurer {

	@Override
	public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
	}

	@Override
	public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext cxt) {
		return null;
	}

}
