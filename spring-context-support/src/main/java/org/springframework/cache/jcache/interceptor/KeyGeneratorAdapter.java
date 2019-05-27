package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Spring的{@link KeyGenerator}实现, 它可以委托给标准的JSR-107 {@link javax.cache.annotation.CacheKeyGenerator},
 * 也可以包装一个标准的{@link KeyGenerator}, 以便只处理相关的参数.
 */
class KeyGeneratorAdapter implements KeyGenerator {

	private final JCacheOperationSource cacheOperationSource;

	private KeyGenerator keyGenerator;

	private CacheKeyGenerator cacheKeyGenerator;


	/**
	 * 使用给定的{@link KeyGenerator}创建实例,
	 * 以便根据规范处理{@link javax.cache.annotation.CacheKey}和{@link javax.cache.annotation.CacheValue}.
	 */
	public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, KeyGenerator target) {
		Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
		Assert.notNull(target, "KeyGenerator must not be null");
		this.cacheOperationSource = cacheOperationSource;
		this.keyGenerator = target;
	}

	/**
	 * 创建用于包装指定的{@link javax.cache.annotation.CacheKeyGenerator}的实例.
	 */
	public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, CacheKeyGenerator target) {
		Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
		Assert.notNull(target, "CacheKeyGenerator must not be null");
		this.cacheOperationSource = cacheOperationSource;
		this.cacheKeyGenerator = target;
	}


	/**
	 * 返回目标Key生成器, 以{@link KeyGenerator}或{@link CacheKeyGenerator}的形式使用.
	 */
	public Object getTarget() {
		return (this.keyGenerator != null ? this.keyGenerator : this.cacheKeyGenerator);
	}

	@Override
	public Object generate(Object target, Method method, Object... params) {
		JCacheOperation<?> operation = this.cacheOperationSource.getCacheOperation(method, target.getClass());
		if (!(AbstractJCacheKeyOperation.class.isInstance(operation))) {
			throw new IllegalStateException("Invalid operation, should be a key-based operation " + operation);
		}
		CacheKeyInvocationContext<?> invocationContext = createCacheKeyInvocationContext(target, operation, params);

		if (this.cacheKeyGenerator != null) {
			return this.cacheKeyGenerator.generateCacheKey(invocationContext);
		}
		else {
			return doGenerate(this.keyGenerator, invocationContext);
		}
	}

	@SuppressWarnings("unchecked")
	private static Object doGenerate(KeyGenerator keyGenerator, CacheKeyInvocationContext<?> context) {
		List<Object> parameters = new ArrayList<Object>();
		for (CacheInvocationParameter param : context.getKeyParameters()) {
			Object value = param.getValue();
			if (param.getParameterPosition() == context.getAllParameters().length - 1 &&
					context.getMethod().isVarArgs()) {
				parameters.addAll((List<Object>) CollectionUtils.arrayToList(value));
			}
			else {
				parameters.add(value);
			}
		}
		return keyGenerator.generate(context.getTarget(), context.getMethod(), parameters.toArray());
	}


	@SuppressWarnings("unchecked")
	private CacheKeyInvocationContext<?> createCacheKeyInvocationContext(
			Object target, JCacheOperation<?> operation, Object[] params) {

		AbstractJCacheKeyOperation<Annotation> keyCacheOperation = (AbstractJCacheKeyOperation<Annotation>) operation;
		return new DefaultCacheKeyInvocationContext<Annotation>(keyCacheOperation, target, params);
	}

}
