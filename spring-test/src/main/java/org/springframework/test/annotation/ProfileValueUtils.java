package org.springframework.test.annotation;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 使用<em>配置文件值</em>的方法.
 */
public abstract class ProfileValueUtils {

	private static final Log logger = LogFactory.getLog(ProfileValueUtils.class);


	/**
	 * 通过{@link ProfileValueSourceConfiguration &#064;ProfileValueSourceConfiguration}注解
	 * 检索指定的{@link Class 测试类}的{@link ProfileValueSource}类型, 并实例化该类型的新实例.
	 * <p>如果指定的类上不存在{@link ProfileValueSourceConfiguration  &#064;ProfileValueSourceConfiguration},
	 * 或者未声明自定义{@link ProfileValueSource}, 则将返回默认的{@link SystemProfileValueSource}.
	 * 
	 * @param testClass 应检索ProfileValueSource的测试类
	 * 
	 * @return 指定类的配置的 (或默认的) ProfileValueSource
	 */
	@SuppressWarnings("unchecked")
	public static ProfileValueSource retrieveProfileValueSource(Class<?> testClass) {
		Assert.notNull(testClass, "testClass must not be null");

		Class<ProfileValueSourceConfiguration> annotationType = ProfileValueSourceConfiguration.class;
		ProfileValueSourceConfiguration config = AnnotatedElementUtils.findMergedAnnotation(testClass, annotationType);
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved @ProfileValueSourceConfiguration [" + config + "] for test class [" +
					testClass.getName() + "]");
		}

		Class<? extends ProfileValueSource> profileValueSourceType;
		if (config != null) {
			profileValueSourceType = config.value();
		}
		else {
			profileValueSourceType = (Class<? extends ProfileValueSource>) AnnotationUtils.getDefaultValue(annotationType);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved ProfileValueSource type [" + profileValueSourceType + "] for class [" +
					testClass.getName() + "]");
		}

		ProfileValueSource profileValueSource;
		if (SystemProfileValueSource.class == profileValueSourceType) {
			profileValueSource = SystemProfileValueSource.getInstance();
		}
		else {
			try {
				profileValueSource = profileValueSourceType.newInstance();
			}
			catch (Exception ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not instantiate a ProfileValueSource of type [" + profileValueSourceType +
							"] for class [" + testClass.getName() + "]: using default.", ex);
				}
				profileValueSource = SystemProfileValueSource.getInstance();
			}
		}

		return profileValueSource;
	}

	/**
	 * 确定当前环境中提供的{@code testClass}是否<em>已启用</em>,
	 * 由类级别的{@link IfProfileValue &#064;IfProfileValue}注解指定的.
	 * <p>如果未声明{@link IfProfileValue &#064;IfProfileValue}注解, 则默认{@code true}.
	 * 
	 * @param testClass 测试类
	 * 
	 * @return {@code true}如果在当前环境中<em>启用</em>测试
	 */
	public static boolean isTestEnabledInThisEnvironment(Class<?> testClass) {
		IfProfileValue ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testClass, IfProfileValue.class);
		return isTestEnabledInThisEnvironment(retrieveProfileValueSource(testClass), ifProfileValue);
	}

	/**
	 * 确定当前环境中提供的{@code testMethod}是否<em>已启用</em>,
	 * 由{@link IfProfileValue  &#064;IfProfileValue}注解指定, 该注解可在测试方法本身或类级别上声明.
	 * 类级别使用会覆盖方法级别的使用.
	 * <p>如果未声明{@link IfProfileValue &#064;IfProfileValue}注解, 则默认为{@code true}.
	 * 
	 * @param testMethod 测试方法
	 * @param testClass 测试类
	 * 
	 * @return {@code true}如果在当前环境中<em>启用</em>测试
	 */
	public static boolean isTestEnabledInThisEnvironment(Method testMethod, Class<?> testClass) {
		return isTestEnabledInThisEnvironment(retrieveProfileValueSource(testClass), testMethod, testClass);
	}

	/**
	 * 确定当前环境中提供的{@code testMethod}是否<em>已启用</em>,
	 * 由{@link IfProfileValue &#064;IfProfileValue}注解指定, 该注解可在测试方法本身或类级别上声明.
	 * 类级别使用会覆盖方法级别的使用.
	 * <p>如果未声明{@link IfProfileValue &#064;IfProfileValue}注解, 则默认为{@code true}.
	 * 
	 * @param profileValueSource 用于确定是否启用了测试的ProfileValueSource
	 * @param testMethod 测试方法
	 * @param testClass 测试类
	 * 
	 * @return {@code true} 如果在当前环境中<em>启用</em>测试
	 */
	public static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource, Method testMethod,
			Class<?> testClass) {

		IfProfileValue ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testClass, IfProfileValue.class);
		boolean classLevelEnabled = isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);

		if (classLevelEnabled) {
			ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testMethod, IfProfileValue.class);
			return isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);
		}

		return false;
	}

	/**
	 * 确定当前环境中所提供的{@link IfProfileValue &#064;IfProfileValue}注解
	 * 中的{@code value}(或{@code values}其中一个)是否<em>已启用</em>.
	 * 
	 * @param profileValueSource 用于确定是否启用了测试的ProfileValueSource
	 * @param ifProfileValue 要内省的注解; 可能是{@code null}
	 * 
	 * @return {@code true} 如果在当前环境中<em>启用</em>测试, 或提供的{@code ifProfileValue}是 {@code null}
	 */
	private static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource,
			IfProfileValue ifProfileValue) {

		if (ifProfileValue == null) {
			return true;
		}

		String environmentValue = profileValueSource.get(ifProfileValue.name());
		String[] annotatedValues = ifProfileValue.values();
		if (StringUtils.hasLength(ifProfileValue.value())) {
			if (annotatedValues.length > 0) {
				throw new IllegalArgumentException("Setting both the 'value' and 'values' attributes " +
						"of @IfProfileValue is not allowed: choose one or the other.");
			}
			annotatedValues = new String[] { ifProfileValue.value() };
		}

		for (String value : annotatedValues) {
			if (ObjectUtils.nullSafeEquals(value, environmentValue)) {
				return true;
			}
		}
		return false;
	}

}
