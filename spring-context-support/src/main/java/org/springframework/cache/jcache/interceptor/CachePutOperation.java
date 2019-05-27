package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.util.List;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;

/**
 * {@link CachePut}操作的{@link JCacheOperation}实现.
 */
class CachePutOperation extends AbstractJCacheKeyOperation<CachePut> {

	private final ExceptionTypeFilter exceptionTypeFilter;

	private final CacheParameterDetail valueParameterDetail;


	public CachePutOperation(
			CacheMethodDetails<CachePut> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator) {

		super(methodDetails, cacheResolver, keyGenerator);
		CachePut ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.cacheFor(), ann.noCacheFor());
		this.valueParameterDetail = initializeValueParameterDetail(methodDetails.getMethod(), this.allParameterDetails);
		if (this.valueParameterDetail == null) {
			throw new IllegalArgumentException("No parameter annotated with @CacheValue was found for " +
					"" + methodDetails.getMethod());
		}
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * 指定在调用方法之前是否应更新缓存.
	 * 默认情况下, 在方法调用后更新缓存.
	 */
	public boolean isEarlyPut() {
		return !getCacheAnnotation().afterInvocation();
	}

	/**
	 * 返回保存要缓存的值的参数的{@link CacheInvocationParameter}.
	 * <p>方法参数必须与相关方法调用的签名匹配
	 * 
	 * @param values 特定调用的参数值
	 * 
	 * @return 值参数的{@link CacheInvocationParameter}实例
	 */
	public CacheInvocationParameter getValueParameter(Object... values) {
		int parameterPosition = this.valueParameterDetail.getParameterPosition();
		if (parameterPosition >= values.length) {
			throw new IllegalStateException("Values mismatch, value parameter at position " +
					parameterPosition + " cannot be matched against " + values.length + " value(s)");
		}
		return this.valueParameterDetail.toCacheInvocationParameter(values[parameterPosition]);
	}


	private static CacheParameterDetail initializeValueParameterDetail(
			Method method, List<CacheParameterDetail> allParameters) {

		CacheParameterDetail result = null;
		for (CacheParameterDetail parameter : allParameters) {
			if (parameter.isValue()) {
				if (result == null) {
					result = parameter;
				}
				else {
					throw new IllegalArgumentException("More than one @CacheValue found on " + method + "");
				}
			}
		}
		return result;
	}

}
