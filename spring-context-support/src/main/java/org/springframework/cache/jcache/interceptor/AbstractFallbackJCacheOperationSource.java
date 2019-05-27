package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.util.ClassUtils;

/**
 * {@link JCacheOperationSource}的抽象实现, 它缓存方法的属性并实现回退策略:
 * 1. 具体目标方法;
 * 2. 声明方法.
 *
 * <p>此实现在首次使用后按方法缓存属性.
 */
public abstract class AbstractFallbackJCacheOperationSource implements JCacheOperationSource {

	/**
	 * 缓存中保存的规范值, 表示没有找到此方法的缓存属性, 我们不需要再查看.
	 */
	private final static Object NULL_CACHING_ATTRIBUTE = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<MethodClassKey, Object> cache = new ConcurrentHashMap<MethodClassKey, Object>(1024);


	@Override
	public JCacheOperation<?> getCacheOperation(Method method, Class<?> targetClass) {
		MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
		Object cached = this.cache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_ATTRIBUTE ? (JCacheOperation<?>) cached : null);
		}
		else {
			JCacheOperation<?> operation = computeCacheOperation(method, targetClass);
			if (operation != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Adding cacheable method '" + method.getName() + "' with operation: " + operation);
				}
				this.cache.put(cacheKey, operation);
			}
			else {
				this.cache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return operation;
		}
	}

	private JCacheOperation<?> computeCacheOperation(Method method, Class<?> targetClass) {
		// 不允许no-public方法是必须的.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// 该方法可以在接口上, 但需要来自目标类的属性.
		// 如果目标类为null, 则方法将保持不变.
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		// 如果使用泛型参数处理方法, 找到原始方法.
		specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

		// 首先尝试的是目标类中的方法.
		JCacheOperation<?> operation = findCacheOperation(specificMethod, targetClass);
		if (operation != null) {
			return operation;
		}
		if (specificMethod != method) {
			// 后退是看原始方法.
			operation = findCacheOperation(method, targetClass);
			if (operation != null) {
				return operation;
			}
		}
		return null;
	}


	/**
	 * 子类需要实现此操作以返回给定方法的缓存操作.
	 * 
	 * @param method 要检索操作的方法
	 * @param targetType 目标类
	 * 
	 * @return 与此方法关联的缓存操作 (如果没有, 则为{@code null})
	 */
	protected abstract JCacheOperation<?> findCacheOperation(Method method, Class<?> targetType);

	/**
	 * 应该只允许公共方法具有缓存语义?
	 * <p>默认实现返回{@code false}.
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
