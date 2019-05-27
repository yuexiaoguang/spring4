package org.springframework.core.io.support;

import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;

/**
 * 用于确定给定URL是否是可以通过{@link ResourcePatternResolver}加载的资源位置的实用程序类.
 *
 * <p>如果{@link #isUrl(String)}方法返回{@code false}, 调用者通常会认为某个位置是相对路径.
 */
public abstract class ResourcePatternUtils {

	/**
	 * 返回给定资源位置是否为URL:
	 * 特殊的"classpath"或"classpath*"伪URL, 或标准URL.
	 * 
	 * @param resourceLocation 要检查的位置
	 * 
	 * @return 该位置是否符合URL
	 */
	public static boolean isUrl(String resourceLocation) {
		return (resourceLocation != null &&
				(resourceLocation.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX) ||
						ResourceUtils.isUrl(resourceLocation)));
	}

	/**
	 * 返回给定ResourceLoader的默认ResourcePatternResolver.
	 * <p>这可能是ResourceLoader本身, 如果它实现ResourcePatternResolver扩展,
	 * 或者是在给定ResourceLoader上构建的PathMatchingResourcePatternResolver.
	 * 
	 * @param resourceLoader 用于构建模式解析器的ResourceLoader (可能是{@code null}以指示默认的ResourceLoader)
	 * 
	 * @return ResourcePatternResolver
	 */
	public static ResourcePatternResolver getResourcePatternResolver(ResourceLoader resourceLoader) {
		if (resourceLoader instanceof ResourcePatternResolver) {
			return (ResourcePatternResolver) resourceLoader;
		}
		else if (resourceLoader != null) {
			return new PathMatchingResourcePatternResolver(resourceLoader);
		}
		else {
			return new PathMatchingResourcePatternResolver();
		}
	}
}
