package org.springframework.test.web.servlet;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * A sub-class of {@code DispatcherServlet} that saves the result in an
 * {@link MvcResult}. The {@code MvcResult} instance is expected to be available
 * as the request attribute {@link MockMvc#MVC_RESULT_ATTRIBUTE}.
 */
@SuppressWarnings("serial")
final class TestDispatcherServlet extends DispatcherServlet {

	private static final String KEY = TestDispatcherServlet.class.getName() + ".interceptor";


	/**
	 * Create a new instance with the given web application context.
	 */
	public TestDispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
	}


	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		registerAsyncResultInterceptors(request);

		super.service(request, response);

		if (request.getAsyncContext() != null) {
			MockHttpServletRequest mockRequest = WebUtils.getNativeRequest(request, MockHttpServletRequest.class);
			Assert.notNull(mockRequest, "Expected MockHttpServletRequest");
			MockAsyncContext mockAsyncContext = ((MockAsyncContext) mockRequest.getAsyncContext());
			Assert.notNull(mockAsyncContext, "MockAsyncContext not found. Did request wrapper not delegate startAsync?");

			final CountDownLatch dispatchLatch = new CountDownLatch(1);
			mockAsyncContext.addDispatchHandler(new Runnable() {
				@Override
				public void run() {
					dispatchLatch.countDown();
				}
			});
			getMvcResult(request).setAsyncDispatchLatch(dispatchLatch);
		}
	}

	private void registerAsyncResultInterceptors(final HttpServletRequest request) {
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(KEY, new CallableProcessingInterceptorAdapter() {
			@Override
			public <T> void postProcess(NativeWebRequest r, Callable<T> task, Object value) throws Exception {
				getMvcResult(request).setAsyncResult(value);
			}
		});
		asyncManager.registerDeferredResultInterceptor(KEY, new DeferredResultProcessingInterceptorAdapter() {
			@Override
			public <T> void postProcess(NativeWebRequest r, DeferredResult<T> result, Object value) throws Exception {
				getMvcResult(request).setAsyncResult(value);
			}
		});
	}

	protected DefaultMvcResult getMvcResult(ServletRequest request) {
		return (DefaultMvcResult) request.getAttribute(MockMvc.MVC_RESULT_ATTRIBUTE);
	}

	@Override
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		HandlerExecutionChain chain = super.getHandler(request);
		if (chain != null) {
			DefaultMvcResult mvcResult = getMvcResult(request);
			mvcResult.setHandler(chain.getHandler());
			mvcResult.setInterceptors(chain.getInterceptors());
		}
		return chain;
	}

	@Override
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		DefaultMvcResult mvcResult = getMvcResult(request);
		mvcResult.setModelAndView(mv);
		super.render(mv, request, response);
	}

	@Override
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		ModelAndView mav = super.processHandlerException(request, response, handler, ex);

		// We got this far, exception was processed..
		DefaultMvcResult mvcResult = getMvcResult(request);
		mvcResult.setResolvedException(ex);
		mvcResult.setModelAndView(mav);

		return mav;
	}

}
