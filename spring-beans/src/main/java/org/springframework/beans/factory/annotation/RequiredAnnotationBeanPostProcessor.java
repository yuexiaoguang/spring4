package org.springframework.beans.factory.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现, 强制必须的JavaBean属性已经配置.
 * 通过Java 5注解检测必需的bean属性: 默认情况下, Spring的 {@link Required}注解.
 *
 * <p>存在此BeanPostProcessor的动机是允许开发人员使用任意JDK 1.5注解来注解其自己的类的setter属性,
 * 以指示容器必须检查依赖项注入值的配置.
 * 这巧妙地将这种检查的责任推到容器上, 并且不需要开发人员编写一个只检查所有必需的属性是否已实际设置的方法.
 *
 * <p>请注意, 可能仍需要实现'init'方法, 因为这个类的所有功能都强制要求'required'属性实际配置了一个值.
 * 它不会检查任何其他内容... 特别是, 它不会检查配置的值是不是{@code null}.
 *
 * <p>Note: 默认的RequiredAnnotationBeanPostProcessor 将由"context:annotation-config"和"context:component-scan" XML标记注册.
 * 如果要指定自定义RequiredAnnotationBeanPostProcessor bean定义, 请删除或关闭默认注解配置.
 */
public class RequiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	/**
	 * Bean定义属性, 指示在执行此后处理器的必需属性检查时, 是否跳过给定的bean.
	 */
	public static final String SKIP_REQUIRED_CHECK_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(RequiredAnnotationBeanPostProcessor.class, "skipRequiredCheck");


	private Class<? extends Annotation> requiredAnnotationType = Required.class;

	private int order = Ordered.LOWEST_PRECEDENCE - 1;

	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * 缓存已验证的bean名称, 跳过对同一个bean的重新验证
	 */
	private final Set<String> validatedBeanNames =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(64));


	/**
	 * 设置'required' 注解类型, 用于bean属性setter方法.
	 * <p>默认的必需注解类型是Spring提供的{@link Required}注解.
	 * <p>此setter属性存在, 以便开发人员可以提供自己的（非Spring特定的）注解类型, 以指示属性值是必需的.
	 */
	public void setRequiredAnnotationType(Class<? extends Annotation> requiredAnnotationType) {
		Assert.notNull(requiredAnnotationType, "'requiredAnnotationType' must not be null");
		this.requiredAnnotationType = requiredAnnotationType;
	}

	/**
	 * 返回'required'注解类型.
	 */
	protected Class<? extends Annotation> getRequiredAnnotationType() {
		return this.requiredAnnotationType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		if (!this.validatedBeanNames.contains(beanName)) {
			if (!shouldSkip(this.beanFactory, beanName)) {
				List<String> invalidProperties = new ArrayList<String>();
				for (PropertyDescriptor pd : pds) {
					if (isRequiredProperty(pd) && !pvs.contains(pd.getName())) {
						invalidProperties.add(pd.getName());
					}
				}
				if (!invalidProperties.isEmpty()) {
					throw new BeanInitializationException(buildExceptionMessage(invalidProperties, beanName));
				}
			}
			this.validatedBeanNames.add(beanName);
		}
		return pvs;
	}

	/**
	 * 检查给定的bean定义是否不受, 此后处理器执行的基于注解的必需属性检查, 的影响.
	 * <p>默认实现检查bean定义中是否存在{@link #SKIP_REQUIRED_CHECK_ATTRIBUTE}属性.
	 * 它还建议在使用“factory-bean”引用集的bean定义的情况下跳过, 假设基于实例的工厂预先填充bean.
	 * 
	 * @param beanFactory 要检查的BeanFactory
	 * @param beanName 要检查的bean的名称
	 * 
	 * @return {@code true} 跳过 bean; {@code false} 处理它
	 */
	protected boolean shouldSkip(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanFactory == null || !beanFactory.containsBeanDefinition(beanName)) {
			return false;
		}
		BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
		if (beanDefinition.getFactoryBeanName() != null) {
			return true;
		}
		Object value = beanDefinition.getAttribute(SKIP_REQUIRED_CHECK_ATTRIBUTE);
		return (value != null && (Boolean.TRUE.equals(value) || Boolean.valueOf(value.toString())));
	}

	/**
	 * 提供的属性是否需要具有值 (即, 依赖注入)?
	 * <p>此实现在提供的{@link PropertyDescriptor属性}中查找是否存在{@link #setRequiredAnnotationType "required"注解}.
	 * 
	 * @param propertyDescriptor 目标PropertyDescriptor (never {@code null})
	 *
	 * @return {@code true} 如果提供的属性已被标记为必需;
	 * {@code false}如果不是, 或者如果提供的属性没有setter方法
	 */
	protected boolean isRequiredProperty(PropertyDescriptor propertyDescriptor) {
		Method setter = propertyDescriptor.getWriteMethod();
		return (setter != null && AnnotationUtils.getAnnotation(setter, getRequiredAnnotationType()) != null);
	}

	/**
	 * 为给定的无效属性列表构建异常消息.
	 * 
	 * @param invalidProperties 无效属性的名称列表
	 * @param beanName bean名称
	 * 
	 * @return 异常消息
	 */
	private String buildExceptionMessage(List<String> invalidProperties, String beanName) {
		int size = invalidProperties.size();
		StringBuilder sb = new StringBuilder();
		sb.append(size == 1 ? "Property" : "Properties");
		for (int i = 0; i < size; i++) {
			String propertyName = invalidProperties.get(i);
			if (i > 0) {
				if (i == (size - 1)) {
					sb.append(" and");
				}
				else {
					sb.append(",");
				}
			}
			sb.append(" '").append(propertyName).append("'");
		}
		sb.append(size == 1 ? " is" : " are");
		sb.append(" required for bean '").append(beanName).append("'");
		return sb.toString();
	}
}
