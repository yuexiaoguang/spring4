package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@code TestPropertySourceAttributes}封装了通过{@link TestPropertySource @TestPropertySource}声明的属性.
 *
 * <p>除了封装声明的属性外, {@code TestPropertySourceAttributes}还强制执行配置规则并检测默认属性文件.
 */
class TestPropertySourceAttributes {

	private static final Log logger = LogFactory.getLog(TestPropertySourceAttributes.class);

	private final Class<?> declaringClass;

	private final String[] locations;

	private final boolean inheritLocations;

	private final String[] properties;

	private final boolean inheritProperties;


	/**
	 * 必要时强制执行配置规则并检测默认属性文件.
	 * 
	 * @param declaringClass 声明{@code @TestPropertySource}的类
	 * @param testPropertySource 从中检索属性的注解
	 */
	TestPropertySourceAttributes(Class<?> declaringClass, TestPropertySource testPropertySource) {
		this(declaringClass, testPropertySource.locations(), testPropertySource.inheritLocations(),
			testPropertySource.properties(), testPropertySource.inheritProperties());
	}

	private TestPropertySourceAttributes(Class<?> declaringClass, String[] locations, boolean inheritLocations,
			String[] properties, boolean inheritProperties) {
		Assert.notNull(declaringClass, "declaringClass must not be null");

		if (ObjectUtils.isEmpty(locations) && ObjectUtils.isEmpty(properties)) {
			locations = new String[] { detectDefaultPropertiesFile(declaringClass) };
		}

		this.declaringClass = declaringClass;
		this.locations = locations;
		this.inheritLocations = inheritLocations;
		this.properties = properties;
		this.inheritProperties = inheritProperties;
	}

	/**
	 * 获取声明{@code @TestPropertySource}的{@linkplain Class class}.
	 *
	 * @return 声明类; never {@code null}
	 */
	Class<?> getDeclaringClass() {
		return declaringClass;
	}

	/**
	 * 获取通过{@code @TestPropertySource}声明的资源位置.
	 *
	 * <p>Note: 返回的值可能表示<em>检测到的默认值</em>, 与通过{@code @TestPropertySource}声明的原始值不匹配.
	 *
	 * @return 资源位置; 可能为{@code null} 或<em>empty</em>
	 */
	String[] getLocations() {
		return locations;
	}

	/**
	 * 获取通过{@code @TestPropertySource}声明的{@code inheritLocations}标志.
	 *
	 * @return the {@code inheritLocations} flag
	 */
	boolean isInheritLocations() {
		return inheritLocations;
	}

	/**
	 * 获取通过{@code @TestPropertySource}声明的内联属性.
	 *
	 * @return 内联属性; 可能为{@code null}或<em>空</em>
	 */
	String[] getProperties() {
		return this.properties;
	}

	/**
	 * 获取通过{@code @TestPropertySource}声明的{@code inheritProperties}标志.
	 *
	 * @return the {@code inheritProperties} flag
	 */
	boolean isInheritProperties() {
		return this.inheritProperties;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("declaringClass", declaringClass.getName())//
		.append("locations", ObjectUtils.nullSafeToString(locations))//
		.append("inheritLocations", inheritLocations)//
		.append("properties", ObjectUtils.nullSafeToString(properties))//
		.append("inheritProperties", inheritProperties)//
		.toString();
	}

	/**
	 * 检测所提供类的默认属性文件, 如{@link TestPropertySource}的类级别Javadoc中所指定.
	 */
	private static String detectDefaultPropertiesFile(Class<?> testClass) {
		String resourcePath = ClassUtils.convertClassNameToResourcePath(testClass.getName()) + ".properties";
		String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);

		if (classPathResource.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Detected default properties file \"%s\" for test class [%s]",
					prefixedResourcePath, testClass.getName()));
			}
			return prefixedResourcePath;
		}
		else {
			String msg = String.format("Could not detect default properties file for test [%s]: "
					+ "%s does not exist. Either declare the 'locations' or 'properties' attributes "
					+ "of @TestPropertySource or make the default properties file available.", testClass.getName(),
				classPathResource);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
