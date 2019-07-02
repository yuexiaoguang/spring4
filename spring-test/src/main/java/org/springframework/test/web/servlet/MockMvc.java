package org.springframework.test.web.servlet;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.springframework.beans.Mergeable;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <strong>服务器端Spring MVC测试支持的主要入口点.</strong>
 *
 * <h3>示例</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 *     .andExpect(status().isOk())
 *     .andExpect(content().mimeType("text/html"))
 *     .andExpect(forwardedUrl("/WEB-INF/layouts/main.jsp"));
 * </pre>
 */
public final class MockMvc {

	static final String MVC_RESULT_ATTRIBUTE = MockMvc.class.getName().concat(".MVC_RESULT_ATTRIBUTE");

	private final TestDispatcherServlet servlet;

	private final Filter[] filters;

	private final ServletContext servletContext;

	private RequestBuilder defaultRequestBuilder;

	private List<ResultMatcher> defaultResultMatchers = new ArrayList<ResultMatcher>();

	private List<ResultHandler> defaultResultHandlers = new ArrayList<ResultHandler>();


	MockMvc(TestDispatcherServlet servlet, Filter[] filters, ServletContext servletContext) {
		Assert.notNull(servlet, "DispatcherServlet is required");
		Assert.notNull(filters, "Filters cannot be null");
		Assert.noNullElements(filters, "Filters cannot contain null values");
		Assert.notNull(servletContext, "ServletContext is required");

		this.servlet = servlet;
		this.filters = filters;
		this.servletContext = servletContext;
	}


	/**
	 * 合并到每个执行的请求中的默认请求构建器.
	 */
	void setDefaultRequest(RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
	}

	/**
	 * 期望在每次执行请求后断言.
	 */
	void setGlobalResultMatchers(List<ResultMatcher> resultMatchers) {
		Assert.notNull(resultMatchers, "ResultMatcher List is required");
		this.defaultResultMatchers = resultMatchers;
	}

	/**
	 * 每次执行请求后应用的一般操作.
	 */
	void setGlobalResultHandlers(List<ResultHandler> resultHandlers) {
		Assert.notNull(resultHandlers, "ResultHandler List is required");
		this.defaultResultHandlers = resultHandlers;
	}

	/**
	 * 执行请求并返回一种类型, 该类型允许在结果上链接进一步的操作, 例如断言期望.
	 * 
	 * @param requestBuilder 用于准备执行的请求; 请参阅
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}中的静态工厂方法
	 * 
	 * @return {@link ResultActions}的实例 (never {@code null})
	 */
	public ResultActions perform(RequestBuilder requestBuilder) throws Exception {
		if (this.defaultRequestBuilder != null) {
			if (requestBuilder instanceof Mergeable) {
				requestBuilder = (RequestBuilder) ((Mergeable) requestBuilder).merge(this.defaultRequestBuilder);
			}
		}

		MockHttpServletRequest request = requestBuilder.buildRequest(this.servletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();

		if (requestBuilder instanceof SmartRequestBuilder) {
			request = ((SmartRequestBuilder) requestBuilder).postProcessRequest(request);
		}

		final MvcResult mvcResult = new DefaultMvcResult(request, response);
		request.setAttribute(MVC_RESULT_ATTRIBUTE, mvcResult);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

		MockFilterChain filterChain = new MockFilterChain(this.servlet, this.filters);
		filterChain.doFilter(request, response);

		if (DispatcherType.ASYNC.equals(request.getDispatcherType()) &&
				request.getAsyncContext() != null && !request.isAsyncStarted()) {
			request.getAsyncContext().complete();
		}

		applyDefaultResultActions(mvcResult);
		RequestContextHolder.setRequestAttributes(previousAttributes);

		return new ResultActions() {
			@Override
			public ResultActions andExpect(ResultMatcher matcher) throws Exception {
				matcher.match(mvcResult);
				return this;
			}
			@Override
			public ResultActions andDo(ResultHandler handler) throws Exception {
				handler.handle(mvcResult);
				return this;
			}
			@Override
			public MvcResult andReturn() {
				return mvcResult;
			}
		};
	}

	private void applyDefaultResultActions(MvcResult mvcResult) throws Exception {
		for (ResultMatcher matcher : this.defaultResultMatchers) {
			matcher.match(mvcResult);
		}
		for (ResultHandler handler : this.defaultResultHandlers) {
			handler.handle(mvcResult);
		}
	}
}
