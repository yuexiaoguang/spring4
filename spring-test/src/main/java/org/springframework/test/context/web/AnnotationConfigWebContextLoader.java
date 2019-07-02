package org.springframework.test.context.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * {@link AbstractGenericWebContextLoader}的具体实现, 它从带注解的类加载bean定义.
 *
 * <p>有关<em>带注解的类</em>的定义, 请参阅
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}的Javadoc.
 *
 * <p>Note: {@code AnnotationConfigWebContextLoader}支持<em>带注解的类</em>,
 * 而不是旧版{@link org.springframework.test.context.ContextLoader ContextLoader} API定义的基于字符串的资源位置.
 * 因此, 虽然{@code AnnotationConfigWebContextLoader}扩展了{@code AbstractGenericWebContextLoader},
 * 但{@code AnnotationConfigWebContextLoader} <em>不</em>支持
 * {@link org.springframework.test.context.support.AbstractContextLoader AbstractContextLoader}
 * 或{@code AbstractGenericWebContextLoader}定义的任何基于String的方法.
 * 因此, {@code AnnotationConfigWebContextLoader}应该主要被视为
 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
 * 而非{@link org.springframework.test.context.ContextLoader ContextLoader}.
 */
public class AnnotationConfigWebContextLoader extends AbstractGenericWebContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigWebContextLoader.class);


	// SmartContextLoader

	/**
	 * 在提供的{@link ContextConfigurationAttributes}中处理<em>带注解的类</em>.
	 * <p>如果<em>带注解的类</em>是{@code null}或为空, 且{@link #isGenerateDefaultLocations()}返回{@code true},
	 * 则此{@code SmartContextLoader}将尝试{@linkplain #detectDefaultConfigurationClasses 检测默认配置类}.
	 * 如果检测到默认值, 它们将在提供的配置属性中{@linkplain ContextConfigurationAttributes#setClasses(Class[]) 设置}.
	 * 否则, 将不会修改提供的配置属性中的属性.
	 * 
	 * @param configAttributes 要处理的上下文配置属性
	 */
	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		if (!configAttributes.hasClasses() && isGenerateDefaultLocations()) {
			configAttributes.setClasses(detectDefaultConfigurationClasses(configAttributes.getDeclaringClass()));
		}
	}

	/**
	 * 检测提供的测试类的默认配置类.
	 * <p>默认实现委托给
	 * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses(Class)}.
	 * 
	 * @param declaringClass 声明{@code @ContextConfiguration}的测试类
	 * 
	 * @return 一组默认配置类, 可能为空但不能是{@code null}
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
	}


	// AbstractContextLoader

	/**
	 * {@code AnnotationConfigWebContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * {@code AnnotationConfigWebContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * {@code AnnotationConfigWebContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the getResourceSuffix() method");
	}


	// AbstractGenericWebContextLoader

	/**
	 * 将提供的{@link WebMergedContextConfiguration}中的类注册到提供的{@linkplain GenericWebApplicationContext context}.
	 * <p>每个类必须表示<em>带注解的类</em>.
	 * {@link AnnotatedBeanDefinitionReader}用于注册适当的bean定义.
	 * 
	 * @param context 应该注册带注解的类的上下文
	 * @param webMergedConfig 应从中检索类的合并配置
	 */
	@Override
	protected void loadBeanDefinitions(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

		Class<?>[] annotatedClasses = webMergedConfig.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering annotated classes: " + ObjectUtils.nullSafeToString(annotatedClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(annotatedClasses);
	}

	/**
	 * 确保提供的{@link WebMergedContextConfiguration}不包含{@link MergedContextConfiguration#getLocations() locations}.
	 */
	@Override
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
		if (webMergedConfig.hasLocations()) {
			String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'locations' " +
							"(or 'value') attribute %s, but %s does not support resource locations.",
					webMergedConfig.getTestClass().getName(),
					ObjectUtils.nullSafeToString(webMergedConfig.getLocations()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}
}
