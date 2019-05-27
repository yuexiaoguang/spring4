package org.springframework.test.context.cache;

import org.springframework.core.SpringProperties;
import org.springframework.util.StringUtils;

/**
 * Collection of utilities for working with {@link ContextCache ContextCaches}.
 */
public abstract class ContextCacheUtils {

	/**
	 * Retrieve the maximum size of the {@link ContextCache}.
	 * <p>Uses {@link SpringProperties} to retrieve a system property or Spring
	 * property named {@code spring.test.context.cache.maxSize}.
	 * <p>Falls back to the value of the {@link ContextCache#DEFAULT_MAX_CONTEXT_CACHE_SIZE}
	 * if no such property has been set or if the property is not an integer.
	 * @return the maximum size of the context cache
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
