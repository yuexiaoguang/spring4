package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link MockMvcConfigurer}的空实现.
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
