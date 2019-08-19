package org.springframework.web.servlet.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link HandlerMapping}实现的抽象基类, 用于定义请求与{@link HandlerMethod}之间的映射.
 *
 * <p>对于每个已注册的处理器方法, 都会维护一个唯一的映射, 其子类定义映射类型{@code <T>}的详细信息.
 *
 * @param <T> {@link HandlerMethod}的映射, 包含将处理器方法与传入请求匹配所需的条件.
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * 作用域代理之外的目标bean的Bean名称前缀.
	 * 用于从处理器方法检测中排除这些目标, 以支持相应的代理.
	 * <p>没有在这里检查autowire候选状态, 这是在自动装配级别处理代理目标过滤问题的方式,
	 * 因为autowire-candidate可能由于其他原因而转向{@code false}, 同时仍然期望bean有资格获得处理器方法.
	 * <p>最初在{@link org.springframework.aop.scope.ScopedProxyUtils}中定义, 但在此重复以避免对spring-aop模块的硬依赖.
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new EmptyHandler(), ClassUtils.getMethod(EmptyHandler.class, "handle"));

	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		ALLOW_CORS_CONFIG.addAllowedOrigin("*");
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}


	private boolean detectHandlerMethodsInAncestorContexts = false;

	private HandlerMethodMappingNamingStrategy<T> namingStrategy;

	private final MappingRegistry mappingRegistry = new MappingRegistry();


	/**
	 * 是否在祖先ApplicationContexts中检测bean中的处理器方法.
	 * <p>默认为"false": 仅考虑当前ApplicationContext中的bean,
	 * i.e. 仅在此HandlerMapping本身定义的上下文中 (通常是当前的DispatcherServlet的上下文).
	 * <p>切换此标志以检测祖先上下文中的处理器bean (通常是Spring根WebApplicationContext).
	 */
	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
		this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
	}

	/**
	 * 配置命名策略, 用于为每个映射的处理器方法分配默认名称.
	 * <p>默认命名策略基于类名的大写字母, 后跟"#", 然后是方法名称, e.g. "TC#getFoo" 用于名为TestController的类, 方法为getFoo.
	 */
	public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * 返回配置的命名策略或{@code null}.
	 */
	public HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
		return this.namingStrategy;
	}

	/**
	 * 返回包含所有映射和HandlerMethod的 (只读)映射.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		this.mappingRegistry.acquireReadLock();
		try {
			return Collections.unmodifiableMap(this.mappingRegistry.getMappings());
		}
		finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 返回给定映射名称的处理器方法.
	 * 
	 * @param mappingName 映射名称
	 * 
	 * @return 匹配HandlerMethod的列表或{@code null}; 返回的列表永远不会被修改, 并且可以安全地进行迭代.
	 */
	public List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
		return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
	}

	/**
	 * 返回内部映射注册表. 用于测试.
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * 注册给定的映射.
	 * <p>初始化完成后, 可以在运行时调用此方法.
	 * 
	 * @param mapping 处理器方法的映射
	 * @param handler 处理器
	 * @param method 方法
	 */
	public void registerMapping(T mapping, Object handler, Method method) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * 注销给定的映射.
	 * <p>初始化完成后, 可以在运行时调用此方法.
	 * 
	 * @param mapping 要注销的映射
	 */
	public void unregisterMapping(T mapping) {
		this.mappingRegistry.unregister(mapping);
	}


	// Handler method detection

	/**
	 * 在初始化时检测处理器方法.
	 */
	@Override
	public void afterPropertiesSet() {
		initHandlerMethods();
	}

	/**
	 * 在ApplicationContext中扫描bean, 检测并注册处理器方法.
	 */
	protected void initHandlerMethods() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for request mappings in application context: " + getApplicationContext());
		}
		String[] beanNames = (this.detectHandlerMethodsInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getApplicationContext(), Object.class) :
				getApplicationContext().getBeanNamesForType(Object.class));

		for (String beanName : beanNames) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = getApplicationContext().getType(beanName);
				}
				catch (Throwable ex) {
					// 一个不可解析的bean类型, 可能来自延迟的bean - 忽略它.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (beanType != null && isHandler(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * 在处理器中查找处理器方法.
	 * 
	 * @param handler 处理器或处理器实例的bean名称
	 */
	protected void detectHandlerMethods(final Object handler) {
		Class<?> handlerType = (handler instanceof String ?
				getApplicationContext().getType((String) handler) : handler.getClass());
		final Class<?> userType = ClassUtils.getUserClass(handlerType);

		Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
				new MethodIntrospector.MetadataLookup<T>() {
					@Override
					public T inspect(Method method) {
						try {
							return getMappingForMethod(method, userType);
						}
						catch (Throwable ex) {
							throw new IllegalStateException("Invalid mapping on handler class [" +
									userType.getName() + "]: " + method, ex);
						}
					}
				});

		if (logger.isDebugEnabled()) {
			logger.debug(methods.size() + " request handler methods found on " + userType + ": " + methods);
		}
		for (Map.Entry<Method, T> entry : methods.entrySet()) {
			Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), userType);
			T mapping = entry.getValue();
			registerHandlerMethod(handler, invocableMethod, mapping);
		}
	}

	/**
	 * 注册处理器方法及其唯一映射. 在启动时为每个检测到的处理器方法调用.
	 * 
	 * @param handler 处理器或处理器实例的bean名称
	 * @param method 要注册的方法
	 * @param mapping 与处理器方法关联的映射条件
	 * 
	 * @throws IllegalStateException 如果另一个方法已在同一映射下注册
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * 创建HandlerMethod实例.
	 * 
	 * @param handler bean名称或实际的处理器实例
	 * @param method 目标方法
	 * 
	 * @return 创建的HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName,
					getApplicationContext().getAutowireCapableBeanFactory(), method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 * 提取并返回映射的CORS配置.
	 */
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
		return null;
	}

	/**
	 * 检测到所有处理器方法后调用.
	 * 
	 * @param handlerMethods 带有处理器方法和映射的只读映射
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
	}


	// Handler method lookup

	/**
	 * 查找给定请求的处理器方法.
	 */
	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up handler method for path " + lookupPath);
		}
		this.mappingRegistry.acquireReadLock();
		try {
			HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
			if (logger.isDebugEnabled()) {
				if (handlerMethod != null) {
					logger.debug("Returning handler method [" + handlerMethod + "]");
				}
				else {
					logger.debug("Did not find handler method for [" + lookupPath + "]");
				}
			}
			return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
		}
		finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 查找当前请求的最佳匹配处理器方法.
	 * 如果找到多个匹配项, 则选择最佳匹配项.
	 * 
	 * @param lookupPath 映射当前servlet映射中的查找路径
	 * @param request 当前的请求
	 * 
	 * @return 最佳匹配处理器方法, 或{@code null}
	 */
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
		List<Match> matches = new ArrayList<Match>();
		List<T> directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath);
		if (directPathMatches != null) {
			addMatchingMappings(directPathMatches, matches, request);
		}
		if (matches.isEmpty()) {
			// 别无选择, 只能通过所有映射...
			addMatchingMappings(this.mappingRegistry.getMappings().keySet(), matches, request);
		}

		if (!matches.isEmpty()) {
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
			Collections.sort(matches, comparator);
			if (logger.isTraceEnabled()) {
				logger.trace("Found " + matches.size() + " matching mapping(s) for [" +
						lookupPath + "] : " + matches);
			}
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				if (CorsUtils.isPreFlightRequest(request)) {
					return PREFLIGHT_AMBIGUOUS_MATCH;
				}
				Match secondBestMatch = matches.get(1);
				if (comparator.compare(bestMatch, secondBestMatch) == 0) {
					Method m1 = bestMatch.handlerMethod.getMethod();
					Method m2 = secondBestMatch.handlerMethod.getMethod();
					throw new IllegalStateException("Ambiguous handler methods mapped for HTTP path '" +
							request.getRequestURL() + "': {" + m1 + ", " + m2 + "}");
				}
			}
			handleMatch(bestMatch.mapping, lookupPath, request);
			return bestMatch.handlerMethod;
		}
		else {
			return handleNoMatch(this.mappingRegistry.getMappings().keySet(), lookupPath, request);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
		for (T mapping : mappings) {
			T match = getMatchingMapping(mapping, request);
			if (match != null) {
				matches.add(new Match(match, this.mappingRegistry.getMappings().get(mapping)));
			}
		}
	}

	/**
	 * 找到匹配的映射时调用.
	 * 
	 * @param mapping 匹配映射
	 * @param lookupPath 映射当前servlet映射中的查找路径
	 * @param request 当前的请求
	 */
	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * 未找到匹配的映射时调用.
	 * 
	 * @param mappings 所有注册的映射
	 * @param lookupPath 映射当前servlet映射中的查找路径
	 * @param request 当前的请求
	 * 
	 * @throws ServletException
	 */
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
			throws Exception {

		return null;
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
			}
			else {
				CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);
				corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);
			}
		}
		return corsConfig;
	}


	// Abstract template methods

	/**
	 * 给定类型是否是具有处理器方法的处理器.
	 * 
	 * @param beanType 被检查的bean的类型
	 * 
	 * @return "true" 如果是一个处理器类型, 否则"false".
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * 提供处理器方法的映射. 不能提供映射的方法不是处理器方法.
	 * 
	 * @param method 提供映射的方法
	 * @param handlerType 处理器类型, 可能是方法声明类的子类型
	 * 
	 * @return 映射, 或{@code null}
	 */
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * 提取并返回映射中包含的URL路径.
	 */
	protected abstract Set<String> getMappingPathPatterns(T mapping);

	/**
	 * 检查映射是否与当前请求匹配, 并返回具有与当前请求相关的条件的 (可能是新的)映射.
	 * 
	 * @param mapping 获取匹配的映射
	 * @param request 当前的HTTP servlet请求
	 * 
	 * @return 匹配, 或{@code null}
	 */
	protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);

	/**
	 * 返回比较器以对匹配映射进行排序.
	 * 返回的比较器应该排序'更好'匹配更高.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 比较器 (never {@code null})
	 */
	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);


	/**
	 * 一个注册表, 用于维护处理器方法的所有映射, 公开执行查找和提供并发访问的方法.
	 *
	 * <p>包级私有用于测试.
	 */
	class MappingRegistry {

		private final Map<T, MappingRegistration<T>> registry = new HashMap<T, MappingRegistration<T>>();

		private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<T, HandlerMethod>();

		private final MultiValueMap<String, T> urlLookup = new LinkedMultiValueMap<String, T>();

		private final Map<String, List<HandlerMethod>> nameLookup =
				new ConcurrentHashMap<String, List<HandlerMethod>>();

		private final Map<HandlerMethod, CorsConfiguration> corsLookup =
				new ConcurrentHashMap<HandlerMethod, CorsConfiguration>();

		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

		/**
		 * 返回所有映射和处理器方法. 不是线程安全的.
		 */
		public Map<T, HandlerMethod> getMappings() {
			return this.mappingLookup;
		}

		/**
		 * 返回给定URL路径的匹配项. 不是线程安全的.
		 */
		public List<T> getMappingsByUrl(String urlPath) {
			return this.urlLookup.get(urlPath);
		}

		/**
		 * 通过映射名称返回处理器方法. 线程安全的.
		 */
		public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
			return this.nameLookup.get(mappingName);
		}

		/**
		 * 返回CORS配置. 线程安全的.
		 */
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
			return this.corsLookup.get(original != null ? original : handlerMethod);
		}

		/**
		 * 使用getMappings和getMappingsByUrl时获取读锁.
		 */
		public void acquireReadLock() {
			this.readWriteLock.readLock().lock();
		}

		/**
		 * 使用getMappings和getMappingsByUrl后释放读锁.
		 */
		public void releaseReadLock() {
			this.readWriteLock.readLock().unlock();
		}

		public void register(T mapping, Object handler, Method method) {
			this.readWriteLock.writeLock().lock();
			try {
				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
				assertUniqueMethodMapping(handlerMethod, mapping);

				if (logger.isInfoEnabled()) {
					logger.info("Mapped \"" + mapping + "\" onto " + handlerMethod);
				}
				this.mappingLookup.put(mapping, handlerMethod);

				List<String> directUrls = getDirectUrls(mapping);
				for (String url : directUrls) {
					this.urlLookup.add(url, mapping);
				}

				String name = null;
				if (getNamingStrategy() != null) {
					name = getNamingStrategy().getName(handlerMethod, mapping);
					addMappingName(name, handlerMethod);
				}

				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
				if (corsConfig != null) {
					this.corsLookup.put(handlerMethod, corsConfig);
				}

				this.registry.put(mapping, new MappingRegistration<T>(mapping, handlerMethod, directUrls, name));
			}
			finally {
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void assertUniqueMethodMapping(HandlerMethod newHandlerMethod, T mapping) {
			HandlerMethod handlerMethod = this.mappingLookup.get(mapping);
			if (handlerMethod != null && !handlerMethod.equals(newHandlerMethod)) {
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" +	newHandlerMethod.getBean() + "' method \n" +
						newHandlerMethod + "\nto " + mapping + ": There is already '" +
						handlerMethod.getBean() + "' bean method\n" + handlerMethod + " mapped.");
			}
		}

		private List<String> getDirectUrls(T mapping) {
			List<String> urls = new ArrayList<String>(1);
			for (String path : getMappingPathPatterns(mapping)) {
				if (!getPathMatcher().isPattern(path)) {
					urls.add(path);
				}
			}
			return urls;
		}

		private void addMappingName(String name, HandlerMethod handlerMethod) {
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			if (oldList == null) {
				oldList = Collections.<HandlerMethod>emptyList();
			}

			for (HandlerMethod current : oldList) {
				if (handlerMethod.equals(current)) {
					return;
				}
			}

			if (logger.isTraceEnabled()) {
				logger.trace("Mapping name '" + name + "'");
			}

			List<HandlerMethod> newList = new ArrayList<HandlerMethod>(oldList.size() + 1);
			newList.addAll(oldList);
			newList.add(handlerMethod);
			this.nameLookup.put(name, newList);

			if (newList.size() > 1) {
				if (logger.isTraceEnabled()) {
					logger.trace("Mapping name clash for handlerMethods " + newList +
							". Consider assigning explicit names.");
				}
			}
		}

		public void unregister(T mapping) {
			this.readWriteLock.writeLock().lock();
			try {
				MappingRegistration<T> definition = this.registry.remove(mapping);
				if (definition == null) {
					return;
				}

				this.mappingLookup.remove(definition.getMapping());

				for (String url : definition.getDirectUrls()) {
					List<T> list = this.urlLookup.get(url);
					if (list != null) {
						list.remove(definition.getMapping());
						if (list.isEmpty()) {
							this.urlLookup.remove(url);
						}
					}
				}

				removeMappingName(definition);

				this.corsLookup.remove(definition.getHandlerMethod());
			}
			finally {
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void removeMappingName(MappingRegistration<T> definition) {
			String name = definition.getMappingName();
			if (name == null) {
				return;
			}
			HandlerMethod handlerMethod = definition.getHandlerMethod();
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			if (oldList == null) {
				return;
			}
			if (oldList.size() <= 1) {
				this.nameLookup.remove(name);
				return;
			}
			List<HandlerMethod> newList = new ArrayList<HandlerMethod>(oldList.size() - 1);
			for (HandlerMethod current : oldList) {
				if (!current.equals(handlerMethod)) {
					newList.add(current);
				}
			}
			this.nameLookup.put(name, newList);
		}
	}


	private static class MappingRegistration<T> {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		private final List<String> directUrls;

		private final String mappingName;

		public MappingRegistration(T mapping, HandlerMethod handlerMethod, List<String> directUrls, String mappingName) {
			Assert.notNull(mapping, "Mapping must not be null");
			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
			this.directUrls = (directUrls != null ? directUrls : Collections.<String>emptyList());
			this.mappingName = mappingName;
		}

		public T getMapping() {
			return this.mapping;
		}

		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public List<String> getDirectUrls() {
			return this.directUrls;
		}

		public String getMappingName() {
			return this.mappingName;
		}
	}


	/**
	 * 围绕匹配的HandlerMethod及其映射的轻量级包装器, 用于在当前请求的上下文中使用比较器比较最佳匹配.
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


	private static class EmptyHandler {

		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
