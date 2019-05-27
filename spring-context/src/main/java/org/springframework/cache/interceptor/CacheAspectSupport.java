package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于缓存切面的基类, 例如{@link CacheInterceptor}或AspectJ切面.
 *
 * <p>这使得底层的Spring缓存基础结构, 可以轻松地用于实现任何切面系统的切面.
 *
 * <p>子类负责以正确的顺序调用相关方法.
 *
 * <p>使用<b>策略</b>设计模式. {@link CacheOperationSource}用于确定缓存操作,
 * {@link KeyGenerator}将构建缓存键, {@link CacheResolver}将解析要使用的实际缓存.
 *
 * <p>Note: 缓存切面是可序列化的, 但在反序列化后不执行任何实际缓存.
 */
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {

	private static Class<?> javaUtilOptionalClass = null;

	static {
		try {
			javaUtilOptionalClass =
					ClassUtils.forName("java.util.Optional", CacheAspectSupport.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// Java 8 not available - Optional references simply not supported then.
		}
	}

	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<CacheOperationCacheKey, CacheOperationMetadata> metadataCache =
			new ConcurrentHashMap<CacheOperationCacheKey, CacheOperationMetadata>(1024);

	private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

	private CacheOperationSource cacheOperationSource;

	private KeyGenerator keyGenerator = new SimpleKeyGenerator();

	private CacheResolver cacheResolver;

	private BeanFactory beanFactory;

	private boolean initialized = false;


	/**
	 * 设置一个或多个用于查找缓存属性的缓存操作源.
	 * 如果提供了多个源, 则使用 {@link CompositeCacheOperationSource}聚合它们.
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		Assert.notEmpty(cacheOperationSources, "At least 1 CacheOperationSource needs to be specified");
		this.cacheOperationSource = (cacheOperationSources.length > 1 ?
				new CompositeCacheOperationSource(cacheOperationSources) : cacheOperationSources[0]);
	}

	/**
	 * 返回此缓存切面的CacheOperationSource.
	 */
	public CacheOperationSource getCacheOperationSource() {
		return this.cacheOperationSource;
	}

	/**
	 * 如果没有为操作设置特定的Key生成器, 请设置此缓存切面应该委派给的默认{@link KeyGenerator}.
	 * <p>默认是{@link SimpleKeyGenerator}.
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * 返回此缓存切面委派给的默认{@link KeyGenerator}.
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator;
	}

	/**
	 * 如果没有为操作设置特定的缓存解析器, 设置此缓存切面应该委派给的默认{@link CacheResolver}.
	 * <p>默认解析器根据其名称和默认缓存管理器解析缓存.
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.cacheResolver = cacheResolver;
	}

	/**
	 * 返回此缓存切面委派给的默认{@link CacheResolver}.
	 */
	public CacheResolver getCacheResolver() {
		return this.cacheResolver;
	}

	/**
	 * 设置用于创建默认{@link CacheResolver}的{@link CacheManager}.
	 * 替换当前{@link CacheResolver}.
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheResolver = new SimpleCacheResolver(cacheManager);
	}

	/**
	 * 为{@link CacheManager}和其他服务查找设置包含的{@link BeanFactory}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * @deprecated as of 4.3, in favor of {@link #setBeanFactory}
	 */
	@Deprecated
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.beanFactory = applicationContext;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.state(getCacheOperationSource() != null, "The 'cacheOperationSources' property is required: " +
				"If there are no cacheable methods, then don't use a cache aspect.");
		Assert.state(getErrorHandler() != null, "The 'errorHandler' property is required");
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (getCacheResolver() == null) {
			// 通过默认缓存管理器延迟初始化缓存解析器...
			try {
				setCacheManager(this.beanFactory.getBean(CacheManager.class));
			}
			catch (NoUniqueBeanDefinitionException ex) {
				throw new IllegalStateException("No CacheResolver specified, and no unique bean of type " +
						"CacheManager found. Mark one as primary or declare a specific CacheManager to use.");
			}
			catch (NoSuchBeanDefinitionException ex) {
				throw new IllegalStateException("No CacheResolver specified, and no bean of type CacheManager found. " +
						"Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.");
			}
		}
		this.initialized = true;
	}


	/**
	 * 返回此Method的String表示形式以便在日志记录中使用.
	 * 可以在子类中重写, 以便为给定方法提供不同的标识符.
	 * 
	 * @param method 感兴趣的方法
	 * @param targetClass 该方法所在的类
	 * 
	 * @return 标识此方法的日志消息
	 */
	protected String methodIdentification(Method method, Class<?> targetClass) {
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		return ClassUtils.getQualifiedMethodName(specificMethod);
	}

	protected Collection<? extends Cache> getCaches(
			CacheOperationInvocationContext<CacheOperation> context, CacheResolver cacheResolver) {

		Collection<? extends Cache> caches = cacheResolver.resolveCaches(context);
		if (caches.isEmpty()) {
			throw new IllegalStateException("No cache could be resolved for '" +
					context.getOperation() + "' using resolver '" + cacheResolver +
					"'. At least one cache should be provided per cache operation.");
		}
		return caches;
	}

	protected CacheOperationContext getOperationContext(
			CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {

		CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
		return new CacheOperationContext(metadata, args, target);
	}

	/**
	 * 返回指定操作的{@link CacheOperationMetadata}.
	 * <p>解析要用于操作的{@link CacheResolver}和{@link KeyGenerator}.
	 * 
	 * @param operation 操作
	 * @param method 调用操作的方法
	 * @param targetClass 目标类型
	 * 
	 * @return 已解析的操作元数据
	 */
	protected CacheOperationMetadata getCacheOperationMetadata(
			CacheOperation operation, Method method, Class<?> targetClass) {

		CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
		CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
		if (metadata == null) {
			KeyGenerator operationKeyGenerator;
			if (StringUtils.hasText(operation.getKeyGenerator())) {
				operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
			}
			else {
				operationKeyGenerator = getKeyGenerator();
			}
			CacheResolver operationCacheResolver;
			if (StringUtils.hasText(operation.getCacheResolver())) {
				operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
			}
			else if (StringUtils.hasText(operation.getCacheManager())) {
				CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
				operationCacheResolver = new SimpleCacheResolver(cacheManager);
			}
			else {
				operationCacheResolver = getCacheResolver();
			}
			metadata = new CacheOperationMetadata(operation, method, targetClass,
					operationKeyGenerator, operationCacheResolver);
			this.metadataCache.put(cacheKey, metadata);
		}
		return metadata;
	}

	/**
	 * 返回具有指定名称和类型的bean. 用于解析{@link CacheOperation}中按名称引用的服务.
	 * 
	 * @param beanName bean的名称, 由操作定义
	 * @param expectedType bean的类型
	 * 
	 * @return 与该名称匹配的bean
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果这样的bean不存在
	 */
	protected <T> T getBean(String beanName, Class<T> expectedType) {
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, expectedType, beanName);
	}

	/**
	 * 清除缓存的元数据.
	 */
	protected void clearMetadataCache() {
		this.metadataCache.clear();
		this.evaluator.clear();
	}

	protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
		// 检查切面是否已启用 (以应对AJ被自动拉入的情况)
		if (this.initialized) {
			Class<?> targetClass = getTargetClass(target);
			Collection<CacheOperation> operations = getCacheOperationSource().getCacheOperations(method, targetClass);
			if (!CollectionUtils.isEmpty(operations)) {
				return execute(invoker, method, new CacheOperationContexts(operations, method, args, target, targetClass));
			}
		}

		return invoker.invoke();
	}

	/**
	 * 执行底层操作 (通常在缓存未命中的情况下), 并返回调用的结果.
	 * 如果发生异常, 它将被包装在 {@link CacheOperationInvoker.ThrowableWrapper}中:
	 * 可以处理或修改异常, 但它必须包含在{@link CacheOperationInvoker.ThrowableWrapper}中.
	 * 
	 * @param invoker 处理缓存操作的调用者
	 * 
	 * @return 调用的结果
	 */
	protected Object invokeOperation(CacheOperationInvoker invoker) {
		return invoker.invoke();
	}

	private Class<?> getTargetClass(Object target) {
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
		if (targetClass == null && target != null) {
			targetClass = target.getClass();
		}
		return targetClass;
	}

	private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
		// Special handling of synchronized invocation
		if (contexts.isSynchronized()) {
			CacheOperationContext context = contexts.get(CacheableOperation.class).iterator().next();
			if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
				Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
				Cache cache = context.getCaches().iterator().next();
				try {
					return wrapCacheValue(method, cache.get(key, new Callable<Object>() {
						@Override
						public Object call() throws Exception {
							return unwrapReturnValue(invokeOperation(invoker));
						}
					}));
				}
				catch (Cache.ValueRetrievalException ex) {
					// 调用者在ThrowableWrapper实例中包装任何Throwable, 这样我们就可以确保一个在堆栈中冒泡.
					throw (CacheOperationInvoker.ThrowableWrapper) ex.getCause();
				}
			}
			else {
				// 不需要缓存, 只调用底层方法
				return invokeOperation(invoker);
			}
		}


		// 处理任何早期驱逐
		processCacheEvicts(contexts.get(CacheEvictOperation.class), true,
				CacheOperationExpressionEvaluator.NO_RESULT);

		// 检查是否有符合条件的缓存项
		Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(CacheableOperation.class));

		// Collect puts from any @Cacheable miss, if no cached item is found
		List<CachePutRequest> cachePutRequests = new LinkedList<CachePutRequest>();
		if (cacheHit == null) {
			collectPutRequests(contexts.get(CacheableOperation.class),
					CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
		}

		Object cacheValue;
		Object returnValue;

		if (cacheHit != null && cachePutRequests.isEmpty() && !hasCachePut(contexts)) {
			// If there are no put requests, just use the cache hit
			cacheValue = cacheHit.get();
			returnValue = wrapCacheValue(method, cacheValue);
		}
		else {
			// Invoke the method if we don't have a cache hit
			returnValue = invokeOperation(invoker);
			cacheValue = unwrapReturnValue(returnValue);
		}

		// Collect any explicit @CachePuts
		collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);

		// Process any collected put requests, either from @CachePut or a @Cacheable miss
		for (CachePutRequest cachePutRequest : cachePutRequests) {
			cachePutRequest.apply(cacheValue);
		}

		// Process any late evictions
		processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);

		return returnValue;
	}

	private Object wrapCacheValue(Method method, Object cacheValue) {
		if (method.getReturnType() == javaUtilOptionalClass &&
				(cacheValue == null || cacheValue.getClass() != javaUtilOptionalClass)) {
			return OptionalUnwrapper.wrap(cacheValue);
		}
		return cacheValue;
	}

	private Object unwrapReturnValue(Object returnValue) {
		if (returnValue != null && returnValue.getClass() == javaUtilOptionalClass) {
			return OptionalUnwrapper.unwrap(returnValue);
		}
		return returnValue;
	}

	private boolean hasCachePut(CacheOperationContexts contexts) {
		// 在没有结果对象的情况下评估条件, 因为我们还没有它...
		Collection<CacheOperationContext> cachePutContexts = contexts.get(CachePutOperation.class);
		Collection<CacheOperationContext> excluded = new ArrayList<CacheOperationContext>();
		for (CacheOperationContext context : cachePutContexts) {
			try {
				if (!context.isConditionPassing(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE)) {
					excluded.add(context);
				}
			}
			catch (VariableNotAvailableException ex) {
				// Ignoring failure due to missing result, consider the cache put has to proceed
			}
		}
		// 检查是否已按条件排除所有puts
		return (cachePutContexts.size() != excluded.size());
	}

	private void processCacheEvicts(Collection<CacheOperationContext> contexts, boolean beforeInvocation, Object result) {
		for (CacheOperationContext context : contexts) {
			CacheEvictOperation operation = (CacheEvictOperation) context.metadata.operation;
			if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
				performCacheEvict(context, operation, result);
			}
		}
	}

	private void performCacheEvict(CacheOperationContext context, CacheEvictOperation operation, Object result) {
		Object key = null;
		for (Cache cache : context.getCaches()) {
			if (operation.isCacheWide()) {
				logInvalidating(context, operation, null);
				doClear(cache);
			}
			else {
				if (key == null) {
					key = context.generateKey(result);
				}
				logInvalidating(context, operation, key);
				doEvict(cache, key);
			}
		}
	}

	private void logInvalidating(CacheOperationContext context, CacheEvictOperation operation, Object key) {
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
					" for operation " + operation + " on method " + context.metadata.method);
		}
	}

	/**
	 * 仅为传递条件的{@link CacheableOperation}查找缓存项.
	 * 
	 * @param contexts 可缓存的操作
	 * 
	 * @return 持有缓存的项目的{@link Cache.ValueWrapper}, 或{@code null}
	 */
	private Cache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
		Object result = CacheOperationExpressionEvaluator.NO_RESULT;
		for (CacheOperationContext context : contexts) {
			if (isConditionPassing(context, result)) {
				Object key = generateKey(context, result);
				Cache.ValueWrapper cached = findInCaches(context, key);
				if (cached != null) {
					return cached;
				}
				else {
					if (logger.isTraceEnabled()) {
						logger.trace("No cache entry for key '" + key + "' in cache(s) " + context.getCacheNames());
					}
				}
			}
		}
		return null;
	}

	/**
	 * 使用指定的结果项为所有{@link CacheOperation}收集{@link CachePutRequest}.
	 * 
	 * @param contexts 要处理的上下文
	 * @param result 结果项 (never {@code null})
	 * @param putRequests 要更新的集合
	 */
	private void collectPutRequests(Collection<CacheOperationContext> contexts,
			Object result, Collection<CachePutRequest> putRequests) {

		for (CacheOperationContext context : contexts) {
			if (isConditionPassing(context, result)) {
				Object key = generateKey(context, result);
				putRequests.add(new CachePutRequest(context, key));
			}
		}
	}

	private Cache.ValueWrapper findInCaches(CacheOperationContext context, Object key) {
		for (Cache cache : context.getCaches()) {
			Cache.ValueWrapper wrapper = doGet(cache, key);
			if (wrapper != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
				}
				return wrapper;
			}
		}
		return null;
	}

	private boolean isConditionPassing(CacheOperationContext context, Object result) {
		boolean passing = context.isConditionPassing(result);
		if (!passing && logger.isTraceEnabled()) {
			logger.trace("Cache condition failed on method " + context.metadata.method +
					" for operation " + context.metadata.operation);
		}
		return passing;
	}

	private Object generateKey(CacheOperationContext context, Object result) {
		Object key = context.generateKey(result);
		if (key == null) {
			throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
					"using named params on classes without debug info?) " + context.metadata.operation);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Computed cache key '" + key + "' for operation " + context.metadata.operation);
		}
		return key;
	}


	private class CacheOperationContexts {

		private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts =
				new LinkedMultiValueMap<Class<? extends CacheOperation>, CacheOperationContext>();

		private final boolean sync;

		public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method,
				Object[] args, Object target, Class<?> targetClass) {

			for (CacheOperation operation : operations) {
				this.contexts.add(operation.getClass(), getOperationContext(operation, method, args, target, targetClass));
			}
			this.sync = determineSyncFlag(method);
		}

		public Collection<CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
			Collection<CacheOperationContext> result = this.contexts.get(operationClass);
			return (result != null ? result : Collections.<CacheOperationContext>emptyList());
		}

		public boolean isSynchronized() {
			return this.sync;
		}

		private boolean determineSyncFlag(Method method) {
			List<CacheOperationContext> cacheOperationContexts = this.contexts.get(CacheableOperation.class);
			if (cacheOperationContexts == null) {  // no @Cacheable operation at all
				return false;
			}
			boolean syncEnabled = false;
			for (CacheOperationContext cacheOperationContext : cacheOperationContexts) {
				if (((CacheableOperation) cacheOperationContext.getOperation()).isSync()) {
					syncEnabled = true;
					break;
				}
			}
			if (syncEnabled) {
				if (this.contexts.size() > 1) {
					throw new IllegalStateException("@Cacheable(sync=true) cannot be combined with other cache operations on '" + method + "'");
				}
				if (cacheOperationContexts.size() > 1) {
					throw new IllegalStateException("Only one @Cacheable(sync=true) entry is allowed on '" + method + "'");
				}
				CacheOperationContext cacheOperationContext = cacheOperationContexts.iterator().next();
				CacheableOperation operation = (CacheableOperation) cacheOperationContext.getOperation();
				if (cacheOperationContext.getCaches().size() > 1) {
					throw new IllegalStateException("@Cacheable(sync=true) only allows a single cache on '" + operation + "'");
				}
				if (StringUtils.hasText(operation.getUnless())) {
					throw new IllegalStateException("@Cacheable(sync=true) does not support unless attribute on '" + operation + "'");
				}
				return true;
			}
			return false;
		}
	}


	/**
	 * 缓存操作的元数据, 它不依赖于特定的调用, 这使其成为缓存的良好候选者.
	 */
	protected static class CacheOperationMetadata {

		private final CacheOperation operation;

		private final Method method;

		private final Class<?> targetClass;

		private final KeyGenerator keyGenerator;

		private final CacheResolver cacheResolver;

		public CacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass,
				KeyGenerator keyGenerator, CacheResolver cacheResolver) {

			this.operation = operation;
			this.method = method;
			this.targetClass = targetClass;
			this.keyGenerator = keyGenerator;
			this.cacheResolver = cacheResolver;
		}
	}


	/**
	 * {@link CacheOperation}的{@link CacheOperationInvocationContext}上下文.
	 */
	protected class CacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {

		private final CacheOperationMetadata metadata;

		private final Object[] args;

		private final Object target;

		private final Collection<? extends Cache> caches;

		private final Collection<String> cacheNames;

		private final AnnotatedElementKey methodCacheKey;

		public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
			this.metadata = metadata;
			this.args = extractArgs(metadata.method, args);
			this.target = target;
			this.caches = CacheAspectSupport.this.getCaches(this, metadata.cacheResolver);
			this.cacheNames = createCacheNames(this.caches);
			this.methodCacheKey = new AnnotatedElementKey(metadata.method, metadata.targetClass);
		}

		@Override
		public CacheOperation getOperation() {
			return this.metadata.operation;
		}

		@Override
		public Object getTarget() {
			return this.target;
		}

		@Override
		public Method getMethod() {
			return this.metadata.method;
		}

		@Override
		public Object[] getArgs() {
			return this.args;
		}

		private Object[] extractArgs(Method method, Object[] args) {
			if (!method.isVarArgs()) {
				return args;
			}
			Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
			Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
			System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
			System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
			return combinedArgs;
		}

		protected boolean isConditionPassing(Object result) {
			if (StringUtils.hasText(this.metadata.operation.getCondition())) {
				EvaluationContext evaluationContext = createEvaluationContext(result);
				return evaluator.condition(this.metadata.operation.getCondition(),
						this.methodCacheKey, evaluationContext);
			}
			return true;
		}

		protected boolean canPutToCache(Object value) {
			String unless = "";
			if (this.metadata.operation instanceof CacheableOperation) {
				unless = ((CacheableOperation) this.metadata.operation).getUnless();
			}
			else if (this.metadata.operation instanceof CachePutOperation) {
				unless = ((CachePutOperation) this.metadata.operation).getUnless();
			}
			if (StringUtils.hasText(unless)) {
				EvaluationContext evaluationContext = createEvaluationContext(value);
				return !evaluator.unless(unless, this.methodCacheKey, evaluationContext);
			}
			return true;
		}

		/**
		 * 计算给定缓存操作的Key.
		 * 
		 * @return 生成的key, 或{@code null}
		 */
		protected Object generateKey(Object result) {
			if (StringUtils.hasText(this.metadata.operation.getKey())) {
				EvaluationContext evaluationContext = createEvaluationContext(result);
				return evaluator.key(this.metadata.operation.getKey(), this.methodCacheKey, evaluationContext);
			}
			return this.metadata.keyGenerator.generate(this.target, this.metadata.method, this.args);
		}

		private EvaluationContext createEvaluationContext(Object result) {
			return evaluator.createEvaluationContext(this.caches, this.metadata.method, this.args,
					this.target, this.metadata.targetClass, result, beanFactory);
		}

		protected Collection<? extends Cache> getCaches() {
			return this.caches;
		}

		protected Collection<String> getCacheNames() {
			return this.cacheNames;
		}

		private Collection<String> createCacheNames(Collection<? extends Cache> caches) {
			Collection<String> names = new ArrayList<String>();
			for (Cache cache : caches) {
				names.add(cache.getName());
			}
			return names;
		}
	}


	private class CachePutRequest {

		private final CacheOperationContext context;

		private final Object key;

		public CachePutRequest(CacheOperationContext context, Object key) {
			this.context = context;
			this.key = key;
		}

		public void apply(Object result) {
			if (this.context.canPutToCache(result)) {
				for (Cache cache : this.context.getCaches()) {
					doPut(cache, this.key, result);
				}
			}
		}
	}


	private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {

		private final CacheOperation cacheOperation;

		private final AnnotatedElementKey methodCacheKey;

		private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
			this.cacheOperation = cacheOperation;
			this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof CacheOperationCacheKey)) {
				return false;
			}
			CacheOperationCacheKey otherKey = (CacheOperationCacheKey) other;
			return (this.cacheOperation.equals(otherKey.cacheOperation) &&
					this.methodCacheKey.equals(otherKey.methodCacheKey));
		}

		@Override
		public int hashCode() {
			return (this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode());
		}

		@Override
		public String toString() {
			return this.cacheOperation + " on " + this.methodCacheKey;
		}

		@Override
		public int compareTo(CacheOperationCacheKey other) {
			int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
			if (result == 0) {
				result = this.methodCacheKey.compareTo(other.methodCacheKey);
			}
			return result;
		}
	}


	/**
	 * 内部类, 以避免对Java 8的硬依赖.
	 */
	@UsesJava8
	private static class OptionalUnwrapper {

		public static Object unwrap(Object optionalObject) {
			Optional<?> optional = (Optional<?>) optionalObject;
			if (!optional.isPresent()) {
				return null;
			}
			Object result = optional.get();
			Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
			return result;
		}

		public static Object wrap(Object value) {
			return Optional.ofNullable(value);
		}
	}

}
