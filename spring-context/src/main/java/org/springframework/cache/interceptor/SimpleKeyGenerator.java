package org.springframework.cache.interceptor;

import java.lang.reflect.Method;

/**
 * 简单的Key生成器.
 * 如果给出单个非null值, 则返回参数本身; 否则返回参数的{@link SimpleKey}.
 *
 * <p>与{@link DefaultKeyGenerator}不同, 此类生成的Key不会发生冲突.
 * 返回的{@link SimpleKey}对象可以安全地与{@link org.springframework.cache.concurrent.ConcurrentMapCache}一起使用,
 * 但是, 可能不适合所有{@link org.springframework.cache.Cache}实现.
 */
public class SimpleKeyGenerator implements KeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		return generateKey(params);
	}

	/**
	 * 根据指定的参数生成Key.
	 */
	public static Object generateKey(Object... params) {
		if (params.length == 0) {
			return SimpleKey.EMPTY;
		}
		if (params.length == 1) {
			Object param = params[0];
			if (param != null && !param.getClass().isArray()) {
				return param;
			}
		}
		return new SimpleKey(params);
	}

}
