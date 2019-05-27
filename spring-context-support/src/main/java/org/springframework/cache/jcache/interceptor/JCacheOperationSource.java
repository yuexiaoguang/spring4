package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;

/**
 * {@link JCacheInterceptor}使用的接口. 实现知道如何从标准JSR-107注解中获取缓存操作属性.
 */
public interface JCacheOperationSource {

	/**
	 * 返回此方法的缓存操作; 如果方法不包含<em>JSR-107</em>相关元数据, 则返回{@code null}.
	 *
	 * @param method 要内省的方法
	 * @param targetClass 目标类(可以是{@code null}, 在这种情况下, 必须使用方法的声明类)
	 * 
	 * @return 此方法的缓存操作, 或{@code null}
	 */
	JCacheOperation<?> getCacheOperation(Method method, Class<?> targetClass);

}
