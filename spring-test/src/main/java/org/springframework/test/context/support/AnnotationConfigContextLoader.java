package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

/**
 * {@link AbstractGenericContextLoader}的具体实现, 用于从带注解的类加载bean定义.
 *
 * <p>有关<em>带注解的类</em>的定义, 请参阅
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}的Javadoc.
 *
 * <p>Note: {@code AnnotationConfigContextLoader}支持<em>带注解的类</em>, 而不是传统
 * {@link org.springframework.test.context.ContextLoader ContextLoader} API定义的基于字符串的资源位置.
 * 因此, 虽然{@code AnnotationConfigContextLoader}扩展了
 * {@code AbstractGenericContextLoader}, {@code AnnotationConfigContextLoader}
 * 但是<em>不</em>支持{@code AbstractContextLoader}或{@code AbstractGenericContextLoader}定义的任何基于String的方法.
 * 因此, {@code AnnotationConfigContextLoader}应该主要被认为是
 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
 * 而不是{@link org.springframework.test.context.ContextLoader ContextLoader}.
 */
public class AnnotationConfigContextLoader extends AbstractGenericContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);


	// SmartContextLoader

	/**
	 * 在提供的{@link ContextConfigurationAttributes}中处理<em>带注解的类</em>.
	 * <p>如果<em>带注解的类</em>是{@code null}或为空, 且{@link #isGenerateDefaultLocations()}返回{@code true},
	 * 则此{@code SmartContextLoader}将尝试 {@link #detectDefaultConfigurationClasses 检测默认的配置类}.
	 * 如果检测到默认值, 它们将在提供的配置属性中{@link ContextConfigurationAttributes#setClasses(Class[]) set}.
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


	// AnnotationConfigContextLoader

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
	 * {@code AnnotationConfigContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * {@code AnnotationConfigContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * {@code AnnotationConfigContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the getResourceSuffix() method");
	}


	// AbstractGenericContextLoader

	/**
	 * 确保提供的{@link MergedContextConfiguration}不包含{@link MergedContextConfiguration#getLocations() locations}.
	 */
	@Override
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		if (mergedConfig.hasLocations()) {
			String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'locations' " +
							"(or 'value') attribute %s, but %s does not support resource locations.",
					mergedConfig.getTestClass().getName(), ObjectUtils.nullSafeToString(mergedConfig.getLocations()),
					getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

	/**
	 * 从提供的{@link MergedContextConfiguration}中的类注册所提供的{@link GenericApplicationContext context}中的类.
	 * <p>每个类必须表示<em>带注解的类</em>.
	 * {@link AnnotatedBeanDefinitionReader}用于注册适当的bean定义.
	 * <p>请注意, 此方法不会调用{@link #createBeanDefinitionReader},
	 * 因为{@code AnnotatedBeanDefinitionReader}不是{@link BeanDefinitionReader}的实例.
	 * 
	 * @param context 应该注册带注解的类的上下文
	 * @param mergedConfig 应从中检索类的合并配置
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		Class<?>[] annotatedClasses = mergedConfig.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering annotated classes: " + ObjectUtils.nullSafeToString(annotatedClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(annotatedClasses);
	}

	/**
	 * {@code AnnotationConfigContextLoader}应该用作
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * 而不是传统的{@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the createBeanDefinitionReader(GenericApplicationContext) method");
	}

}
