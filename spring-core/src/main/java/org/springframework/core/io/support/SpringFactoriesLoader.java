package org.springframework.core.io.support;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 框架内部使用的通用工厂装载机制.
 *
 * <p>{@code SpringFactoriesLoader} {@linkplain #loadFactories loads}
 * 并从 {@value #FACTORIES_RESOURCE_LOCATION}文件中实例化给定类型的工厂, 这些文件可能存在于类路径中的多个JAR文件中.
 * {@code spring.factories}文件必须采用{@link Properties}格式,
 * 其中键是接口或抽象类的完全限定名称, 值是以逗号分隔的实现类名称列表.
 * 例如:
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 *
 * 其中{@code example.MyService}是接口的名称, {@code MyServiceImpl1}和{@code MyServiceImpl2}是两个实现.
 */
public abstract class SpringFactoriesLoader {

	private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

	/**
	 * 寻找工厂的位置.
	 * <p>可以存在于多个JAR文件中.
	 */
	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";


	/**
	 * 使用给定的类加载器从{@value #FACTORIES_RESOURCE_LOCATION}加载并实例化给定类型的工厂实现.
	 * <p>返回的工厂通过{@link AnnotationAwareOrderComparator}排序.
	 * <p>如果需要自定义实例化策略, 使用{@link #loadFactoryNames}获取所有已注册的工厂名称.
	 * 
	 * @param factoryClass 表示工厂的接口或抽象类
	 * @param classLoader 用于加载的ClassLoader (可以是{@code null}使用默认值)
	 * 
	 * @throws IllegalArgumentException 如果无法加载任何工厂实现类, 或者在实例化任何工厂时发生错误
	 */
	public static <T> List<T> loadFactories(Class<T> factoryClass, ClassLoader classLoader) {
		Assert.notNull(factoryClass, "'factoryClass' must not be null");
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
		}
		List<String> factoryNames = loadFactoryNames(factoryClass, classLoaderToUse);
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded [" + factoryClass.getName() + "] names: " + factoryNames);
		}
		List<T> result = new ArrayList<T>(factoryNames.size());
		for (String factoryName : factoryNames) {
			result.add(instantiateFactory(factoryName, factoryClass, classLoaderToUse));
		}
		AnnotationAwareOrderComparator.sort(result);
		return result;
	}

	/**
	 * 使用给定的类加载器从{@value #FACTORIES_RESOURCE_LOCATION}加载给定类型的工厂实现的完全限定类名.
	 * 
	 * @param factoryClass 表示工厂的接口或抽象类
	 * @param classLoader 用于加载资源的ClassLoader; 可以是{@code null}使用默认值
	 * 
	 * @throws IllegalArgumentException 如果加载工厂名称时发生错误
	 */
	public static List<String> loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader) {
		String factoryClassName = factoryClass.getName();
		try {
			Enumeration<URL> urls = (classLoader != null ? classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
			List<String> result = new ArrayList<String>();
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));
				String factoryClassNames = properties.getProperty(factoryClassName);
				result.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(factoryClassNames)));
			}
			return result;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load [" + factoryClass.getName() +
					"] factories from location [" + FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T instantiateFactory(String instanceClassName, Class<T> factoryClass, ClassLoader classLoader) {
		try {
			Class<?> instanceClass = ClassUtils.forName(instanceClassName, classLoader);
			if (!factoryClass.isAssignableFrom(instanceClass)) {
				throw new IllegalArgumentException(
						"Class [" + instanceClassName + "] is not assignable to [" + factoryClass.getName() + "]");
			}
			Constructor<?> constructor = instanceClass.getDeclaredConstructor();
			ReflectionUtils.makeAccessible(constructor);
			return (T) constructor.newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Unable to instantiate factory class: " + factoryClass.getName(), ex);
		}
	}
}
