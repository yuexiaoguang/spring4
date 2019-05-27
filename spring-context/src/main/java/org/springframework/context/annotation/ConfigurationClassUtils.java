package org.springframework.context.annotation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;

/**
 * 用于识别 @{@link Configuration}类的实用程序.
 */
abstract class ConfigurationClassUtils {

	private static final String CONFIGURATION_CLASS_FULL = "full";

	private static final String CONFIGURATION_CLASS_LITE = "lite";

	private static final String CONFIGURATION_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	private static final String ORDER_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "order");


	private static final Log logger = LogFactory.getLog(ConfigurationClassUtils.class);

	private static final Set<String> candidateIndicators = new HashSet<String>(8);

	static {
		candidateIndicators.add(Component.class.getName());
		candidateIndicators.add(ComponentScan.class.getName());
		candidateIndicators.add(Import.class.getName());
		candidateIndicators.add(ImportResource.class.getName());
	}


	/**
	 * 检查给定的bean定义是否是配置类的候选者
	 * (或者在配置/组件类中声明的嵌套组件类, 也可以自动注册), 并相应地标记.
	 * 
	 * @param beanDef 要检查的bean定义
	 * @param metadataReaderFactory 调用者使用的当前工厂
	 * 
	 * @return 候选者是否有资格成为 (任何类型) 配置类
	 */
	public static boolean checkConfigurationClassCandidate(BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
		String className = beanDef.getBeanClassName();
		if (className == null || beanDef.getFactoryMethodName() != null) {
			return false;
		}

		AnnotationMetadata metadata;
		if (beanDef instanceof AnnotatedBeanDefinition &&
				className.equals(((AnnotatedBeanDefinition) beanDef).getMetadata().getClassName())) {
			// 可以重用给定BeanDefinition中的预解析元数据...
			metadata = ((AnnotatedBeanDefinition) beanDef).getMetadata();
		}
		else if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			// 检查已加载的类...
			// 因为甚至无法加载此类的类文件.
			Class<?> beanClass = ((AbstractBeanDefinition) beanDef).getBeanClass();
			metadata = new StandardAnnotationMetadata(beanClass, true);
		}
		else {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
				metadata = metadataReader.getAnnotationMetadata();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find class file for introspecting configuration annotations: " + className, ex);
				}
				return false;
			}
		}

		if (isFullConfigurationCandidate(metadata)) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
		}
		else if (isLiteConfigurationCandidate(metadata)) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
		}
		else {
			return false;
		}

		// 它是一个完整的或精简配置候选者... 确定顺序值.
		Map<String, Object> orderAttributes = metadata.getAnnotationAttributes(Order.class.getName());
		if (orderAttributes != null) {
			beanDef.setAttribute(ORDER_ATTRIBUTE, orderAttributes.get(AnnotationUtils.VALUE));
		}

		return true;
	}

	/**
	 * 检查配置类候选者的给定元数据
	 * (或在配置/组件类中声明的嵌套组件类).
	 * 
	 * @param metadata 带注解的类的元数据
	 * 
	 * @return {@code true}如果要将给定的类注册为反射检测的bean定义; 否则{@code false}
	 */
	public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
		return (isFullConfigurationCandidate(metadata) || isLiteConfigurationCandidate(metadata));
	}

	/**
	 * 检查给定的元数据以获取完整的配置类候选 (i.e. 一个用{@code @Configuration}注解的类).
	 * 
	 * @param metadata 带注解的类的元数据
	 * 
	 * @return {@code true} 如果给定的类要作为完整的配置类处理, 包括跨方法调用拦截
	 */
	public static boolean isFullConfigurationCandidate(AnnotationMetadata metadata) {
		return metadata.isAnnotated(Configuration.class.getName());
	}

	/**
	 * 检查精简配置类候选的给定元数据
	 * (e.g. 一个带{@code @Component}注解或只有{@code @Import}声明或{@code @Bean方法}的类).
	 * 
	 * @param metadata 带注解的类的元数据
	 * 
	 * @return {@code true} 如果给定的类要作为精简配置类处理, 只需注册它并扫描它以获取{@code @Bean}方法
	 */
	public static boolean isLiteConfigurationCandidate(AnnotationMetadata metadata) {
		// 不要考虑接口或注解...
		if (metadata.isInterface()) {
			return false;
		}

		// 发现任何典型的注解?
		for (String indicator : candidateIndicators) {
			if (metadata.isAnnotated(indicator)) {
				return true;
			}
		}

		// 最后, 让我们来看看@Bean方法...
		try {
			return metadata.hasAnnotatedMethods(Bean.class.getName());
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
			}
			return false;
		}
	}

	/**
	 * 通过检查{@link #checkConfigurationClassCandidate}的元数据标记, 确定给定的b​​ean定义是否表示完整的{@code @Configuration}类.
	 */
	public static boolean isFullConfigurationClass(BeanDefinition beanDef) {
		return CONFIGURATION_CLASS_FULL.equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE));
	}

	/**
	 * 通过检查 {@link #checkConfigurationClassCandidate}的元数据标记, 确定给定的b​​ean定义是否表示精简{@code @Configuration}类.
	 */
	public static boolean isLiteConfigurationClass(BeanDefinition beanDef) {
		return CONFIGURATION_CLASS_LITE.equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE));
	}

	/**
	 * 确定给定配置类bean定义的顺序, 由{@link #checkConfigurationClassCandidate}设置.
	 * 
	 * @param beanDef 要检查的bean定义
	 * 
	 * @return 配置类上的{@link @Order}注解值; 如果没有声明, 则为{@link Ordered#LOWEST_PRECEDENCE}
	 */
	public static int getOrder(BeanDefinition beanDef) {
		Integer order = (Integer) beanDef.getAttribute(ORDER_ATTRIBUTE);
		return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
	}

}
