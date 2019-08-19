package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.ui.ModelMap;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.method.annotation.MapMethodProcessor;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * 通过{@code @ExceptionHandler}方法解析异常的{@link AbstractHandlerMethodExceptionResolver}.
 *
 * <p>可以通过{@link #setCustomArgumentResolvers} 和 {@link #setCustomReturnValueHandlers}
 * 添加对自定义参数和返回值类型的支持.
 * 或者, 使用{@link #setArgumentResolvers} 和 {@link #setReturnValueHandlers(List)}
 * 重新配置所有参数和返回值类型.
 */
public class ExceptionHandlerExceptionResolver extends AbstractHandlerMethodExceptionResolver
		implements ApplicationContextAware, InitializingBean {

	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private HandlerMethodArgumentResolverComposite argumentResolvers;

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	private List<HttpMessageConverter<?>> messageConverters;

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private final List<Object> responseBodyAdvice = new ArrayList<Object>();

	private ApplicationContext applicationContext;

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver>(64);

	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<ControllerAdviceBean, ExceptionHandlerMethodResolver>();


	public ExceptionHandlerExceptionResolver() {
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);  // see SPR-7316

		this.messageConverters = new ArrayList<HttpMessageConverter<?>>();
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		this.messageConverters.add(stringHttpMessageConverter);
		this.messageConverters.add(new SourceHttpMessageConverter<Source>());
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
	}


	/**
	 * 提供自定义参数类型的解析器.
	 * 在内置的解析器之后排序自定义解析器.
	 * 要覆盖对参数解析的内置支持, 改用{@link #setArgumentResolvers}.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers= argumentResolvers;
	}

	/**
	 * 返回自定义参数解析器, 或{@code null}.
	 */
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * 配置支持的参数类型的完整列表, 从而覆盖默认配置的解析器.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers = null;
		}
		else {
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.argumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * 返回已配置的参数解析器, 如果尚未通过{@link #afterPropertiesSet()}初始化, 则可能返回{@code null}.
	 */
	public HandlerMethodArgumentResolverComposite getArgumentResolvers() {
		return this.argumentResolvers;
	}

	/**
	 * 提供自定义返回值类型的处理器. 自定义处理器在内置处理器之后排序.
	 * 要覆盖对返回值处理的内置支持, 请使用{@link #setReturnValueHandlers}.
	 */
	public void setCustomReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.customReturnValueHandlers = returnValueHandlers;
	}

	/**
	 * 返回自定义返回值处理器, 或{@code null}.
	 */
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * 配置支持的返回值类型的完整列表, 从而覆盖默认配置的处理器.
	 */
	public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			this.returnValueHandlers = null;
		}
		else {
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			this.returnValueHandlers.addHandlers(returnValueHandlers);
		}
	}

	/**
	 * 返回配置的处理器, 如果尚未通过{@link #afterPropertiesSet()}初始化, 则可能返回{@code null}.
	 */
	public HandlerMethodReturnValueHandlerComposite getReturnValueHandlers() {
		return this.returnValueHandlers;
	}

	/**
	 * 设置要使用的消息正文转换器.
	 * <p>这些转换器用于转换HTTP请求和响应.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 返回配置的消息正文转换器.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * 设置用于确定请求的媒体类型的{@link ContentNegotiationManager}.
	 * 如果未设置, 则使用默认构造函数.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回配置的{@link ContentNegotiationManager}.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 在执行使用{@code @ResponseBody}注解的控制器方法或返回{@code ResponseEntity}之后,
	 * 但在使用所选{@code HttpMessageConverter}将主体写入响应之前, 添加一个或多个要调用的组件.
	 */
	public void setResponseBodyAdvice(List<ResponseBodyAdvice<?>> responseBodyAdvice) {
		this.responseBodyAdvice.clear();
		if (responseBodyAdvice != null) {
			this.responseBodyAdvice.addAll(responseBodyAdvice);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() {
		// 首先执行此操作, 它可能会添加 ResponseBodyAdvice beans
		initExceptionHandlerAdviceCache();

		if (this.argumentResolvers == null) {
			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		if (this.returnValueHandlers == null) {
			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
		}
	}

	private void initExceptionHandlerAdviceCache() {
		if (getApplicationContext() == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for exception mappings: " + getApplicationContext());
		}

		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
		AnnotationAwareOrderComparator.sort(adviceBeans);

		for (ControllerAdviceBean adviceBean : adviceBeans) {
			ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(adviceBean.getBeanType());
			if (resolver.hasExceptionMappings()) {
				this.exceptionHandlerAdviceCache.put(adviceBean, resolver);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @ExceptionHandler methods in " + adviceBean);
				}
			}
			if (ResponseBodyAdvice.class.isAssignableFrom(adviceBean.getBeanType())) {
				this.responseBodyAdvice.add(adviceBean);
				if (logger.isInfoEnabled()) {
					logger.info("Detected ResponseBodyAdvice implementation in " + adviceBean);
				}
			}
		}
	}

	/**
	 * 使用ApplicationContext中发现的{@link ControllerAdvice @ControllerAdvice} bean返回不可修改的Map.
	 * 如果在通过{@link #afterPropertiesSet()}初始化bean之前调用该方法, 则返回的Map将为空.
	 */
	public Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> getExceptionHandlerAdviceCache() {
		return Collections.unmodifiableMap(this.exceptionHandlerAdviceCache);
	}

	/**
	 * 返回要使用的参数解析器列表, 包括通过{@link #setCustomArgumentResolvers}提供的内置解析器和自定义解析器.
	 */
	protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();

		// 基于注解的参数解析
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// 基于类型的参数解析
		resolvers.add(new ServletRequestMethodArgumentResolver());
		resolvers.add(new ServletResponseMethodArgumentResolver());
		resolvers.add(new RedirectAttributesMethodArgumentResolver());
		resolvers.add(new ModelMethodProcessor());

		// 自定义参数
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		return resolvers;
	}

	/**
	 * 返回要使用的返回值处理器列表, 包括通过{@link #setReturnValueHandlers}提供的内置和自定义处理器.
	 */
	protected List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<HandlerMethodReturnValueHandler>();

		// 单用途返回值类型
		handlers.add(new ModelAndViewMethodReturnValueHandler());
		handlers.add(new ModelMethodProcessor());
		handlers.add(new ViewMethodReturnValueHandler());
		handlers.add(new HttpEntityMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// 基于注解的返回值类型
		handlers.add(new ModelAttributeMethodProcessor(false));
		handlers.add(new RequestResponseBodyMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// 多用途返回值类型
		handlers.add(new ViewNameMethodReturnValueHandler());
		handlers.add(new MapMethodProcessor());

		// 自定义返回值类型
		if (getCustomReturnValueHandlers() != null) {
			handlers.addAll(getCustomReturnValueHandlers());
		}

		// Catch-all
		handlers.add(new ModelAttributeMethodProcessor(true));

		return handlers;
	}


	/**
	 * 查找{@code @ExceptionHandler}方法并调用它来处理引发的异常.
	 */
	@Override
	protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod, Exception exception) {

		ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception);
		if (exceptionHandlerMethod == null) {
			return null;
		}

		exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);

		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking @ExceptionHandler method: " + exceptionHandlerMethod);
			}
			Throwable cause = exception.getCause();
			if (cause != null) {
				// Expose cause as provided argument as well
				exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, exception, cause, handlerMethod);
			}
			else {
				// Otherwise, just the given exception as-is
				exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, exception, handlerMethod);
			}
		}
		catch (Throwable invocationEx) {
			// 除了原始异常以外的任何其他内容都是无意的, 可能是意外 (e.g. 断言失败等).
			if (invocationEx != exception && logger.isWarnEnabled()) {
				logger.warn("Failed to invoke @ExceptionHandler method: " + exceptionHandlerMethod, invocationEx);
			}
			// 继续默认处理原始异常...
			return null;
		}

		if (mavContainer.isRequestHandled()) {
			return new ModelAndView();
		}
		else {
			ModelMap model = mavContainer.getModel();
			HttpStatus status = mavContainer.getStatus();
			ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, status);
			mav.setViewName(mavContainer.getViewName());
			if (!mavContainer.isViewReference()) {
				mav.setView((View) mavContainer.getView());
			}
			if (model instanceof RedirectAttributes) {
				Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
				RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
			}
			return mav;
		}
	}

	/**
	 * 查找给定异常的{@code @ExceptionHandler}方法.
	 * 默认实现首先搜索控制器的类层次结构中的方法, 如果没有找到, 它会继续搜索其他{@code @ExceptionHandler}方法,
	 * 假设有一些{@linkplain ControllerAdvice @ControllerAdvice}检测到Spring管理的bean.
	 * 
	 * @param handlerMethod 引发异常的方法 (may be {@code null})
	 * @param exception 引发的异常
	 * 
	 * @return 要处理异常的方法, 或{@code null}
	 */
	protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
		Class<?> handlerType = null;

		if (handlerMethod != null) {
			// 控制器类本身的本地异常处理器方法.
			// 即使在基于接口的代理的情况下, 也要通过代理调用.
			handlerType = handlerMethod.getBeanType();
			ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(handlerType);
			if (resolver == null) {
				resolver = new ExceptionHandlerMethodResolver(handlerType);
				this.exceptionHandlerCache.put(handlerType, resolver);
			}
			Method method = resolver.resolveMethod(exception);
			if (method != null) {
				return new ServletInvocableHandlerMethod(handlerMethod.getBean(), method);
			}
			// 对于下面的增强适用性检查 (涉及基础包, 可分配类型和注解的存在), 使用目标类而不是基于接口的代理.
			if (Proxy.isProxyClass(handlerType)) {
				handlerType = AopUtils.getTargetClass(handlerMethod.getBean());
			}
		}

		for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
			ControllerAdviceBean advice = entry.getKey();
			if (advice.isApplicableToBeanType(handlerType)) {
				ExceptionHandlerMethodResolver resolver = entry.getValue();
				Method method = resolver.resolveMethod(exception);
				if (method != null) {
					return new ServletInvocableHandlerMethod(advice.resolveBean(), method);
				}
			}
		}

		return null;
	}
}
