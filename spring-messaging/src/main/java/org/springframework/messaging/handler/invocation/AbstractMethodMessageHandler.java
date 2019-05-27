package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * 基于HandlerMethod的消息处理的抽象基类.
 * 提供在启动时发现处理器方法所需的大部分逻辑, 在运行时为给定消息查找匹配的处理器方法并调用它.
 *
 * <p>还支持发现和调用异常处理方法来处理在消息处理期间引发的异常.
 *
 * @param <T> 包含将{@link org.springframework.messaging.handler.HandlerMethod}映射到传入消息的信息的Object类型
 */
public abstract class AbstractMethodMessageHandler<T>
		implements MessageHandler, ApplicationContextAware, InitializingBean {

	/**
	 * 作用域代理之外的目标bean的Bean名称前缀.
	 * 用于从处理器方法检测中排除这些目标, 以支持相应的代理.
	 * <p>没有在这里检查autowire候选状态, 这是在自动装配级别处理代理目标过滤问题的方式,
	 * 因为autowire候选可能由于其他原因而转向{@code false}, 同时仍然期望bean有资格获得处理器方法.
	 * <p>最初在{@link org.springframework.aop.scope.ScopedProxyUtils}中定义, 但在此重复以避免对spring-aop模块的硬依赖.
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";


	protected final Log logger = LogFactory.getLog(getClass());

	private final List<String> destinationPrefixes = new ArrayList<String>();

	private final List<HandlerMethodArgumentResolver> customArgumentResolvers =
			new ArrayList<HandlerMethodArgumentResolver>(4);

	private final List<HandlerMethodReturnValueHandler> customReturnValueHandlers =
			new ArrayList<HandlerMethodReturnValueHandler>(4);

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private final HandlerMethodReturnValueHandlerComposite returnValueHandlers =
			new HandlerMethodReturnValueHandlerComposite();

	private ApplicationContext applicationContext;

	private final Map<T, HandlerMethod> handlerMethods = new LinkedHashMap<T, HandlerMethod>(64);

	private final MultiValueMap<String, T> destinationLookup = new LinkedMultiValueMap<String, T>(64);

	private final Map<Class<?>, AbstractExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<Class<?>, AbstractExceptionHandlerMethodResolver>(64);

	private final Map<MessagingAdviceBean, AbstractExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<MessagingAdviceBean, AbstractExceptionHandlerMethodResolver>(64);


	/**
	 * 配置此属性时, 只有与配置的前缀之一匹配的目标的消息才有资格进行处理.
	 * 如果匹配, 则删除前缀, 并且仅将目标的剩余部分用于方法映射.
	 * <p>默认不配置前缀, 在这种情况下, 所有消息都有资格进行处理.
	 */
	public void setDestinationPrefixes(Collection<String> prefixes) {
		this.destinationPrefixes.clear();
		if (prefixes != null) {
			for (String prefix : prefixes) {
				prefix = prefix.trim();
				this.destinationPrefixes.add(prefix);
			}
		}
	}

	/**
	 * 返回配置的目标前缀.
	 */
	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	/**
	 * 设置将在支持的参数类型的解析器之后使用的自定义{@code HandlerMethodArgumentResolver}的列表.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		this.customArgumentResolvers.clear();
		if (customArgumentResolvers != null) {
			this.customArgumentResolvers.addAll(customArgumentResolvers);
		}
	}

	/**
	 * 返回已配置的自定义参数解析器.
	 */
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * 设置将在已知类型的返回值处理器之后使用的自定义{@code HandlerMethodReturnValueHandler}的列表.
	 */
	public void setCustomReturnValueHandlers(List<HandlerMethodReturnValueHandler> customReturnValueHandlers) {
		this.customReturnValueHandlers.clear();
		if (customReturnValueHandlers != null) {
			this.customReturnValueHandlers.addAll(customReturnValueHandlers);
		}
	}

	/**
	 * 返回已配置的自定义返回值处理器.
	 */
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * 配置支持的参数类型的完整列表, 有效地覆盖默认配置的参数类型.
	 * 这是一个高级选项; 对于大多数用例, 使用{@link #setCustomArgumentResolvers}就足够了.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers.clear();
			return;
		}
		this.argumentResolvers.addResolvers(argumentResolvers);
	}

	/**
	 * 返回参数解析器的完整列表.
	 */
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers.getResolvers();
	}

	/**
	 * 配置支持的返回值类型的完整列表, 有效地覆盖默认配置的返回值类型.
	 * 这是一个高级选项; 对于大多数用例, 使用{@link #setCustomReturnValueHandlers}就足够了.
	 */
	public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			this.returnValueHandlers.clear();
			return;
		}
		this.returnValueHandlers.addHandlers(returnValueHandlers);
	}

	/**
	 * 返回返回值处理器的完整列表.
	 */
	public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		return this.returnValueHandlers.getReturnValueHandlers();
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
		if (this.argumentResolvers.getResolvers().isEmpty()) {
			this.argumentResolvers.addResolvers(initArgumentResolvers());
		}

		if (this.returnValueHandlers.getReturnValueHandlers().isEmpty()) {
			this.returnValueHandlers.addHandlers(initReturnValueHandlers());
		}

		for (String beanName : this.applicationContext.getBeanNamesForType(Object.class)) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = getApplicationContext().getType(beanName);
				}
				catch (Throwable ex) {
					// 一个不可解析的bean类型, 可能来自延迟的bean - 忽略.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (beanType != null && isHandler(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}
	}

	/**
	 * 返回要使用的参数解析器列表.
	 * 仅在尚未通过{@link #setArgumentResolvers}设置解析器时调用.
	 * <p>子类还应考虑通过{@link #setCustomArgumentResolvers}配置的自定义参数类型.
	 */
	protected abstract List<? extends HandlerMethodArgumentResolver> initArgumentResolvers();

	/**
	 * 返回要使用的返回值处理器列表.
	 * 仅在尚未通过{@link #setReturnValueHandlers}设置返回值处理器时调用.
	 * <p>子类还应考虑通过{@link #setCustomReturnValueHandlers}配置的自定义返回值类型.
	 */
	protected abstract List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers();


	/**
	 * 是否应该为消息传递处理方法内省给定的bean类型.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * 检测给定的处理器是否具有可以处理消息的任何方法, 如果有, 则将其与提取的映射信息一起注册.
	 * 
	 * @param handler 要检查的处理器, 可能是Spring bean名称的实例
	 */
	protected final void detectHandlerMethods(final Object handler) {
		Class<?> handlerType = (handler instanceof String ?
				this.applicationContext.getType((String) handler) : handler.getClass());
		final Class<?> userType = ClassUtils.getUserClass(handlerType);

		Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
				new MethodIntrospector.MetadataLookup<T>() {
					@Override
					public T inspect(Method method) {
						return getMappingForMethod(method, userType);
					}
				});

		if (logger.isDebugEnabled()) {
			logger.debug(methods.size() + " message handler methods found on " + userType + ": " + methods);
		}
		for (Map.Entry<Method, T> entry : methods.entrySet()) {
			registerHandlerMethod(handler, entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 提供处理器方法的映射.
	 * 
	 * @param method 要提供映射的方法
	 * @param handlerType 处理器类型, 可能是方法声明类的子类型
	 * 
	 * @return 映射, 或{@code null} 如果未映射方法
	 */
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * 注册处理器方法及其唯一映射.
	 * 
	 * @param handler 处理器的bean名称, 或处理器实例
	 * @param method 要注册的方法
	 * @param mapping 与处理器方法关联的映射条件
	 * 
	 * @throws IllegalStateException 如果另一个方法已在同一映射下注册
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		Assert.notNull(mapping, "Mapping must not be null");
		HandlerMethod newHandlerMethod = createHandlerMethod(handler, method);
		HandlerMethod oldHandlerMethod = this.handlerMethods.get(mapping);

		if (oldHandlerMethod != null && !oldHandlerMethod.equals(newHandlerMethod)) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + newHandlerMethod.getBean() +
					"' bean method \n" + newHandlerMethod + "\nto " + mapping + ": There is already '" +
					oldHandlerMethod.getBean() + "' bean method\n" + oldHandlerMethod + " mapped.");
		}

		this.handlerMethods.put(mapping, newHandlerMethod);
		if (logger.isInfoEnabled()) {
			logger.info("Mapped \"" + mapping + "\" onto " + newHandlerMethod);
		}

		for (String pattern : getDirectLookupDestinations(mapping)) {
			this.destinationLookup.add(pattern, mapping);
		}
	}

	/**
	 * 从Object处理器创建HandlerMethod实例, 该Object处理器是处理器实例或基于字符串的bean名称.
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName,
					this.applicationContext.getAutowireCapableBeanFactory(), method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 * 返回映射中包含的目标, 这些目标不是模式, 因此适合直接查找.
	 */
	protected abstract Set<String> getDirectLookupDestinations(T mapping);

	/**
	 * 子类可以调用此方法来填充MessagingAdviceBean缓存 (e.g. 支持"global" {@code @MessageExceptionHandler}).
	 */
	protected void registerExceptionHandlerAdvice(
			MessagingAdviceBean bean, AbstractExceptionHandlerMethodResolver resolver) {

		this.exceptionHandlerAdviceCache.put(bean, resolver);
	}

	/**
	 * 返回所有处理器方法及其映射.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		return Collections.unmodifiableMap(this.handlerMethods);
	}


	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		String destination = getDestination(message);
		if (destination == null) {
			return;
		}
		String lookupDestination = getLookupDestination(destination);
		if (lookupDestination == null) {
			return;
		}
		MessageHeaderAccessor headerAccessor = MessageHeaderAccessor.getMutableAccessor(message);
		headerAccessor.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, lookupDestination);
		headerAccessor.setLeaveMutable(true);
		message = MessageBuilder.createMessage(message.getPayload(), headerAccessor.getMessageHeaders());

		if (logger.isDebugEnabled()) {
			logger.debug("Searching methods to handle " +
					headerAccessor.getShortLogMessage(message.getPayload()) +
					", lookupDestination='" + lookupDestination + "'");
		}

		handleMessageInternal(message, lookupDestination);
		headerAccessor.setImmutable();
	}

	protected abstract String getDestination(Message<?> message);

	/**
	 * 检查给定目标 (传入消息的) 是否与其中一个配置的目标前缀匹配, 如果匹配则返回匹配前缀后的目标的剩余部分.
	 * <p>如果没有匹配的前缀, 返回{@code null}.
	 * <p>如果没有目标前缀, 则按原样返回目标.
	 */
	protected String getLookupDestination(String destination) {
		if (destination == null) {
			return null;
		}
		if (CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return destination;
		}
		for (String prefix : this.destinationPrefixes) {
			if (destination.startsWith(prefix)) {
				return destination.substring(prefix.length());
			}
		}
		return null;
	}

	protected void handleMessageInternal(Message<?> message, String lookupDestination) {
		List<Match> matches = new ArrayList<Match>();

		List<T> mappingsByUrl = this.destinationLookup.get(lookupDestination);
		if (mappingsByUrl != null) {
			addMatchesToCollection(mappingsByUrl, message, matches);
		}
		if (matches.isEmpty()) {
			// No direct hits, go through all mappings
			Set<T> allMappings = this.handlerMethods.keySet();
			addMatchesToCollection(allMappings, message, matches);
		}
		if (matches.isEmpty()) {
			handleNoMatch(this.handlerMethods.keySet(), lookupDestination, message);
			return;
		}

		Comparator<Match> comparator = new MatchComparator(getMappingComparator(message));
		Collections.sort(matches, comparator);
		if (logger.isTraceEnabled()) {
			logger.trace("Found " + matches.size() + " handler methods: " + matches);
		}

		Match bestMatch = matches.get(0);
		if (matches.size() > 1) {
			Match secondBestMatch = matches.get(1);
			if (comparator.compare(bestMatch, secondBestMatch) == 0) {
				Method m1 = bestMatch.handlerMethod.getMethod();
				Method m2 = secondBestMatch.handlerMethod.getMethod();
				throw new IllegalStateException("Ambiguous handler methods mapped for destination '" +
						lookupDestination + "': {" + m1 + ", " + m2 + "}");
			}
		}

		handleMatch(bestMatch.mapping, bestMatch.handlerMethod, lookupDestination, message);
	}

	private void addMatchesToCollection(Collection<T> mappingsToCheck, Message<?> message, List<Match> matches) {
		for (T mapping : mappingsToCheck) {
			T match = getMatchingMapping(mapping, message);
			if (match != null) {
				matches.add(new Match(match, this.handlerMethods.get(mapping)));
			}
		}
	}

	/**
	 * 检查映射是否与当前消息匹配, 并返回可能具有与当前请求相关的条件的新映射.
	 * 
	 * @param mapping 要获取匹配的映射
	 * @param message 正在处理的消息
	 * 
	 * @return 匹配或{@code null}如果没有匹配
	 */
	protected abstract T getMatchingMapping(T mapping, Message<?> message);

	protected void handleNoMatch(Set<T> ts, String lookupDestination, Message<?> message) {
		logger.debug("No matching message handler methods.");
	}

	/**
	 * 返回对匹配映射进行排序的比较器.
	 * 返回的比较器应该排序'更好'匹配更高.
	 * 
	 * @param message 当前 Message
	 * 
	 * @return 比较器, never {@code null}
	 */
	protected abstract Comparator<T> getMappingComparator(Message<?> message);

	protected void handleMatch(T mapping, HandlerMethod handlerMethod, String lookupDestination, Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + handlerMethod.getShortLogMessage());
		}
		handlerMethod = handlerMethod.createWithResolvedBean();
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		invocable.setMessageMethodArgumentResolvers(this.argumentResolvers);
		try {
			Object returnValue = invocable.invoke(message);
			MethodParameter returnType = handlerMethod.getReturnType();
			if (void.class == returnType.getParameterType()) {
				return;
			}
			if (this.returnValueHandlers.isAsyncReturnValue(returnValue, returnType)) {
				ListenableFuture<?> future = this.returnValueHandlers.toListenableFuture(returnValue, returnType);
				if (future != null) {
					future.addCallback(new ReturnValueListenableFutureCallback(invocable, message));
				}
			}
			else {
				this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
			}
		}
		catch (Exception ex) {
			processHandlerMethodException(handlerMethod, ex, message);
		}
		catch (Throwable ex) {
			Exception handlingException =
					new MessageHandlingException(message, "Unexpected handler method invocation error", ex);
			processHandlerMethodException(handlerMethod, handlingException, message);
		}
	}

	protected void processHandlerMethodException(HandlerMethod handlerMethod, Exception exception, Message<?> message) {
		InvocableHandlerMethod invocable = getExceptionHandlerMethod(handlerMethod, exception);
		if (invocable == null) {
			logger.error("Unhandled exception from message handler method", exception);
			return;
		}
		invocable.setMessageMethodArgumentResolvers(this.argumentResolvers);
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + invocable.getShortLogMessage());
		}
		try {
			Throwable cause = exception.getCause();
			Object returnValue = (cause != null ?
					invocable.invoke(message, exception, cause, handlerMethod) :
					invocable.invoke(message, exception, handlerMethod));
			MethodParameter returnType = invocable.getReturnType();
			if (void.class == returnType.getParameterType()) {
				return;
			}
			this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
		}
		catch (Throwable ex2) {
			logger.error("Error while processing handler method exception", ex2);
		}
	}

	/**
	 * 查找给定异常的{@code @MessageExceptionHandler}方法.
	 * 默认实现首先搜索HandlerMethod的类层次结构中的方法, 如果没有找到,
	 * 它会继续在配置的{@linkplain org.springframework.messaging.handler.MessagingAdviceBean MessagingAdviceBean}中
	 * 搜索其他的{@code @MessageExceptionHandler}方法.
	 * 
	 * @param handlerMethod 引发异常的方法
	 * @param exception 引发的异常
	 * 
	 * @return 处理异常的方法, 或{@code null}
	 */
	protected InvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching methods to handle " + exception.getClass().getSimpleName());
		}
		Class<?> beanType = handlerMethod.getBeanType();
		AbstractExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(beanType);
		if (resolver == null) {
			resolver = createExceptionHandlerMethodResolverFor(beanType);
			this.exceptionHandlerCache.put(beanType, resolver);
		}
		Method method = resolver.resolveMethod(exception);
		if (method != null) {
			return new InvocableHandlerMethod(handlerMethod.getBean(), method);
		}
		for (MessagingAdviceBean advice : this.exceptionHandlerAdviceCache.keySet()) {
			if (advice.isApplicableToBeanType(beanType)) {
				resolver = this.exceptionHandlerAdviceCache.get(advice);
				method = resolver.resolveMethod(exception);
				if (method != null) {
					return new InvocableHandlerMethod(advice.resolveBean(), method);
				}
			}
		}
		return null;
	}

	protected abstract AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(
			Class<?> beanType);


	@Override
	public String toString() {
		return getClass().getSimpleName() + "[prefixes=" + getDestinationPrefixes() + "]";
	}


	/**
	 * 围绕匹配的HandlerMethod及其匹配的映射的包装器, 用于在消息上下文中使用比较器比较最佳匹配.
	 */
	private class Match {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		public Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	private class ReturnValueListenableFutureCallback implements ListenableFutureCallback<Object> {

		private final InvocableHandlerMethod handlerMethod;

		private final Message<?> message;

		public ReturnValueListenableFutureCallback(InvocableHandlerMethod handlerMethod, Message<?> message) {
			this.handlerMethod = handlerMethod;
			this.message = message;
		}

		@Override
		public void onSuccess(Object result) {
			try {
				MethodParameter returnType = this.handlerMethod.getAsyncReturnValueType(result);
				returnValueHandlers.handleReturnValue(result, returnType, this.message);
			}
			catch (Throwable ex) {
				handleFailure(ex);
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			handleFailure(ex);
		}

		private void handleFailure(Throwable ex) {
			Exception cause = (ex instanceof Exception ? (Exception) ex : new IllegalStateException(ex));
			processHandlerMethodException(this.handlerMethod, cause, this.message);
		}
	}
}
