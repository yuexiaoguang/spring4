package org.springframework.test.context.support;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.test.util.MetaAnnotationUtils.*;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.test.util.MetaAnnotationUtils.*;

/**
 * 使用{@link TestPropertySource @TestPropertySource}
 * 并将测试 {@link PropertySource PropertySources}添加到{@code Environment}的实用方法.
 *
 * <p>主要用于框架内.
 */
public abstract class TestPropertySourceUtils {

	/**
	 * 从<em>内联属性</em>创建的{@link MapPropertySource}的名称.
	 */
	public static final String INLINED_PROPERTIES_PROPERTY_SOURCE_NAME = "Inlined Test Properties";

	private static final Log logger = LogFactory.getLog(TestPropertySourceUtils.class);


	static MergedTestPropertySources buildMergedTestPropertySources(Class<?> testClass) {
		Class<TestPropertySource> annotationType = TestPropertySource.class;
		AnnotationDescriptor<TestPropertySource> descriptor = findAnnotationDescriptor(testClass, annotationType);
		if (descriptor == null) {
			return new MergedTestPropertySources();
		}

		List<TestPropertySourceAttributes> attributesList = resolveTestPropertySourceAttributes(testClass);
		String[] locations = mergeLocations(attributesList);
		String[] properties = mergeProperties(attributesList);
		return new MergedTestPropertySources(locations, properties);
	}

	private static List<TestPropertySourceAttributes> resolveTestPropertySourceAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");
		List<TestPropertySourceAttributes> attributesList = new ArrayList<TestPropertySourceAttributes>();
		Class<TestPropertySource> annotationType = TestPropertySource.class;

		AnnotationDescriptor<TestPropertySource> descriptor = findAnnotationDescriptor(testClass, annotationType);
		Assert.notNull(descriptor, String.format(
				"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
				annotationType.getName(), testClass.getName()));

		while (descriptor != null) {
			TestPropertySource testPropertySource = descriptor.synthesizeAnnotation();
			Class<?> rootDeclaringClass = descriptor.getRootDeclaringClass();
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @TestPropertySource [%s] for declaring class [%s].",
					testPropertySource, rootDeclaringClass.getName()));
			}
			TestPropertySourceAttributes attributes =
					new TestPropertySourceAttributes(rootDeclaringClass, testPropertySource);
			if (logger.isTraceEnabled()) {
				logger.trace("Resolved TestPropertySource attributes: " + attributes);
			}
			attributesList.add(attributes);
			descriptor = findAnnotationDescriptor(rootDeclaringClass.getSuperclass(), annotationType);
		}

		return attributesList;
	}

	private static String[] mergeLocations(List<TestPropertySourceAttributes> attributesList) {
		final List<String> locations = new ArrayList<String>();
		for (TestPropertySourceAttributes attrs : attributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations for TestPropertySource attributes %s", attrs));
			}
			String[] locationsArray = TestContextResourceUtils.convertToClasspathResourcePaths(
					attrs.getDeclaringClass(), attrs.getLocations());
			locations.addAll(0, Arrays.<String> asList(locationsArray));
			if (!attrs.isInheritLocations()) {
				break;
			}
		}
		return StringUtils.toStringArray(locations);
	}

	private static String[] mergeProperties(List<TestPropertySourceAttributes> attributesList) {
		final List<String> properties = new ArrayList<String>();
		for (TestPropertySourceAttributes attrs : attributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing inlined properties for TestPropertySource attributes %s", attrs));
			}
			properties.addAll(0, Arrays.<String>asList(attrs.getProperties()));
			if (!attrs.isInheritProperties()) {
				break;
			}
		}
		return StringUtils.toStringArray(properties);
	}

	/**
	 * 将给定资源{@code locations}中的{@link Properties}文件添加到提供的{@code context}的{@link Environment}中.
	 * <p>此方法委托给
	 * {@link #addPropertiesFilesToEnvironment(ConfigurableEnvironment, ResourceLoader, String...)}.
	 * 
	 * @param context 应该更新其环境的应用程序上下文; never {@code null}
	 * @param locations 要添加到环境中的{@code Properties}文件的资源位置; 可能是空但不能是{@code null}
	 * 
	 * @throws IllegalStateException 如果处理属性文件时发生错误
	 */
	public static void addPropertiesFilesToEnvironment(ConfigurableApplicationContext context, String... locations) {
		Assert.notNull(context, "'context' must not be null");
		Assert.notNull(locations, "'locations' must not be null");
		addPropertiesFilesToEnvironment(context.getEnvironment(), context, locations);
	}

	/**
	 * 将给定资源{@code locations}中的{@link Properties}文件添加到提供的{@link ConfigurableEnvironment 环境}.
	 * <p>资源位置中的属性占位符 (i.e., <code>${...}</code>)将针对{@code Environment}
	 * {@linkplain Environment#resolveRequiredPlaceholders(String) 解析}.
	 * <p>每个属性文件将转换为{@link ResourcePropertySource}, 将添加到具有最高优先级的环境的{@link PropertySources}.
	 * 
	 * @param environment 要更新的环境; never {@code null}
	 * @param resourceLoader 用于加载每个资源的{@code ResourceLoader}; never {@code null}
	 * @param locations 要添加到环境中的{@code Properties}文件的资源位置; 可能为空但不能是{@code null}
	 * 
	 * @throws IllegalStateException 如果处理属性文件时发生错误
	 */
	public static void addPropertiesFilesToEnvironment(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, String... locations) {

		Assert.notNull(environment, "'environment' must not be null");
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		Assert.notNull(locations, "'locations' must not be null");
		try {
			for (String location : locations) {
				String resolvedLocation = environment.resolveRequiredPlaceholders(location);
				Resource resource = resourceLoader.getResource(resolvedLocation);
				environment.getPropertySources().addFirst(new ResourcePropertySource(resource));
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to add PropertySource to Environment", ex);
		}
	}

	/**
	 * 将给定的<em>内联属性</em>添加到提供的{@code context}的{@link Environment}中.
	 * <p>此方法委托给
	 * {@link #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])}.
	 * 
	 * @param context 应该更新其环境的应用程序上下文; never {@code null}
	 * @param inlinedProperties 要添加到环境中的内联属性; 可能为空但不能是{@code null}
	 */
	public static void addInlinedPropertiesToEnvironment(ConfigurableApplicationContext context, String... inlinedProperties) {
		Assert.notNull(context, "'context' must not be null");
		Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
		addInlinedPropertiesToEnvironment(context.getEnvironment(), inlinedProperties);
	}

	/**
	 * 将给定的<em>内联属性</em> (以<em>键值</em>对的形式) 添加到提供的{@link ConfigurableEnvironment 环境}.
	 * <p>所有键值对将作为具有最高优先级的单个{@link MapPropertySource}添加到{@code Environment}.
	 * <p>有关解析<em>内联属性</em>的详细信息, 请参阅{@link #convertInlinedPropertiesToMap}的Javadoc.
	 * 
	 * @param environment 要更新的环境; never {@code null}
	 * @param inlinedProperties 要添加到环境中的内联属性; 可能为空但不能是{@code null}
	 */
	public static void addInlinedPropertiesToEnvironment(ConfigurableEnvironment environment, String... inlinedProperties) {
		Assert.notNull(environment, "'environment' must not be null");
		Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
		if (!ObjectUtils.isEmpty(inlinedProperties)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Adding inlined properties to environment: " +
						ObjectUtils.nullSafeToString(inlinedProperties));
			}
			MapPropertySource ps = (MapPropertySource)
					environment.getPropertySources().get(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
			if (ps == null) {
				ps = new MapPropertySource(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME,
						new LinkedHashMap<String, Object>());
				environment.getPropertySources().addFirst(ps);
			}
			ps.getSource().putAll(convertInlinedPropertiesToMap(inlinedProperties));
		}
	}

	/**
	 * 将提供的<em>内联属性</em> (以<em>键值</em>对的形式)转换为以属性名称作为键的Map, 保留返回的Map中属性名称的顺序.
	 * <p>解析键值对是通过将所有对转换为内存中的<em>虚拟</em>属性文件,
	 * 并委托给{@link Properties#load(java.io.Reader)}来解析每个虚拟文件来实现的.
	 * <p>有关<em>内联属性</em>的完整讨论, 请参阅{@link TestPropertySource#properties}的Javadoc.
	 * 
	 * @param inlinedProperties 要转换的内联属性; 可能为空但不能是{@code null}
	 * 
	 * @return 包含已转换属性的新的有序的Map
	 * @throws IllegalStateException 如果无法解析给定的键值对, 或者给定的内联属性包含多个键值对
	 */
	public static Map<String, Object> convertInlinedPropertiesToMap(String... inlinedProperties) {
		Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Properties props = new Properties();

		for (String pair : inlinedProperties) {
			if (!StringUtils.hasText(pair)) {
				continue;
			}
			try {
				props.load(new StringReader(pair));
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to load test environment property from [" + pair + "]", ex);
			}
			Assert.state(props.size() == 1, "Failed to load exactly one test environment property from [" + pair + "]");
			for (String name : props.stringPropertyNames()) {
				map.put(name, props.getProperty(name));
			}
			props.clear();
		}

		return map;
	}

}
