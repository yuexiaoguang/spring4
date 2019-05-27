package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.util.ClassUtils;

/**
 * {@link CacheOperation}的抽象实现, 用于缓存方法的属性并实现回退策略:
 * 1. 具体目标方法;
 * 2. 目标类;
 * 3. 声明方法;
 * 4. 声明类/接口.
 *
 * <p>如果没有与目标方法关联, 则默认使用目标类的缓存属性.
 * 与目标方法关联的任何缓存属性都会完全覆盖类缓存属性.
 * 如果在目标类上找不到, 则将检查已调用的调用方法的接口 (如果是JDK代理).
 *
 * <p>此实现在首次使用后按方法缓存属性.
 * 如果希望允许动态更改可缓存的属性(这是非常不可能的), 则可以使缓存可配置.
 */
public abstract class AbstractFallbackCacheOperationSource implements CacheOperationSource {

	/**
	 * 缓存中保存的规范值表示没有找到此方法的缓存属性, 不需要再查看.
	 */
	private static final Collection<CacheOperation> NULL_CACHING_ATTRIBUTE = Collections.emptyList();


	/**
	 * Logger available to subclasses.
	 * <p>由于此基类未标记为Serializable, 序列化后将重新创建记录器 - 只要具体子类是Serializable.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * CacheOperations的缓存, 由特定目标类上的方法作为Key.
	 * <p>由于此基类未标记为Serializable, 序列化后将重新创建缓存 - 只要具体子类是Serializable.
	 */
	private final Map<Object, Collection<CacheOperation>> attributeCache =
			new ConcurrentHashMap<Object, Collection<CacheOperation>>(1024);


	/**
	 * 确定此方法调用的缓存属性.
	 * <p>如果未找到方法属性, 则默认为类的缓存属性.
	 * 
	 * @param method 当前调用的方法 (never {@code null})
	 * @param targetClass 此调用的目标类 (may be {@code null})
	 * 
	 * @return 此方法的{@link CacheOperation}; 如果方法不可缓存, 则为 {@code null}
	 */
	@Override
	public Collection<CacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		Object cacheKey = getCacheKey(method, targetClass);
		Collection<CacheOperation> cached = this.attributeCache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
		}
		else {
			Collection<CacheOperation> cacheOps = computeCacheOperations(method, targetClass);
			if (cacheOps != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Adding cacheable method '" + method.getName() + "' with attribute: " + cacheOps);
				}
				this.attributeCache.put(cacheKey, cacheOps);
			}
			else {
				this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return cacheOps;
		}
	}

	/**
	 * 确定给定方法和目标类的缓存键.
	 * <p>不得为重载方法生成相同的键.
	 * 必须为同一方法的不同实例生成相同的键.
	 * 
	 * @param method 方法 (never {@code null})
	 * @param targetClass 目标类 (may be {@code null})
	 * 
	 * @return 缓存键 (never {@code null})
	 */
	protected Object getCacheKey(Method method, Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	private Collection<CacheOperation> computeCacheOperations(Method method, Class<?> targetClass) {
		// Don't allow no-public methods as required.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// 该方法可以在接口上, 但我们需要来自目标类的属性.
		// 如果目标类为 null, 则方法将保持不变.
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		// 如果我们使用泛型参数处理方法, 请找到原始方法.
		specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

		// 首先尝试的是目标类中的方法.
		Collection<CacheOperation> opDef = findCacheOperations(specificMethod);
		if (opDef != null) {
			return opDef;
		}

		// 第二次尝试是对目标类的缓存操作.
		opDef = findCacheOperations(specificMethod.getDeclaringClass());
		if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
			return opDef;
		}

		if (specificMethod != method) {
			// Fallback is to look at the original method.
			opDef = findCacheOperations(method);
			if (opDef != null) {
				return opDef;
			}
			// Last fallback is the class of the original method.
			opDef = findCacheOperations(method.getDeclaringClass());
			if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
				return opDef;
			}
		}

		return null;
	}


	/**
	 * 子类需要实现它来返回给定类的缓存属性.
	 * 
	 * @param clazz 要检索属性的类
	 * 
	 * @return 与此类关联的所有缓存属性, 或{@code null}
	 */
	protected abstract Collection<CacheOperation> findCacheOperations(Class<?> clazz);

	/**
	 * 子类需要实现它来返回给定方法的缓存属性.
	 * 
	 * @param method 要检索属性的方法
	 * 
	 * @return 与此方法关联的所有缓存属性, 或{@code null}
	 */
	protected abstract Collection<CacheOperation> findCacheOperations(Method method);

	/**
	 * 应该只允许public方法具有缓存语义?
	 * <p>默认实现返回 {@code false}.
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
