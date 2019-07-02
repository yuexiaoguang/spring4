package org.springframework.test.context.web.socket;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link ContextCustomizer}, 实例化一个新的{@link MockServerContainer},
 * 并将其存储在名为{@code "javax.websocket.server.ServerContainer"}的属性下的{@code ServletContext}中.
 */
class MockServerContainerContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		if (context instanceof WebApplicationContext) {
			WebApplicationContext wac = (WebApplicationContext) context;
			wac.getServletContext().setAttribute("javax.websocket.server.ServerContainer", new MockServerContainer());
		}
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other != null && getClass() == other.getClass()));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
