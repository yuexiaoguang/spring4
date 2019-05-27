package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheInvocationParameter;

import org.springframework.cache.interceptor.CacheOperationInvocationContext;

/**
 * 所有拦截器使用的默认{@link CacheOperationInvocationContext}实现.
 * 在调用JSR-107 {@link javax.cache.annotation.CacheResolver}时, 还可以实现{@link CacheInvocationContext}作为正确的桥梁
 */
class DefaultCacheInvocationContext<A extends Annotation>
		implements CacheInvocationContext<A>, CacheOperationInvocationContext<JCacheOperation<A>> {

	private final JCacheOperation<A> operation;

	private final Object target;

	private final Object[] args;

	private final CacheInvocationParameter[] allParameters;


	public DefaultCacheInvocationContext(JCacheOperation<A> operation, Object target, Object[] args) {
		this.operation = operation;
		this.target = target;
		this.args = args;
		this.allParameters = operation.getAllParameters(args);
	}


	@Override
	public JCacheOperation<A> getOperation() {
		return this.operation;
	}

	@Override
	public Method getMethod() {
		return this.operation.getMethod();
	}

	@Override
	public Object[] getArgs() {
		return this.args.clone();
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return this.operation.getAnnotations();
	}

	@Override
	public A getCacheAnnotation() {
		return this.operation.getCacheAnnotation();
	}

	@Override
	public String getCacheName() {
		return this.operation.getCacheName();
	}

	@Override
	public Object getTarget() {
		return this.target;
	}

	@Override
	public CacheInvocationParameter[] getAllParameters() {
		return this.allParameters.clone();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		throw new IllegalArgumentException("Cannot unwrap to " + cls);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("CacheInvocationContext{");
		sb.append("operation=").append(this.operation);
		sb.append(", target=").append(this.target);
		sb.append(", args=").append(Arrays.toString(this.args));
		sb.append(", allParameters=").append(Arrays.toString(this.allParameters));
		sb.append('}');
		return sb.toString();
	}

}
