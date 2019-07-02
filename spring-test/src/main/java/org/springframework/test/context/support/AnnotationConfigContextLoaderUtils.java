package org.springframework.test.context.support;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link SmartContextLoader SmartContextLoaders}的实用方法,
 * 用于处理带注解的类 (e.g., {@link Configuration @Configuration}类).
 */
public abstract class AnnotationConfigContextLoaderUtils {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoaderUtils.class);


	/**
	 * 检测提供的测试类的默认配置类.
	 * <p>返回的类数组将包含所提供的类的所有静态嵌套类, 这些类满足{@code @Configuration}类实现的要求,
	 * 如{@link Configuration @Configuration}文档中所指定.
	 * <p>此方法的实现遵循
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} SPI中定义的契约.
	 * 具体来说, 此方法使用内省来检测符合{@code @Configuration}类实现所需约束的默认配置类.
	 * 如果潜在的候选配置类不满足这些要求, 则此方法将记录调试消息, 并且将忽略潜在的候选类.
	 * 
	 * @param declaringClass 声明{@code @ContextConfiguration}的测试类
	 * 
	 * @return 一组默认配置类, 可能为空但不能是{@code null}
	 */
	public static Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		Assert.notNull(declaringClass, "Declaring class must not be null");

		List<Class<?>> configClasses = new ArrayList<Class<?>>();

		for (Class<?> candidate : declaringClass.getDeclaredClasses()) {
			if (isDefaultConfigurationClassCandidate(candidate)) {
				configClasses.add(candidate);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Ignoring class [%s]; it must be static, non-private, non-final, and annotated " +
								"with @Configuration to be considered a default configuration class.",
						candidate.getName()));
				}
			}
		}

		if (configClasses.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Could not detect default configuration classes for test class [%s]: " +
						"%s does not declare any static, non-private, non-final, nested classes " +
						"annotated with @Configuration.", declaringClass.getName(), declaringClass.getSimpleName()));
			}
		}

		return ClassUtils.toClassArray(configClasses);
	}

	/**
	 * 确定提供的{@link Class}是否符合被视为<em>默认配置类</em>候选者的条件.
	 * <p>具体来说, 这样的候选:
	 * <ul>
	 * <li>不能是{@code null}</li>
	 * <li>不能是{@code private}</li>
	 * <li>不能是{@code final}</li>
	 * <li>必须是{@code static}</li>
	 * <li>必须带{@code @Configuration}注解或元元注解</li>
	 * </ul>
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return {@code true} 如果提供的类符合候选标准
	 */
	private static boolean isDefaultConfigurationClassCandidate(Class<?> clazz) {
		return (clazz != null && isStaticNonPrivateAndNonFinal(clazz) &&
				AnnotatedElementUtils.hasAnnotation(clazz, Configuration.class));
	}

	private static boolean isStaticNonPrivateAndNonFinal(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		int modifiers = clazz.getModifiers();
		return (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers));
	}

}
