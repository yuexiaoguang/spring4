package org.springframework.web.portlet.mvc.annotation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.ClientDataRequest;
import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.MimeResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.StateAwareResponse;
import javax.portlet.WindowState;
import javax.servlet.http.Cookie;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.NoHandlerFoundException;
import org.springframework.web.portlet.bind.MissingPortletRequestParameterException;
import org.springframework.web.portlet.bind.PortletRequestDataBinder;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;
import org.springframework.web.portlet.context.PortletWebRequest;
import org.springframework.web.portlet.handler.PortletContentGenerator;
import org.springframework.web.portlet.handler.PortletSessionRequiredException;
import org.springframework.web.portlet.util.PortletUtils;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

/**
 * {@link org.springframework.web.portlet.HandlerAdapter}接口的实现,
 * 根据通过{@link RequestMapping}注解表示的portlet模式, 操作/渲染阶段, 和请求参数来映射处理器方法.
 *
 * <p>通过{@link RequestParam}注解支持请求参数绑定.
 * 还支持用于将模型属性值公开给视图的{@link ModelAttribute}注解, 以及用于绑定器初始化方法的{@link InitBinder},
 * 和用于特定属性的自动会话管理的{@link SessionAttributes}.
 *
 * <p>可以通过各种bean属性自定义此适配器.
 * 一个常见的用例是通过自定义{@link #setWebBindingInitializer WebBindingInitializer}应用共享绑定器初始化逻辑.
 */
public class AnnotationMethodHandlerAdapter extends PortletContentGenerator
		implements HandlerAdapter, Ordered, BeanFactoryAware {

	public static final String IMPLICIT_MODEL_SESSION_ATTRIBUTE =
			AnnotationMethodHandlerAdapter.class.getName() + ".IMPLICIT_MODEL";

	public static final String IMPLICIT_MODEL_RENDER_PARAMETER = "implicitModel";


	private WebBindingInitializer webBindingInitializer;

	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

	private int cacheSecondsForSessionAttributeHandlers = 0;

	private boolean synchronizeOnSession = false;

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private WebArgumentResolver[] customArgumentResolvers;

	private ModelAndViewResolver[] customModelAndViewResolvers;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private ConfigurableBeanFactory beanFactory;

	private BeanExpressionContext expressionContext;

	private final Map<Class<?>, PortletHandlerMethodResolver> methodResolverCache =
			new ConcurrentHashMap<Class<?>, PortletHandlerMethodResolver>(64);


	/**
	 * 指定一个WebBindingInitializer, 它将预配置的配置应用于此控制器使用的每个DataBinder.
	 */
	public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * 指定存储会话属性的策略.
	 * <p>默认为{@link org.springframework.web.bind.support.DefaultSessionAttributeStore},
	 * 使用与模型中相同的属性名称在PortletSession中存储会话属性.
	 */
	public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore must not be null");
		this.sessionAttributeStore = sessionAttributeStore;
	}

	/**
	 * 缓存由带{@code @SessionAttributes}注解的处理器生成的内容给定的秒数.
	 * 默认为0, 完全阻止缓存.
	 * <p>与将适用于所有常规处理器(但不适用于带{@code @SessionAttributes}注解的处理器)的"cacheSeconds"属性相反,
	 * 此设置仅适用于带{@code @SessionAttributes}注解的处理器.
	 */
	public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
		this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
	}

	/**
	 * 设置是否应在会话上同步控制器执行, 以序列化来自同一客户端的并行调用.
	 * <p>更具体地说, 如果此标志为"true", 则每个处理器方法的执行将同步.
	 * 最佳可用会话互斥锁将用于同步; 理想情况下, 这将是HttpSessionMutexListener公开的互斥锁.
	 * <p>会话互斥锁在会话的整个生命周期内保证是同一个对象, 在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下可用.
	 * 它用作锁定当前会话的同步的安全引用.
	 * <p>在许多情况下, PortletSession引用本身也是一个安全的互斥锁, 因为它始终是同一个活动逻辑会话的相同对象引用.
	 * 但是, 不能在不同的servlet容器中保证这一点; 唯一 100% 安全的方式是会话互斥.
	 */
	public void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * 设置为用于解析方法参数名称的ParameterNameDiscoverer (e.g. 对于默认属性名称).
	 * <p>默认为{@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 设置用于特殊方法参数类型的自定义WebArgumentResolver.
	 * 这样的自定义WebArgumentResolver将首先启动, 有机会在标准参数处理开始之前解析参数值.
	 */
	public void setCustomArgumentResolver(WebArgumentResolver argumentResolver) {
		this.customArgumentResolvers = new WebArgumentResolver[] {argumentResolver};
	}

	/**
	 * 设置用于特殊方法参数类型的一个或多个自定义WebArgumentResolvers.
	 * 任何这样的自定义WebArgumentResolver都将首先启动, 有机会在标准参数处理开始之前解析参数值.
	 */
	public void setCustomArgumentResolvers(WebArgumentResolver... argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * 设置用于特殊方法返回类型的自定义ModelAndViewResolvers.
	 * 这样的自定义ModelAndViewResolver将首先启动, 有机会在标准ModelAndView处理启动之前解析返回值.
	 */
	public void setCustomModelAndViewResolver(ModelAndViewResolver customModelAndViewResolver) {
		this.customModelAndViewResolvers = new ModelAndViewResolver[]{customModelAndViewResolver};
	}

	/**
	 * 设置用于特殊方法返回类型的一个或多个自定义ModelAndViewResolvers.
	 * 任何这样的自定义ModelAndViewResolver都将首先启动, 有机会在标准ModelAndView处理开始之前解析返回值.
	 */
	public void setCustomModelAndViewResolvers(ModelAndViewResolver... customModelAndViewResolvers) {
		this.customModelAndViewResolvers = customModelAndViewResolvers;
	}

	/**
	 * 指定此HandlerAdapter bean的顺序值.
	 * <p>默认为{@code Integer.MAX_VALUE}, 表示它是非有序的.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
			this.expressionContext = new BeanExpressionContext(this.beanFactory, new RequestScope());
		}
	}


	@Override
	public boolean supports(Object handler) {
		return getMethodResolver(handler).hasHandlerMethods();
	}

	@Override
	public void handleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception {
		Object returnValue = doHandle(request, response, handler);
		if (returnValue != null) {
			throw new IllegalStateException("Invalid action method return value: " + returnValue);
		}
	}

	@Override
	public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception {
		checkAndPrepare(request, response);
		return doHandle(request, response, handler);
	}

	@Override
	public ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler) throws Exception {
		checkAndPrepare(request, response);
		return doHandle(request, response, handler);
	}

	@Override
	public void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
		Object returnValue = doHandle(request, response, handler);
		if (returnValue != null) {
			throw new IllegalStateException("Invalid event method return value: " + returnValue);
		}
	}


	protected ModelAndView doHandle(PortletRequest request, PortletResponse response, Object handler) throws Exception {
		ExtendedModelMap implicitModel = null;

		if (response instanceof MimeResponse) {
			MimeResponse mimeResponse = (MimeResponse) response;
			// 从关联的操作阶段检测隐式模型.
			if (response instanceof RenderResponse) {
				PortletSession session = request.getPortletSession(false);
				if (session != null) {
					if (request.getParameter(IMPLICIT_MODEL_RENDER_PARAMETER) != null) {
						implicitModel = (ExtendedModelMap) session.getAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE);
					}
					else {
						session.removeAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE);
					}
				}
			}
			if (handler.getClass().getAnnotation(SessionAttributes.class) != null) {
				// 在会话属性管理的情况下始终阻止缓存.
				checkAndPrepare(request, mimeResponse, this.cacheSecondsForSessionAttributeHandlers);
			}
			else {
				// 使用配置的默认cacheSeconds设置.
				checkAndPrepare(request, mimeResponse);
			}
		}

		if (implicitModel == null) {
			implicitModel = new BindingAwareModelMap();
		}

		// 如果需要, 在synchronized块中执行invokeHandlerMethod.
		if (this.synchronizeOnSession) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					return invokeHandlerMethod(request, response, handler, implicitModel);
				}
			}
		}

		return invokeHandlerMethod(request, response, handler, implicitModel);
	}

	@SuppressWarnings("unchecked")
	private ModelAndView invokeHandlerMethod(
			PortletRequest request, PortletResponse response, Object handler, ExtendedModelMap implicitModel)
			throws Exception {

		PortletWebRequest webRequest = new PortletWebRequest(request, response);
		PortletHandlerMethodResolver methodResolver = getMethodResolver(handler);
		Method handlerMethod = methodResolver.resolveHandlerMethod(request);
		PortletHandlerMethodInvoker methodInvoker = new PortletHandlerMethodInvoker(methodResolver);

		Object result = methodInvoker.invokeHandlerMethod(handlerMethod, handler, webRequest, implicitModel);
		ModelAndView mav = methodInvoker.getModelAndView(handlerMethod, handler.getClass(), result, implicitModel,
				webRequest);
		methodInvoker.updateModelAttributes(
				handler, (mav != null ? mav.getModel() : null), implicitModel, webRequest);

		// 为后续渲染阶段公开隐式模型.
		if (response instanceof StateAwareResponse && !implicitModel.isEmpty()) {
			StateAwareResponse stateResponse = (StateAwareResponse) response;
			Map<?, ?> modelToStore = implicitModel;
			try {
				stateResponse.setRenderParameter(IMPLICIT_MODEL_RENDER_PARAMETER, Boolean.TRUE.toString());
				if (response instanceof EventResponse) {
					// 在响应事件时更新现有模型 - 而我们在操作响应时替换模型.
					Map<String, Object> existingModel = (Map<String, Object>)
							request.getPortletSession().getAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE);
					if (existingModel != null) {
						existingModel.putAll(implicitModel);
						modelToStore = existingModel;
					}
				}
				request.getPortletSession().setAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE, modelToStore);
			}
			catch (IllegalStateException ex) {
				// 可能sendRedirect被调用... 不需要将模型暴露给渲染阶段.
			}
		}

		return mav;
	}

	/**
	 * 为给定的处理器类型构建HandlerMethodResolver.
	 */
	private PortletHandlerMethodResolver getMethodResolver(Object handler) {
		Class<?> handlerClass = ClassUtils.getUserClass(handler);
		PortletHandlerMethodResolver resolver = this.methodResolverCache.get(handlerClass);
		if (resolver == null) {
			synchronized (this.methodResolverCache) {
				resolver = this.methodResolverCache.get(handlerClass);
				if (resolver == null) {
					resolver = new PortletHandlerMethodResolver(handlerClass);
					this.methodResolverCache.put(handlerClass, resolver);
				}
			}
		}
		return resolver;
	}

	/**
	 * 用于创建新PortletRequestDataBinder实例的模板方法.
	 * <p>默认实现创建标准PortletRequestDataBinder.
	 * 可以为自定义PortletRequestDataBinder子类重写此项.
	 * 
	 * @param request 当前的portlet请求
	 * @param target 绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 * @param objectName 目标对象的objectName
	 * 
	 * @return 要使用的PortletRequestDataBinder实例
	 * @throws Exception 如果状态或参数无效
	 */
	protected PortletRequestDataBinder createBinder(PortletRequest request, Object target, String objectName) throws Exception {
		return new PortletRequestDataBinder(target, objectName);
	}


	/**
	 * {@code HandlerMethodResolver}的特定于Portlet的子类.
	 */
	@SuppressWarnings("deprecation")
	private static class PortletHandlerMethodResolver extends org.springframework.web.bind.annotation.support.HandlerMethodResolver {

		private final Map<Method, RequestMappingInfo> mappings = new HashMap<Method, RequestMappingInfo>();

		public PortletHandlerMethodResolver(Class<?> handlerType) {
			init(handlerType);
		}

		@Override
		protected boolean isHandlerMethod(Method method) {
			if (this.mappings.containsKey(method)) {
				return true;
			}
			RequestMappingInfo mappingInfo = new RequestMappingInfo();
			ActionMapping actionMapping = AnnotationUtils.findAnnotation(method, ActionMapping.class);
			RenderMapping renderMapping = AnnotationUtils.findAnnotation(method, RenderMapping.class);
			ResourceMapping resourceMapping = AnnotationUtils.findAnnotation(method, ResourceMapping.class);
			EventMapping eventMapping = AnnotationUtils.findAnnotation(method, EventMapping.class);
			RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if (actionMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.ACTION_PHASE, actionMapping.name(), actionMapping.params());
			}
			if (renderMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.RENDER_PHASE, renderMapping.windowState(), renderMapping.params());
			}
			if (resourceMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.RESOURCE_PHASE, resourceMapping.value(), new String[0]);
			}
			if (eventMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.EVENT_PHASE, eventMapping.value(), new String[0]);
			}
			if (requestMapping != null) {
				mappingInfo.initStandardMapping(requestMapping.value(), requestMapping.method(),
						requestMapping.params(), requestMapping.headers());
				if (mappingInfo.phase == null) {
					mappingInfo.phase = determineDefaultPhase(method);
				}
			}
			if (mappingInfo.phase != null) {
				this.mappings.put(method, mappingInfo);
				return true;
			}
			return false;
		}

		public Method resolveHandlerMethod(PortletRequest request) throws PortletException {
			Map<RequestMappingInfo, Method> targetHandlerMethods = new LinkedHashMap<RequestMappingInfo, Method>();
			for (Method handlerMethod : getHandlerMethods()) {
				RequestMappingInfo mappingInfo = this.mappings.get(handlerMethod);
				if (mappingInfo.match(request)) {
					Method oldMappedMethod = targetHandlerMethods.put(mappingInfo, handlerMethod);
					if (oldMappedMethod != null && oldMappedMethod != handlerMethod) {
						throw new IllegalStateException("Ambiguous handler methods mapped for portlet mode '" +
								request.getPortletMode() + "': {" + oldMappedMethod + ", " + handlerMethod +
								"}. If you intend to handle the same mode in multiple methods, then factor " +
								"them out into a dedicated handler class with that mode mapped at the type level!");
					}
				}
			}
			if (!targetHandlerMethods.isEmpty()) {
				if (targetHandlerMethods.size() == 1) {
					return targetHandlerMethods.values().iterator().next();
				}
				else {
					RequestMappingInfo bestMappingMatch = null;
					for (RequestMappingInfo mapping : targetHandlerMethods.keySet()) {
						if (bestMappingMatch == null) {
							bestMappingMatch = mapping;
						}
						else {
							if (mapping.isBetterMatchThan(bestMappingMatch)) {
								bestMappingMatch = mapping;
							}
						}
					}
					return targetHandlerMethods.get(bestMappingMatch);
				}
			}
			else {
				throw new NoHandlerFoundException("No matching handler method found for portlet request", request);
			}
		}

		private String determineDefaultPhase(Method handlerMethod) {
			if (void.class != handlerMethod.getReturnType()) {
				return PortletRequest.RENDER_PHASE;
			}
			for (Class<?> argType : handlerMethod.getParameterTypes()) {
				if (ActionRequest.class.isAssignableFrom(argType) || ActionResponse.class.isAssignableFrom(argType) ||
						InputStream.class.isAssignableFrom(argType) || Reader.class.isAssignableFrom(argType)) {
					return PortletRequest.ACTION_PHASE;
				}
				else if (RenderRequest.class.isAssignableFrom(argType) || RenderResponse.class.isAssignableFrom(argType) ||
						OutputStream.class.isAssignableFrom(argType) || Writer.class.isAssignableFrom(argType)) {
					return PortletRequest.RENDER_PHASE;
				}
				else if (ResourceRequest.class.isAssignableFrom(argType) || ResourceResponse.class.isAssignableFrom(argType)) {
					return PortletRequest.RESOURCE_PHASE;
				}
				else if (EventRequest.class.isAssignableFrom(argType) || EventResponse.class.isAssignableFrom(argType)) {
					return PortletRequest.EVENT_PHASE;
				}
			}
			return "";
		}
	}


	/**
	 * {@code HandlerMethodInvoker}的特定于Portlet的子类.
	 */
	@SuppressWarnings("deprecation")
	private class PortletHandlerMethodInvoker extends org.springframework.web.bind.annotation.support.HandlerMethodInvoker {

		public PortletHandlerMethodInvoker(org.springframework.web.bind.annotation.support.HandlerMethodResolver resolver) {
			super(resolver, webBindingInitializer, sessionAttributeStore,
					parameterNameDiscoverer, customArgumentResolvers, null);
		}

		@Override
		protected void raiseMissingParameterException(String paramName, Class<?> paramType) throws Exception {
			throw new MissingPortletRequestParameterException(paramName, paramType.getSimpleName());
		}

		@Override
		protected void raiseSessionRequiredException(String message) throws Exception {
			throw new PortletSessionRequiredException(message);
		}

		@Override
		protected WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
				throws Exception {

			return AnnotationMethodHandlerAdapter.this.createBinder(
					webRequest.getNativeRequest(PortletRequest.class), target, objectName);
		}

		@Override
		protected void doBind(WebDataBinder binder, NativeWebRequest webRequest) throws Exception {
			PortletRequestDataBinder portletBinder = (PortletRequestDataBinder) binder;
			portletBinder.bind(webRequest.getNativeRequest(PortletRequest.class));
		}

		@Override
		protected Object resolveDefaultValue(String value) {
			if (beanFactory == null) {
				return value;
			}
			String placeholdersResolved = beanFactory.resolveEmbeddedValue(value);
			BeanExpressionResolver exprResolver = beanFactory.getBeanExpressionResolver();
			if (exprResolver == null) {
				return value;
			}
			return exprResolver.evaluate(placeholdersResolved, expressionContext);
		}

		@Override
		protected Object resolveCookieValue(String cookieName, Class<?> paramType, NativeWebRequest webRequest)
				throws Exception {

			PortletRequest portletRequest = webRequest.getNativeRequest(PortletRequest.class);
			Cookie cookieValue = PortletUtils.getCookie(portletRequest, cookieName);
			if (Cookie.class.isAssignableFrom(paramType)) {
				return cookieValue;
			}
			else if (cookieValue != null) {
				return cookieValue.getValue();
			}
			else {
				return null;
			}
		}

		@Override
		protected Object resolveStandardArgument(Class<?> parameterType, NativeWebRequest webRequest)
				throws Exception {

			PortletRequest request = webRequest.getNativeRequest(PortletRequest.class);
			PortletResponse response = webRequest.getNativeResponse(PortletResponse.class);

			if (PortletRequest.class.isAssignableFrom(parameterType) ||
					MultipartRequest.class.isAssignableFrom(parameterType)) {
				Object nativeRequest = webRequest.getNativeRequest(parameterType);
				if (nativeRequest == null) {
					throw new IllegalStateException(
							"Current request is not of type [" + parameterType.getName() + "]: " + request);
				}
				return nativeRequest;
			}
			else if (PortletResponse.class.isAssignableFrom(parameterType)) {
				Object nativeResponse = webRequest.getNativeResponse(parameterType);
				if (nativeResponse == null) {
					throw new IllegalStateException(
							"Current response is not of type [" + parameterType.getName() + "]: " + response);
				}
				return nativeResponse;
			}
			else if (PortletSession.class.isAssignableFrom(parameterType)) {
				return request.getPortletSession();
			}
			else if (PortletPreferences.class.isAssignableFrom(parameterType)) {
				return request.getPreferences();
			}
			else if (PortletMode.class.isAssignableFrom(parameterType)) {
				return request.getPortletMode();
			}
			else if (WindowState.class.isAssignableFrom(parameterType)) {
				return request.getWindowState();
			}
			else if (PortalContext.class.isAssignableFrom(parameterType)) {
				return request.getPortalContext();
			}
			else if (Principal.class.isAssignableFrom(parameterType)) {
				return request.getUserPrincipal();
			}
			else if (Locale.class == parameterType) {
				return request.getLocale();
			}
			else if (InputStream.class.isAssignableFrom(parameterType)) {
				if (!(request instanceof ClientDataRequest)) {
					throw new IllegalStateException("InputStream can only get obtained for Action/ResourceRequest");
				}
				return ((ClientDataRequest) request).getPortletInputStream();
			}
			else if (Reader.class.isAssignableFrom(parameterType)) {
				if (!(request instanceof ClientDataRequest)) {
					throw new IllegalStateException("Reader can only get obtained for Action/ResourceRequest");
				}
				return ((ClientDataRequest) request).getReader();
			}
			else if (OutputStream.class.isAssignableFrom(parameterType)) {
				if (!(response instanceof MimeResponse)) {
					throw new IllegalStateException("OutputStream can only get obtained for Render/ResourceResponse");
				}
				return ((MimeResponse) response).getPortletOutputStream();
			}
			else if (Writer.class.isAssignableFrom(parameterType)) {
				if (!(response instanceof MimeResponse)) {
					throw new IllegalStateException("Writer can only get obtained for Render/ResourceResponse");
				}
				return ((MimeResponse) response).getWriter();
			}
			else if (Event.class == parameterType) {
				if (!(request instanceof EventRequest)) {
					throw new IllegalStateException("Event can only get obtained from EventRequest");
				}
				return ((EventRequest) request).getEvent();
			}
			return super.resolveStandardArgument(parameterType, webRequest);
		}

		@SuppressWarnings("unchecked")
		public ModelAndView getModelAndView(Method handlerMethod, Class<?> handlerType, Object returnValue, ExtendedModelMap implicitModel,
				PortletWebRequest webRequest) {
			// Invoke custom resolvers if present...
			if (customModelAndViewResolvers != null) {
				for (ModelAndViewResolver mavResolver : customModelAndViewResolvers) {
					org.springframework.web.servlet.ModelAndView smav =
							mavResolver.resolveModelAndView(handlerMethod, handlerType, returnValue, implicitModel, webRequest);
					if (smav != ModelAndViewResolver.UNRESOLVED) {
						return (smav.isReference() ?
								new ModelAndView(smav.getViewName(), smav.getModelMap()) :
								new ModelAndView(smav.getView(), smav.getModelMap()));
					}
				}
			}

			if (returnValue instanceof ModelAndView) {
				ModelAndView mav = (ModelAndView) returnValue;
				mav.getModelMap().mergeAttributes(implicitModel);
				return mav;
			}
			else if (returnValue instanceof org.springframework.web.servlet.ModelAndView) {
				org.springframework.web.servlet.ModelAndView smav = (org.springframework.web.servlet.ModelAndView) returnValue;
				ModelAndView mav = (smav.isReference() ?
						new ModelAndView(smav.getViewName(), smav.getModelMap()) :
						new ModelAndView(smav.getView(), smav.getModelMap()));
				mav.getModelMap().mergeAttributes(implicitModel);
				return mav;
			}
			else if (returnValue instanceof Model) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects(((Model) returnValue).asMap());
			}
			else if (returnValue instanceof View) {
				return new ModelAndView(returnValue).addAllObjects(implicitModel);
			}
			else if (handlerMethod.isAnnotationPresent(ModelAttribute.class)) {
				addReturnValueAsModelAttribute(handlerMethod, handlerType, returnValue, implicitModel);
				return new ModelAndView().addAllObjects(implicitModel);
			}
			else if (returnValue instanceof Map) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects((Map<String, Object>) returnValue);
			}
			else if (returnValue instanceof String) {
				return new ModelAndView((String) returnValue).addAllObjects(implicitModel);
			}
			else if (returnValue == null) {
				// Either returned null or was 'void' return.
				return null;
			}
			else if (!BeanUtils.isSimpleProperty(returnValue.getClass())) {
				// Assume a single model attribute...
				addReturnValueAsModelAttribute(handlerMethod, handlerType, returnValue, implicitModel);
				return new ModelAndView().addAllObjects(implicitModel);
			}
			else {
				throw new IllegalArgumentException("Invalid handler method return value: " + returnValue);
			}
		}
	}


	/**
	 * 请求映射元数据的保存器. 允许查找最佳匹配候选者.
	 */
	private static class RequestMappingInfo {

		public final Set<PortletMode> modes = new HashSet<PortletMode>();

		public String phase;

		public String value;

		public final Set<String> methods = new HashSet<String>();

		public String[] params = new String[0];

		public String[] headers = new String[0];

		public void initStandardMapping(String[] modes, RequestMethod[] methods, String[] params, String[] headers) {
			for (String mode : modes) {
				this.modes.add(new PortletMode(mode));
			}
			for (RequestMethod method : methods) {
				this.methods.add(method.name());
			}
			this.params = PortletAnnotationMappingUtils.mergeStringArrays(this.params, params);
			this.headers = PortletAnnotationMappingUtils.mergeStringArrays(this.headers, headers);
		}

		public void initPhaseMapping(String phase, String value, String[] params) {
			if (this.phase != null) {
				throw new IllegalStateException(
						"Invalid mapping - more than one phase specified: '" + this.phase + "', '" + phase + "'");
			}
			this.phase = phase;
			this.value = value;
			this.params = PortletAnnotationMappingUtils.mergeStringArrays(this.params, params);
		}

		public boolean match(PortletRequest request) {
			if (!this.modes.isEmpty() && !this.modes.contains(request.getPortletMode())) {
				return false;
			}
			if (StringUtils.hasLength(this.phase) &&
					!this.phase.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE))) {
				return false;
			}
			if (StringUtils.hasLength(this.value)) {
				if (this.phase.equals(PortletRequest.ACTION_PHASE) &&
						!this.value.equals(request.getParameter(ActionRequest.ACTION_NAME))) {
					return false;
				}
				else if (this.phase.equals(PortletRequest.RENDER_PHASE) &&
						!(new WindowState(this.value)).equals(request.getWindowState())) {
					return false;
				}
				else if (this.phase.equals(PortletRequest.RESOURCE_PHASE) &&
						!this.value.equals(((ResourceRequest) request).getResourceID())) {
					return false;
				}
				else if (this.phase.equals(PortletRequest.EVENT_PHASE)) {
					Event event = ((EventRequest) request).getEvent();
					if (!this.value.equals(event.getName()) && !this.value.equals(event.getQName().toString())) {
						return false;
					}
				}
			}
			return (PortletAnnotationMappingUtils.checkRequestMethod(this.methods, request) &&
					PortletAnnotationMappingUtils.checkParameters(this.params, request) &&
					PortletAnnotationMappingUtils.checkHeaders(this.headers, request));
		}

		public boolean isBetterMatchThan(RequestMappingInfo other) {
			return ((!this.modes.isEmpty() && other.modes.isEmpty()) ||
					(StringUtils.hasLength(this.phase) && !StringUtils.hasLength(other.phase)) ||
					(StringUtils.hasLength(this.value) && !StringUtils.hasLength(other.value)) ||
					(!this.methods.isEmpty() && other.methods.isEmpty()) ||
					this.params.length > other.params.length);
		}

		@Override
		public boolean equals(Object obj) {
			RequestMappingInfo other = (RequestMappingInfo) obj;
			return (this.modes.equals(other.modes) &&
					ObjectUtils.nullSafeEquals(this.phase, other.phase) &&
					ObjectUtils.nullSafeEquals(this.value, other.value) &&
					this.methods.equals(other.methods) &&
					Arrays.equals(this.params, other.params) &&
					Arrays.equals(this.headers, other.headers));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.modes) * 29 + this.phase.hashCode());
		}
	}
}
