package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ErrorsMethodArgumentResolver;
import org.springframework.web.method.annotation.ExpressionValueMethodArgumentResolver;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.annotation.MapMethodProcessor;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.annotation.ModelFactory;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.annotation.RequestHeaderMapMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.annotation.SessionAttributesHandler;
import org.springframework.web.method.annotation.SessionStatusMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link AbstractHandlerMethodAdapter}, 它支持{@link HandlerMethod}及其方法参数和返回类型签名,
 * 通过{@code @RequestMapping}定义.
 *
 * <p>可以通过{@link #setCustomArgumentResolvers} 和 {@link #setCustomReturnValueHandlers}
 * 添加对自定义参数和返回值类型的支持.
 * 或者, 要重新配置所有参数和返回值类型, 使用{@link #setArgumentResolvers}和{@link #setReturnValueHandlers}.
 */
public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter
		implements BeanFactoryAware, InitializingBean {

	/**
	 * 与{@link InitBinder @InitBinder}方法匹配的MethodFilter.
	 */
	public static final MethodFilter INIT_BINDER_METHODS = new MethodFilter() {
		@Override
		public boolean matches(Method method) {
			return (AnnotationUtils.findAnnotation(method, InitBinder.class) != null);
		}
	};

	/**
	 * 与{@link ModelAttribute @ModelAttribute}方法匹配的MethodFilter.
	 */
	public static final MethodFilter MODEL_ATTRIBUTE_METHODS = new MethodFilter() {
		@Override
		public boolean matches(Method method) {
			return (AnnotationUtils.findAnnotation(method, RequestMapping.class) == null &&
					AnnotationUtils.findAnnotation(method, ModelAttribute.class) != null);
		}
	};


	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private HandlerMethodArgumentResolverComposite argumentResolvers;

	private HandlerMethodArgumentResolverComposite initBinderArgumentResolvers;

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	private List<ModelAndViewResolver> modelAndViewResolvers;

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private List<HttpMessageConverter<?>> messageConverters;

	private List<Object> requestResponseBodyAdvice = new ArrayList<Object>();

	private WebBindingInitializer webBindingInitializer;

	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("MvcAsync");

	private Long asyncRequestTimeout;

	private CallableProcessingInterceptor[] callableInterceptors = new CallableProcessingInterceptor[0];

	private DeferredResultProcessingInterceptor[] deferredResultInterceptors = new DeferredResultProcessingInterceptor[0];

	private boolean ignoreDefaultModelOnRedirect = false;

	private int cacheSecondsForSessionAttributeHandlers = 0;

	private boolean synchronizeOnSession = false;

	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private ConfigurableBeanFactory beanFactory;


	private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache =
			new ConcurrentHashMap<Class<?>, SessionAttributesHandler>(64);

	private final Map<Class<?>, Set<Method>> initBinderCache = new ConcurrentHashMap<Class<?>, Set<Method>>(64);

	private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache =
			new LinkedHashMap<ControllerAdviceBean, Set<Method>>();

	private final Map<Class<?>, Set<Method>> modelAttributeCache = new ConcurrentHashMap<Class<?>, Set<Method>>(64);

	private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache =
			new LinkedHashMap<ControllerAdviceBean, Set<Method>>();


	public RequestMappingHandlerAdapter() {
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);  // see SPR-7316

		this.messageConverters = new ArrayList<HttpMessageConverter<?>>(4);
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		this.messageConverters.add(stringHttpMessageConverter);
		this.messageConverters.add(new SourceHttpMessageConverter<Source>());
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
	}


	/**
	 * 为自定义参数类型提供解析器. 自定义解析器在内置的解析器之后排序.
	 * 要覆盖对参数解析的内置支持, 请改用{@link #setArgumentResolvers}.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
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
	 * 返回配置的参数解析器, 如果尚未通过{@link #afterPropertiesSet()}初始化, 则可能返回{@code null}.
	 */
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return (this.argumentResolvers != null ? this.argumentResolvers.getResolvers() : null);
	}

	/**
	 * 配置{@code @InitBinder}方法中支持的参数类型.
	 */
	public void setInitBinderArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.initBinderArgumentResolvers = null;
		}
		else {
			this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.initBinderArgumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * 返回{@code @InitBinder}方法的参数解析器, 如果尚未通过{@link #afterPropertiesSet()}初始化, 则可能返回{@code null}.
	 */
	public List<HandlerMethodArgumentResolver> getInitBinderArgumentResolvers() {
		return (this.initBinderArgumentResolvers != null ? this.initBinderArgumentResolvers.getResolvers() : null);
	}

	/**
	 * 为自定义返回值类型提供处理器. 自定义处理器在内置处理器之后排序.
	 * 要覆盖对返回值处理的内置支持, 使用{@link #setReturnValueHandlers}.
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
	public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		return (this.returnValueHandlers != null ? this.returnValueHandlers.getHandlers() : null);
	}

	/**
	 * 提供自定义{@link ModelAndViewResolver}.
	 * <p><strong>Note:</strong> 此方法仅可用于向后兼容.
	 * 但是, 建议将{@code ModelAndViewResolver}重写为{@link HandlerMethodReturnValueHandler}.
	 * 由于无法实现{@link HandlerMethodReturnValueHandler#supportsReturnType}方法, 因此无法在两个接口之间使用适配器.
	 * 因此, {@code ModelAndViewResolver}仅限于在所有其他返回值处理器执行后, 在最后调用.
	 * <p>{@code HandlerMethodReturnValueHandler}提供对返回类型和控制器方法信息的更好访问,
	 * 并且可以相对于其他返回值处理器自由排序.
	 */
	public void setModelAndViewResolvers(List<ModelAndViewResolver> modelAndViewResolvers) {
		this.modelAndViewResolvers = modelAndViewResolvers;
	}

	/**
	 * 返回配置的{@link ModelAndViewResolver}, 或{@code null}.
	 */
	public List<ModelAndViewResolver> getModelAndViewResolvers() {
		return this.modelAndViewResolvers;
	}

	/**
	 * 设置用于确定请求的媒体类型的{@link ContentNegotiationManager}.
	 * 如果未设置, 则使用默认构造函数.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 提供转换器以在参数解析器和返回值处理器中使用, 这些处理器支持读取和/或写入请求和响应的主体.
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
	 * 添加一个或多个{@code RequestBodyAdvice}实例以拦截请求,
	 * 在为{@code @RequestBody}和{@code HttpEntity}方法参数进行读取和转换请求之前.
	 */
	public void setRequestBodyAdvice(List<RequestBodyAdvice> requestBodyAdvice) {
		if (requestBodyAdvice != null) {
			this.requestResponseBodyAdvice.addAll(requestBodyAdvice);
		}
	}

	/**
	 * 在{@code @ResponseBody}或{@code ResponseEntity}返回值写入响应正文之前,
	 * 添加一个或多个{@code ResponseBodyAdvice}实例以拦截响应.
	 */
	public void setResponseBodyAdvice(List<ResponseBodyAdvice<?>> responseBodyAdvice) {
		if (responseBodyAdvice != null) {
			this.requestResponseBodyAdvice.addAll(responseBodyAdvice);
		}
	}

	/**
	 * 提供具有"global"初始化的WebBindingInitializer以应用于每个DataBinder实例.
	 */
	public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * 返回配置的WebBindingInitializer, 或{@code null}.
	 */
	public WebBindingInitializer getWebBindingInitializer() {
		return this.webBindingInitializer;
	}

	/**
	 * 设置当控制器方法返回{@link Callable}时使用的默认{@link AsyncTaskExecutor}.
	 * 控制器方法可以通过返回{@link WebAsyncTask}在每个请求上覆盖此默认值.
	 * <p>默认使用{@link SimpleAsyncTaskExecutor}实例.
	 * 由于简单执行器不重用线程, 因此建议更改生产中的默认值.
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 指定并发处理超时时间, 以毫秒为单位.
	 * 在Servlet 3中, 超时在主请求处理线程退出后开始, 并在再次调度请求时结束, 以进一步处理并发生成的结果.
	 * <p>如果未设置此值, 则使用底层实现的默认超时, e.g. 使用Servlet 3的Tomcat为10秒.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public void setAsyncRequestTimeout(long timeout) {
		this.asyncRequestTimeout = timeout;
	}

	/**
	 * 配置{@code CallableProcessingInterceptor}来注册异步请求.
	 * 
	 * @param interceptors 要注册的拦截器
	 */
	public void setCallableInterceptors(List<CallableProcessingInterceptor> interceptors) {
		Assert.notNull(interceptors, "CallableProcessingInterceptor List must not be null");
		this.callableInterceptors = interceptors.toArray(new CallableProcessingInterceptor[interceptors.size()]);
	}

	/**
	 * 配置{@code DeferredResultProcessingInterceptor}来注册异步请求.
	 * 
	 * @param interceptors 要注册的拦截器
	 */
	public void setDeferredResultInterceptors(List<DeferredResultProcessingInterceptor> interceptors) {
		Assert.notNull(interceptors, "DeferredResultProcessingInterceptor List must not be null");
		this.deferredResultInterceptors = interceptors.toArray(new DeferredResultProcessingInterceptor[interceptors.size()]);
	}

	/**
	 * 默认情况下, 在呈现和重定向场景期间使用"default"模型的内容.
	 * 或者, 控制器方法可以声明{@link RedirectAttributes}参数并使用它来提供重定向的属性.
	 * <p>将此标志设置为{@code true}可确保即使未声明RedirectAttributes参数, 也不会在重定向场景中使用"default"模型.
	 * 将其设置为{@code false}意味着如果控制器方法未声明RedirectAttributes参数, 则可以在重定向中使用"default"模型.
	 * <p>默认设置为{@code false}, 但新应用程序应考虑将其设置为{@code true}.
	 */
	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	/**
	 * 指定存储会话属性的策略.
	 * 默认为{@link org.springframework.web.bind.support.DefaultSessionAttributeStore},
	 * 在HttpSession中存储会话属性, 其属性名称与模型中的属性名称相同.
	 */
	public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
		this.sessionAttributeStore = sessionAttributeStore;
	}

	/**
	 * 缓存由带{@code @SessionAttributes}注解的处理器生成的内容, 保存给定的秒数.
	 * <p>可能的值为:
	 * <ul>
	 * <li>-1: 没有生成与缓存相关的header</li>
	 * <li>0 (默认值): "Cache-Control: no-store"将禁止缓存</li>
	 * <li>1 或更高: "Cache-Control: max-age=seconds" 将缓存内容; 在处理会话属性时不建议</li>
	 * </ul>
	 * <p>与将适用于所有常规处理器(但不适用于带{@code @SessionAttributes}注解的处理器)的"cacheSeconds"属性相反,
	 * 此设置仅适用于{@code @SessionAttributes}处理器.
	 */
	public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
		this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
	}

	/**
	 * 设置是否应在会话上同步控制器执行, 以序列化来自同一客户端的并行调用.
	 * <p>更具体地说, 如果此标志为"true", 则{@code handleRequestInternal}方法的执行将同步.
	 * 最佳可用会话互斥锁将用于同步; 理想情况下, 这将是HttpSessionMutexListener公开的互斥锁.
	 * <p>会话互斥锁在会话的整个生命周期内保证是同一个对象, 在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下可用.
	 * 它用作同步锁定当前会话的安全引用.
	 * <p>在许多情况下, HttpSession引用本身也是一个安全的互斥锁, 因为它对于同一个活动逻辑会话始终是相同的对象引用.
	 * 但是, 不能在不同的servlet容器中保证这一点; 唯一 100% 安全的方式是会话互斥.
	 */
	public void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * 设置ParameterNameDiscoverer, 用于解析方法参数名称 (e.g. 对于默认属性名称).
	 * <p>默认为{@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 期望{@link ConfigurableBeanFactory}用于解析方法参数默认值中的表达式.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	/**
	 * 返回此bean实例的拥有工厂, 或{@code null}.
	 */
	protected ConfigurableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public void afterPropertiesSet() {
		// 首先执行此操作, 它可能会添加ResponseBody增强bean
		initControllerAdviceCache();

		if (this.argumentResolvers == null) {
			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		if (this.initBinderArgumentResolvers == null) {
			List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();
			this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		if (this.returnValueHandlers == null) {
			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
		}
	}

	private void initControllerAdviceCache() {
		if (getApplicationContext() == null) {
			return;
		}
		if (logger.isInfoEnabled()) {
			logger.info("Looking for @ControllerAdvice: " + getApplicationContext());
		}

		List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
		AnnotationAwareOrderComparator.sort(beans);

		List<Object> requestResponseBodyAdviceBeans = new ArrayList<Object>();

		for (ControllerAdviceBean bean : beans) {
			Set<Method> attrMethods = MethodIntrospector.selectMethods(bean.getBeanType(), MODEL_ATTRIBUTE_METHODS);
			if (!attrMethods.isEmpty()) {
				this.modelAttributeAdviceCache.put(bean, attrMethods);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @ModelAttribute methods in " + bean);
				}
			}
			Set<Method> binderMethods = MethodIntrospector.selectMethods(bean.getBeanType(), INIT_BINDER_METHODS);
			if (!binderMethods.isEmpty()) {
				this.initBinderAdviceCache.put(bean, binderMethods);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @InitBinder methods in " + bean);
				}
			}
			if (RequestBodyAdvice.class.isAssignableFrom(bean.getBeanType())) {
				requestResponseBodyAdviceBeans.add(bean);
				if (logger.isInfoEnabled()) {
					logger.info("Detected RequestBodyAdvice bean in " + bean);
				}
			}
			if (ResponseBodyAdvice.class.isAssignableFrom(bean.getBeanType())) {
				requestResponseBodyAdviceBeans.add(bean);
				if (logger.isInfoEnabled()) {
					logger.info("Detected ResponseBodyAdvice bean in " + bean);
				}
			}
		}

		if (!requestResponseBodyAdviceBeans.isEmpty()) {
			this.requestResponseBodyAdvice.addAll(0, requestResponseBodyAdviceBeans);
		}
	}

	/**
	 * 返回要使用的参数解析器列表, 包括通过{@link #setCustomArgumentResolvers}提供的内置解析器和自定义解析器.
	 */
	private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();

		// 基于注解的参数解析
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
		resolvers.add(new RequestParamMapMethodArgumentResolver());
		resolvers.add(new PathVariableMethodArgumentResolver());
		resolvers.add(new PathVariableMapMethodArgumentResolver());
		resolvers.add(new MatrixVariableMethodArgumentResolver());
		resolvers.add(new MatrixVariableMapMethodArgumentResolver());
		resolvers.add(new ServletModelAttributeMethodProcessor(false));
		resolvers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));
		resolvers.add(new RequestPartMethodArgumentResolver(getMessageConverters(), this.requestResponseBodyAdvice));
		resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory()));
		resolvers.add(new RequestHeaderMapMethodArgumentResolver());
		resolvers.add(new ServletCookieValueMethodArgumentResolver(getBeanFactory()));
		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// 基于类型的参数解析
		resolvers.add(new ServletRequestMethodArgumentResolver());
		resolvers.add(new ServletResponseMethodArgumentResolver());
		resolvers.add(new HttpEntityMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));
		resolvers.add(new RedirectAttributesMethodArgumentResolver());
		resolvers.add(new ModelMethodProcessor());
		resolvers.add(new MapMethodProcessor());
		resolvers.add(new ErrorsMethodArgumentResolver());
		resolvers.add(new SessionStatusMethodArgumentResolver());
		resolvers.add(new UriComponentsBuilderMethodArgumentResolver());

		// 自定义参数
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// Catch-all
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));
		resolvers.add(new ServletModelAttributeMethodProcessor(true));

		return resolvers;
	}

	/**
	 * 返回用于{@code @InitBinder}方法的参数解析器列表, 包括内置和自定义解析器.
	 */
	private List<HandlerMethodArgumentResolver> getDefaultInitBinderArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();

		// 基于注解的参数解析
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
		resolvers.add(new RequestParamMapMethodArgumentResolver());
		resolvers.add(new PathVariableMethodArgumentResolver());
		resolvers.add(new PathVariableMapMethodArgumentResolver());
		resolvers.add(new MatrixVariableMethodArgumentResolver());
		resolvers.add(new MatrixVariableMapMethodArgumentResolver());
		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// 基于类型的参数解析
		resolvers.add(new ServletRequestMethodArgumentResolver());
		resolvers.add(new ServletResponseMethodArgumentResolver());

		// Custom arguments
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// Catch-all
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));

		return resolvers;
	}

	/**
	 * 返回要使用的返回值处理器列表, 包括通过{@link #setReturnValueHandlers}提供的内置和自定义处理器.
	 */
	private List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<HandlerMethodReturnValueHandler>();

		// 单用途返回值类型
		handlers.add(new ModelAndViewMethodReturnValueHandler());
		handlers.add(new ModelMethodProcessor());
		handlers.add(new ViewMethodReturnValueHandler());
		handlers.add(new ResponseBodyEmitterReturnValueHandler(getMessageConverters()));
		handlers.add(new StreamingResponseBodyReturnValueHandler());
		handlers.add(new HttpEntityMethodProcessor(getMessageConverters(),
				this.contentNegotiationManager, this.requestResponseBodyAdvice));
		handlers.add(new HttpHeadersReturnValueHandler());
		handlers.add(new CallableMethodReturnValueHandler());
		handlers.add(new DeferredResultMethodReturnValueHandler());
		handlers.add(new AsyncTaskMethodReturnValueHandler(this.beanFactory));

		// 基于注解的返回值类型
		handlers.add(new ModelAttributeMethodProcessor(false));
		handlers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(),
				this.contentNegotiationManager, this.requestResponseBodyAdvice));

		// 多用途返回值类型
		handlers.add(new ViewNameMethodReturnValueHandler());
		handlers.add(new MapMethodProcessor());

		// 自定义返回值类型
		if (getCustomReturnValueHandlers() != null) {
			handlers.addAll(getCustomReturnValueHandlers());
		}

		// Catch-all
		if (!CollectionUtils.isEmpty(getModelAndViewResolvers())) {
			handlers.add(new ModelAndViewResolverMethodReturnValueHandler(getModelAndViewResolvers()));
		}
		else {
			handlers.add(new ModelAttributeMethodProcessor(true));
		}

		return handlers;
	}


	/**
	 * 始终返回{@code true}, 因为任何方法参数和返回值类型都将以某种方式处理.
	 * 任何HandlerMethodArgumentResolver都无法识别的方法参数如果是简单类型,
	 * 则会被解释为请求参数, 否则会被解释为模型属性.
	 * 任何HandlerMethodReturnValueHandler都无法识别的返回值将被解释为模型属性.
	 */
	@Override
	protected boolean supportsInternal(HandlerMethod handlerMethod) {
		return true;
	}

	@Override
	protected ModelAndView handleInternal(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		ModelAndView mav;
		checkRequest(request);

		// 如果需要, 在synchronized块中执行invokeHandlerMethod.
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					mav = invokeHandlerMethod(request, response, handlerMethod);
				}
			}
			else {
				// No HttpSession available -> no mutex necessary
				mav = invokeHandlerMethod(request, response, handlerMethod);
			}
		}
		else {
			// 根本不需要会话同步...
			mav = invokeHandlerMethod(request, response, handlerMethod);
		}

		if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
			if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
				applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
			}
			else {
				prepareResponse(response);
			}
		}

		return mav;
	}

	/**
	 * 此实现总是返回 -1.
	 * {@code @RequestMapping}方法可以计算lastModified值, 调用{@link WebRequest#checkNotModified(long)},
	 * 如果该调用的结果为{@code true}, 则返回{@code null}.
	 */
	@Override
	protected long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod) {
		return -1;
	}


	/**
	 * 返回给定处理器类型的{@link SessionAttributesHandler}实例 (never {@code null}).
	 */
	private SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
		Class<?> handlerType = handlerMethod.getBeanType();
		SessionAttributesHandler sessionAttrHandler = this.sessionAttributesHandlerCache.get(handlerType);
		if (sessionAttrHandler == null) {
			synchronized (this.sessionAttributesHandlerCache) {
				sessionAttrHandler = this.sessionAttributesHandlerCache.get(handlerType);
				if (sessionAttrHandler == null) {
					sessionAttrHandler = new SessionAttributesHandler(handlerType, sessionAttributeStore);
					this.sessionAttributesHandlerCache.put(handlerType, sessionAttrHandler);
				}
			}
		}
		return sessionAttrHandler;
	}

	/**
	 * 如果需要查看解析, 调用{@link RequestMapping}处理器方法准备{@link ModelAndView}.
	 */
	protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		try {
			WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
			ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

			ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
			invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
			invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
			invocableMethod.setDataBinderFactory(binderFactory);
			invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);

			ModelAndViewContainer mavContainer = new ModelAndViewContainer();
			mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
			modelFactory.initModel(webRequest, mavContainer, invocableMethod);
			mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

			AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
			asyncWebRequest.setTimeout(this.asyncRequestTimeout);

			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			asyncManager.setTaskExecutor(this.taskExecutor);
			asyncManager.setAsyncWebRequest(asyncWebRequest);
			asyncManager.registerCallableInterceptors(this.callableInterceptors);
			asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);

			if (asyncManager.hasConcurrentResult()) {
				Object result = asyncManager.getConcurrentResult();
				mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
				asyncManager.clearConcurrentResult();
				if (logger.isDebugEnabled()) {
					logger.debug("Found concurrent result value [" + result + "]");
				}
				invocableMethod = invocableMethod.wrapConcurrentResult(result);
			}

			invocableMethod.invokeAndHandle(webRequest, mavContainer);
			if (asyncManager.isConcurrentHandlingStarted()) {
				return null;
			}

			return getModelAndView(mavContainer, modelFactory, webRequest);
		}
		finally {
			webRequest.requestCompleted();
		}
	}

	/**
	 * 从给定的{@link HandlerMethod}定义创建{@link ServletInvocableHandlerMethod}.
	 * 
	 * @param handlerMethod {@link HandlerMethod}定义
	 * 
	 * @return 相应的{@link ServletInvocableHandlerMethod} (或自定义子类)
	 */
	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
		return new ServletInvocableHandlerMethod(handlerMethod);
	}

	private ModelFactory getModelFactory(HandlerMethod handlerMethod, WebDataBinderFactory binderFactory) {
		SessionAttributesHandler sessionAttrHandler = getSessionAttributesHandler(handlerMethod);
		Class<?> handlerType = handlerMethod.getBeanType();
		Set<Method> methods = this.modelAttributeCache.get(handlerType);
		if (methods == null) {
			methods = MethodIntrospector.selectMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
			this.modelAttributeCache.put(handlerType, methods);
		}
		List<InvocableHandlerMethod> attrMethods = new ArrayList<InvocableHandlerMethod>();
		// Global methods first
		for (Entry<ControllerAdviceBean, Set<Method>> entry : this.modelAttributeAdviceCache.entrySet()) {
			if (entry.getKey().isApplicableToBeanType(handlerType)) {
				Object bean = entry.getKey().resolveBean();
				for (Method method : entry.getValue()) {
					attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
				}
			}
		}
		for (Method method : methods) {
			Object bean = handlerMethod.getBean();
			attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
		}
		return new ModelFactory(attrMethods, binderFactory, sessionAttrHandler);
	}

	private InvocableHandlerMethod createModelAttributeMethod(WebDataBinderFactory factory, Object bean, Method method) {
		InvocableHandlerMethod attrMethod = new InvocableHandlerMethod(bean, method);
		attrMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		attrMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
		attrMethod.setDataBinderFactory(factory);
		return attrMethod;
	}

	private WebDataBinderFactory getDataBinderFactory(HandlerMethod handlerMethod) throws Exception {
		Class<?> handlerType = handlerMethod.getBeanType();
		Set<Method> methods = this.initBinderCache.get(handlerType);
		if (methods == null) {
			methods = MethodIntrospector.selectMethods(handlerType, INIT_BINDER_METHODS);
			this.initBinderCache.put(handlerType, methods);
		}
		List<InvocableHandlerMethod> initBinderMethods = new ArrayList<InvocableHandlerMethod>();
		// Global methods first
		for (Entry<ControllerAdviceBean, Set<Method>> entry : this.initBinderAdviceCache.entrySet()) {
			if (entry.getKey().isApplicableToBeanType(handlerType)) {
				Object bean = entry.getKey().resolveBean();
				for (Method method : entry.getValue()) {
					initBinderMethods.add(createInitBinderMethod(bean, method));
				}
			}
		}
		for (Method method : methods) {
			Object bean = handlerMethod.getBean();
			initBinderMethods.add(createInitBinderMethod(bean, method));
		}
		return createDataBinderFactory(initBinderMethods);
	}

	private InvocableHandlerMethod createInitBinderMethod(Object bean, Method method) {
		InvocableHandlerMethod binderMethod = new InvocableHandlerMethod(bean, method);
		binderMethod.setHandlerMethodArgumentResolvers(this.initBinderArgumentResolvers);
		binderMethod.setDataBinderFactory(new DefaultDataBinderFactory(this.webBindingInitializer));
		binderMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
		return binderMethod;
	}

	/**
	 * 用于创建新InitBinderDataBinderFactory实例的Template方法.
	 * <p>默认实现创建ServletRequestDataBinderFactory.
	 * 这可以为自定义ServletRequestDataBinder子类重写.
	 * 
	 * @param binderMethods {@code @InitBinder}方法
	 * 
	 * @return 要使用的InitBinderDataBinderFactory
	 * @throws Exception 如果状态或参数无效
	 */
	protected InitBinderDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods)
			throws Exception {

		return new ServletRequestDataBinderFactory(binderMethods, getWebBindingInitializer());
	}

	private ModelAndView getModelAndView(ModelAndViewContainer mavContainer,
			ModelFactory modelFactory, NativeWebRequest webRequest) throws Exception {

		modelFactory.updateModel(webRequest, mavContainer);
		if (mavContainer.isRequestHandled()) {
			return null;
		}
		ModelMap model = mavContainer.getModel();
		ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, mavContainer.getStatus());
		if (!mavContainer.isViewReference()) {
			mav.setView((View) mavContainer.getView());
		}
		if (model instanceof RedirectAttributes) {
			Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
		}
		return mav;
	}

}
