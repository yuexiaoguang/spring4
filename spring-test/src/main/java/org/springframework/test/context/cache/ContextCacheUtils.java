package org.springframework.test.context.cache;

import org.springframework.core.SpringProperties;
import org.springframework.util.StringUtils;

/**
 * 使用{@link ContextCache ContextCaches}的工具集合.
 */
public abstract class ContextCacheUtils {

	/**
	 * 检索{@link ContextCache}的最大大小.
	 * <p>使用{@link SpringProperties}检索名为{@code spring.test.context.cache.maxSize}的系统属性或Spring属性.
	 * <p>如果没有设置此类属性或属性不是整数, 则回退到{@link ContextCache#DEFAULT_MAX_CONTEXT_CACHE_SIZE}的值.
	 * 
	 * @return 上下文缓存的最大大小
	 */
	public static int retrieveMaxCacheSize() {
		try {
			String maxSize = SpringProperties.getProperty(ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME);
			if (StringUtils.hasText(maxSize)) {
				return Integer.parseInt(maxSize.trim());
			}
		}
		catch (Exception ex) {
			// ignore
		}

		// Fallback
		return ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
	}

}
