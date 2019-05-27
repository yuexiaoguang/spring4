package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;

/**
 * 默认的{@link CacheKeyInvocationContext}实现.
 */
class DefaultCacheKeyInvocationContext<A extends Annotation>
		extends DefaultCacheInvocationContext<A> implements CacheKeyInvocationContext<A> {

	private final CacheInvocationParameter[] keyParameters;

	private final CacheInvocationParameter valueParameter;

	public DefaultCacheKeyInvocationContext(AbstractJCacheKeyOperation<A> operation,
			Object target, Object[] args) {
		super(operation, target, args);
		this.keyParameters = operation.getKeyParameters(args);
		if (operation instanceof CachePutOperation) {
			this.valueParameter = ((CachePutOperation) operation).getValueParameter(args);
		}
		else {
			this.valueParameter = null;
		}
	}

	@Override
	public CacheInvocationParameter[] getKeyParameters() {
		return keyParameters.clone();
	}

	@Override
	public CacheInvocationParameter getValueParameter() {
		return valueParameter;
	}

}
