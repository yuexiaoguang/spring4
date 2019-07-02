package org.springframework.test.web.servlet;

import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.core.NestedRuntimeException;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * MockMvc构建器实现的基类, 提供创建{@link MockMvc}实例的功能.
 *
 * <p>{@link org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder},
 * 派生自此类, 提供了一个具体的{@code build}方法, 并委托抽象方法来获取{@link WebApplicationContext}.
 */
public abstract class MockMvcBuilderSupport {

	@Deprecated
	protected final MockMvc createMockMvc(Filter[] filters, MockServletConfig servletConfig,
			WebApplicationContext webAppContext, RequestBuilder defaultRequestBuilder,
			List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
			Boolean dispatchOptions) {
		return createMockMvc(filters, servletConfig, webAppContext, defaultRequestBuilder,
				globalResultMatchers, globalResultHandlers,
				Collections.<DispatcherServletCustomizer>singletonList(new DispatchOptionsDispatcherServletCustomizer(dispatchOptions)));
	}

	protected final MockMvc createMockMvc(Filter[] filters, MockServletConfig servletConfig,
			WebApplicationContext webAppContext, RequestBuilder defaultRequestBuilder,
			List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
			List<DispatcherServletCustomizer> dispatcherServletCustomizers) {

		ServletContext servletContext = webAppContext.getServletContext();

		TestDispatcherServlet dispatcherServlet = new TestDispatcherServlet(webAppContext);
		if (dispatcherServletCustomizers != null) {
			for (DispatcherServletCustomizer customizers : dispatcherServletCustomizers) {
				customizers.customize(dispatcherServlet);
			}
		}
		try {
			dispatcherServlet.init(servletConfig);
		}
		catch (ServletException ex) {
			// should never happen..
			throw new MockMvcBuildException("Failed to initialize TestDispatcherServlet", ex);
		}

		MockMvc mockMvc = new MockMvc(dispatcherServlet, filters, servletContext);
		mockMvc.setDefaultRequest(defaultRequestBuilder);
		mockMvc.setGlobalResultMatchers(globalResultMatchers);
		mockMvc.setGlobalResultHandlers(globalResultHandlers);

		return mockMvc;
	}

	@SuppressWarnings("serial")
	private static class MockMvcBuildException extends NestedRuntimeException {

		public MockMvcBuildException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}

	private static class DispatchOptionsDispatcherServletCustomizer
			implements DispatcherServletCustomizer {
		private final Boolean dispatchOptions;

		private DispatchOptionsDispatcherServletCustomizer(Boolean dispatchOptions) {
			this.dispatchOptions = dispatchOptions;
		}

		@Override
		public void customize(DispatcherServlet dispatcherServlet) {
			dispatcherServlet.setDispatchOptionsRequest(this.dispatchOptions);
		}
	}

}
