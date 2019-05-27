package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * 使用Key操作的基础{@link JCacheOperation}.
 */
abstract class AbstractJCacheKeyOperation<A extends Annotation> extends AbstractJCacheOperation<A> {

	private final KeyGenerator keyGenerator;

	private final List<CacheParameterDetail> keyParameterDetails;


	/**
	 * @param methodDetails 与缓存方法相关的{@link CacheMethodDetails}
	 * @param cacheResolver 要解析常规缓存的缓存解析器
	 * @param keyGenerator 用于计算缓存键的Key生成器
	 */
	protected AbstractJCacheKeyOperation(CacheMethodDetails<A> methodDetails,
			CacheResolver cacheResolver, KeyGenerator keyGenerator) {

		super(methodDetails, cacheResolver);
		this.keyGenerator = keyGenerator;
		this.keyParameterDetails = initializeKeyParameterDetails(this.allParameterDetails);
	}


	/**
	 * 返回用于计算缓存键的{@link KeyGenerator}.
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator;
	}

	/**
	 * 返回{@link CacheInvocationParameter}以获取用于计算Key的参数.
	 * <p>根据规范, 如果某些方法参数使用{@link javax.cache.annotation.CacheKey}注解, 则只有那些参数应该是密钥的一部分.
	 * 如果没有注解, 除了使用{@link javax.cache.annotation.CacheValue}注解的参数之外的所有参数都应该是键的一部分.
	 * <p>方法参数必须与相关方法调用的签名匹配
	 * 
	 * @param values 特定调用的参数值
	 * 
	 * @return 用于计算Key的参数的{@link CacheInvocationParameter}实例
	 */
	public CacheInvocationParameter[] getKeyParameters(Object... values) {
		List<CacheInvocationParameter> result = new ArrayList<CacheInvocationParameter>();
		for (CacheParameterDetail keyParameterDetail : this.keyParameterDetails) {
			int parameterPosition = keyParameterDetail.getParameterPosition();
			if (parameterPosition >= values.length) {
				throw new IllegalStateException("Values mismatch, key parameter at position "
						+ parameterPosition + " cannot be matched against " + values.length + " value(s)");
			}
			result.add(keyParameterDetail.toCacheInvocationParameter(values[parameterPosition]));
		}
		return result.toArray(new CacheInvocationParameter[result.size()]);
	}


	private static List<CacheParameterDetail> initializeKeyParameterDetails(List<CacheParameterDetail> allParameters) {
		List<CacheParameterDetail> all = new ArrayList<CacheParameterDetail>();
		List<CacheParameterDetail> annotated = new ArrayList<CacheParameterDetail>();
		for (CacheParameterDetail allParameter : allParameters) {
			if (!allParameter.isValue()) {
				all.add(allParameter);
			}
			if (allParameter.isKey()) {
				annotated.add(allParameter);
			}
		}
		return (annotated.isEmpty() ? all : annotated);
	}

}
