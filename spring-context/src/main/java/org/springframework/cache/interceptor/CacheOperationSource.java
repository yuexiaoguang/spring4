package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * {@link CacheInterceptor}使用的接口.
 * 实现知道如何从配置、源级别的元数据属性或其他地方获取缓存操作属性.
 */
public interface CacheOperationSource {

	/**
	 * 返回此方法的缓存操作集合;
	 * 如果方法不包含cacheable的注释, 则返回{@code null}.
	 * 
	 * @param method 要反射的方法
	 * @param targetClass 目标类 (may be {@code null}, 在这种情况下, 必须使用方法的声明类)
	 * 
	 * @return 此方法的所有缓存操作, 或{@code null}
	 */
	Collection<CacheOperation> getCacheOperations(Method method, Class<?> targetClass);

}
