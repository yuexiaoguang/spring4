package org.springframework.test.context.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@code TestExecutionListener}, 它为<em>Spring TestContext Framework</em>加载的
 * {@link WebApplicationContext WebApplicationContexts}提供模拟Servlet API支持.
 *
 * <p>具体来说, {@code ServletTestExecutionListener}在
 * {@linkplain #prepareTestInstance(TestContext) 准备测试实例}和{@linkplain #beforeTestMethod(TestContext) 每个测试方法之前}
 * 期间通过Spring Web的{@link RequestContextHolder}设置线程本地状态,
 * 并基于{@code WebApplicationContext}中存在的{@link MockServletContext}创建
 * {@link MockHttpServletRequest}, {@link MockHttpServletResponse}, {@link ServletWebRequest}.
 * 此监听器还确保可以将{@code MockHttpServletResponse} 和 {@code ServletWebRequest}注入到测试实例中,
 * 并且一旦测试完成, 此监听器就会{@linkplain #afterTestMethod(TestContext) 清除}线程本地状态.
 *
 * <p>请注意, {@code ServletTestExecutionListener}默认情况下处于启用状态,
 * 但如果{@linkplain TestContext#getTestClass() 测试类}未使用{@link WebAppConfiguration @WebAppConfiguration}注解,
 * 则通常不会执行任何操作.
 * 有关详细信息, 请参阅此类中各个方法的javadoc.
 */
public class ServletTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * {@link TestContext}属性的属性名称, 指示{@code ServletTestExecutionListener}是否应该
	 * {@linkplain RequestContextHolder#resetRequestAttributes() 重置}
	 * {@link #afterTestMethod(TestContext)}中 Spring Web的{@code RequestContextHolder}.
	 * <p>允许的值包括{@link Boolean#TRUE} 和 {@link Boolean#FALSE}.
	 */
	public static final String RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "resetRequestContextHolder");

	/**
	 * {@link TestContext}属性的属性名称,
	 * 表示{@code ServletTestExecutionListener}已经填充了Spring Web的{@code RequestContextHolder}.
	 * <p>允许的值包括{@link Boolean#TRUE} 和 {@link Boolean#FALSE}.
	 */
	public static final String POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "populatedRequestContextHolder");

	/**
	 * 请求属性的属性名称, 表示存储在Spring Web的{@link RequestContextHolder}中的
	 * {@link RequestAttributes}中的{@link MockHttpServletRequest}是由TestContext框架创建的.
	 * <p>允许的值包括{@link Boolean#TRUE} 和 {@link Boolean#FALSE}.
	 */
	public static final String CREATED_BY_THE_TESTCONTEXT_FRAMEWORK = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "createdByTheTestContextFramework");

	/**
	 * {@link TestContext}属性的属性名称, 表示应激活{@code ServletTestExecutionListener}.
	 * 如果未设置为{@code true}, 则给{@linkplain TestContext#getTestClass() 测试类}添加
	 * {@link WebAppConfiguration @WebAppConfiguration}注解时会发生激活.
	 * <p>允许的值包括{@link Boolean#TRUE} 和 {@link Boolean#FALSE}.
	 */
	public static final String ACTIVATE_LISTENER = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "activateListener");


	private static final Log logger = LogFactory.getLog(ServletTestExecutionListener.class);


	/**
	 * Returns {@code 1000}.
	 */
	@Override
	public final int getOrder() {
		return 1000;
	}

	/**
	 * 通过Spring Web的{@link RequestContextHolder}在<em>测试实例准备</em>回调阶段设置线程本地状态,
	 * 但前提是{@linkplain TestContext#getTestClass() 测试类}有{@link WebAppConfiguration @WebAppConfiguration}注解.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * 通过Spring Web的{@link RequestContextHolder}在每个测试方法之前设置线程本地状态,
	 * 但前提是{@linkplain TestContext#getTestClass() 测试类}有{@link WebAppConfiguration @WebAppConfiguration}注解.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * 如果提供的{@code TestContext}中的{@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE}的值为{@link Boolean#TRUE},
	 * 则此方法将
	 * (1) 通过{@linkplain RequestContextHolder#resetRequestAttributes() 重置}Spring Web的{@code RequestContextHolder}
	 * 清除每个测试方法后的线程本地状态;
	 * (2) 通过将测试上下文中的{@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * 设置为{@code true}, 确保将新的模拟注入到测试实例中以进行后续测试.
	 * <p>随后将从测试上下文中删除{@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE}
	 * 和{@link #POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE}, 无论其值如何.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE))) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Resetting RequestContextHolder for test context %s.", testContext));
			}
			RequestContextHolder.resetRequestAttributes();
			testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
				Boolean.TRUE);
		}
		testContext.removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		testContext.removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
	}

	private boolean isActivated(TestContext testContext) {
		return (Boolean.TRUE.equals(testContext.getAttribute(ACTIVATE_LISTENER)) ||
				AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), WebAppConfiguration.class));
	}

	private boolean alreadyPopulatedRequestContextHolder(TestContext testContext) {
		return Boolean.TRUE.equals(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE));
	}

	private void setUpRequestContextIfNecessary(TestContext testContext) {
		if (!isActivated(testContext) || alreadyPopulatedRequestContextHolder(testContext)) {
			return;
		}

		ApplicationContext context = testContext.getApplicationContext();

		if (context instanceof WebApplicationContext) {
			WebApplicationContext wac = (WebApplicationContext) context;
			ServletContext servletContext = wac.getServletContext();
			if (!(servletContext instanceof MockServletContext)) {
				throw new IllegalStateException(String.format(
						"The WebApplicationContext for test context %s must be configured with a MockServletContext.",
						testContext));
			}

			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Setting up MockHttpServletRequest, MockHttpServletResponse, ServletWebRequest, and RequestContextHolder for test context %s.",
						testContext));
			}

			MockServletContext mockServletContext = (MockServletContext) servletContext;
			MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
			request.setAttribute(CREATED_BY_THE_TESTCONTEXT_FRAMEWORK, Boolean.TRUE);
			MockHttpServletResponse response = new MockHttpServletResponse();
			ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

			RequestContextHolder.setRequestAttributes(servletWebRequest);
			testContext.setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
			testContext.setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

			if (wac instanceof ConfigurableApplicationContext) {
				@SuppressWarnings("resource")
				ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) wac;
				ConfigurableListableBeanFactory bf = configurableApplicationContext.getBeanFactory();
				bf.registerResolvableDependency(MockHttpServletResponse.class, response);
				bf.registerResolvableDependency(ServletWebRequest.class, servletWebRequest);
			}
		}
	}

}
