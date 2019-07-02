package org.springframework.test.context.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * 用于处理<em>Spring TestContext Framework</em>中的资源的实用方法. 主要供框架内部使用.
 */
public abstract class TestContextResourceUtils {

	private static final String SLASH = "/";


	private TestContextResourceUtils() {
		/* prevent instantiation */
	}

	/**
	 * 将提供的路径转换为类路径资源路径.
	 *
	 * <p>对于每个提供的路径:
	 * <ul>
	 * <li>一个普通路径 &mdash; 例如, {@code "context.xml"} &mdash; 将被视为相对于定义指定类的包的类路径资源.
	 * <li>以斜杠开头的路径将被视为类路径中的绝对路径, 例如: {@code "/org/example/schema.sql"}.
	 * <li>以URL协议为前缀的路径
	 * (e.g., {@link ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link ResourceUtils#FILE_URL_PREFIX file:}, {@code http:}, etc.)
	 * 将被{@link StringUtils#cleanPath 清理}, 但在其他方面没有改变.
	 *
	 * @param clazz 与路径关联的类
	 * @param paths 要转换的路径
	 * 
	 * @return 一个新的已转换的资源路径数组
	 */
	public static String[] convertToClasspathResourcePaths(Class<?> clazz, String... paths) {
		String[] convertedPaths = new String[paths.length];
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (path.startsWith(SLASH)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + path;
			}
			else if (!ResourcePatternUtils.isUrl(path)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + SLASH
						+ StringUtils.cleanPath(ClassUtils.classPackageAsResourcePath(clazz) + SLASH + path);
			}
			else {
				convertedPaths[i] = StringUtils.cleanPath(path);
			}
		}
		return convertedPaths;
	}

	/**
	 * 使用给定的{@link ResourceLoader}将提供的路径转换为{@link Resource}句柄数组.
	 *
	 * @param resourceLoader 用于转换路径的{@code ResourceLoader}
	 * @param paths 要转换的路径
	 * 
	 * @return 一组资源
	 */
	public static Resource[] convertToResources(ResourceLoader resourceLoader, String... paths) {
		List<Resource> list = convertToResourceList(resourceLoader, paths);
		return list.toArray(new Resource[list.size()]);
	}

	/**
	 * 使用给定的{@link ResourceLoader}将提供的路径转换为{@link Resource}句柄列表.
	 *
	 * @param resourceLoader 用于转换路径的{@code ResourceLoader}
	 * @param paths 要转换的路径
	 * 
	 * @return 一组资源
	 */
	public static List<Resource> convertToResourceList(ResourceLoader resourceLoader, String... paths) {
		List<Resource> list = new ArrayList<Resource>();
		for (String path : paths) {
			list.add(resourceLoader.getResource(path));
		}
		return list;
	}
}
