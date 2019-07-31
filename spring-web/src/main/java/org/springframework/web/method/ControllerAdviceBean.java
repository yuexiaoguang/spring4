package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * 封装有关{@linkplain ControllerAdvice @ControllerAdvice} Spring管理的bean的信息, 而不必要求它实例化.
 *
 * <p>{@link #findAnnotatedBeans(ApplicationContext)}方法可用于发现此类bean.
 * 但是, 可以从任何对象创建{@code ControllerAdviceBean}, 包括没有{@code @ControllerAdvice}的对象.
 */
public class ControllerAdviceBean implements Ordered {

	private final Object bean;

	private final BeanFactory beanFactory;

	private final int order;

	private final Set<String> basePackages;

	private final List<Class<?>> assignableTypes;

	private final List<Class<? extends Annotation>> annotations;


	/**
	 * @param bean bean实例
	 */
	public ControllerAdviceBean(Object bean) {
		this(bean, null);
	}

	/**
	 * @param beanName bean的名称
	 * @param beanFactory 用于解析bean的BeanFactory
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory) {
		this((Object) beanName, beanFactory);
	}

	private ControllerAdviceBean(Object bean, BeanFactory beanFactory) {
		this.bean = bean;
		this.beanFactory = beanFactory;
		Class<?> beanType;

		if (bean instanceof String) {
			String beanName = (String) bean;
			Assert.hasText(beanName, "Bean name must not be null");
			Assert.notNull(beanFactory, "BeanFactory must not be null");
			if (!beanFactory.containsBean(beanName)) {
				throw new IllegalArgumentException("BeanFactory [" + beanFactory +
						"] does not contain specified controller advice bean '" + beanName + "'");
			}
			beanType = this.beanFactory.getType(beanName);
			this.order = initOrderFromBeanType(beanType);
		}
		else {
			Assert.notNull(bean, "Bean must not be null");
			beanType = bean.getClass();
			this.order = initOrderFromBean(bean);
		}

		ControllerAdvice annotation =
				AnnotatedElementUtils.findMergedAnnotation(beanType, ControllerAdvice.class);

		if (annotation != null) {
			this.basePackages = initBasePackages(annotation);
			this.assignableTypes = Arrays.asList(annotation.assignableTypes());
			this.annotations = Arrays.asList(annotation.annotations());
		}
		else {
			this.basePackages = Collections.emptySet();
			this.assignableTypes = Collections.emptyList();
			this.annotations = Collections.emptyList();
		}
	}


	/**
	 * 返回从{@link ControllerAdvice}注解中提取的顺序值, 或{@link Ordered#LOWEST_PRECEDENCE}.
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 返回包含的bean的类型.
	 * <p>如果bean类型是CGLIB生成的类, 则返回原始用户定义的类.
	 */
	public Class<?> getBeanType() {
		Class<?> clazz = (this.bean instanceof String ?
				this.beanFactory.getType((String) this.bean) : this.bean.getClass());
		return ClassUtils.getUserClass(clazz);
	}

	/**
	 * 返回bean实例, 如果需要, 通过BeanFactory解析bean名称.
	 */
	public Object resolveBean() {
		return (this.bean instanceof String ? this.beanFactory.getBean((String) this.bean) : this.bean);
	}

	/**
	 * 检查这个{@code @ControllerAdvice}实例是否应该辅助给定的bean类型.
	 * 
	 * @param beanType 要检查的bean的类型
	 */
	public boolean isApplicableToBeanType(Class<?> beanType) {
		if (!hasSelectors()) {
			return true;
		}
		else if (beanType != null) {
			for (String basePackage : this.basePackages) {
				if (beanType.getName().startsWith(basePackage)) {
					return true;
				}
			}
			for (Class<?> clazz : this.assignableTypes) {
				if (ClassUtils.isAssignable(clazz, beanType)) {
					return true;
				}
			}
			for (Class<? extends Annotation> annotationClass : this.annotations) {
				if (AnnotationUtils.findAnnotation(beanType, annotationClass) != null) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasSelectors() {
		return (!this.basePackages.isEmpty() || !this.assignableTypes.isEmpty() || !this.annotations.isEmpty());
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControllerAdviceBean)) {
			return false;
		}
		ControllerAdviceBean otherAdvice = (ControllerAdviceBean) other;
		return (this.bean.equals(otherAdvice.bean) && this.beanFactory == otherAdvice.beanFactory);
	}

	@Override
	public int hashCode() {
		return this.bean.hashCode();
	}

	@Override
	public String toString() {
		return this.bean.toString();
	}


	/**
	 * 查找给定的ApplicationContext中使用{@linkplain ControllerAdvice @ControllerAdvice}注解的bean的名称,
	 * 并将它们包装为{@code ControllerAdviceBean}实例.
	 */
	public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext applicationContext) {
		List<ControllerAdviceBean> beans = new ArrayList<ControllerAdviceBean>();
		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class)) {
			if (applicationContext.findAnnotationOnBean(name, ControllerAdvice.class) != null) {
				beans.add(new ControllerAdviceBean(name, applicationContext));
			}
		}
		return beans;
	}

	private static int initOrderFromBean(Object bean) {
		return (bean instanceof Ordered ? ((Ordered) bean).getOrder() : initOrderFromBeanType(bean.getClass()));
	}

	private static int initOrderFromBeanType(Class<?> beanType) {
		return OrderUtils.getOrder(beanType, Ordered.LOWEST_PRECEDENCE);
	}

	private static Set<String> initBasePackages(ControllerAdvice annotation) {
		Set<String> basePackages = new LinkedHashSet<String>();
		for (String basePackage : annotation.basePackages()) {
			if (StringUtils.hasText(basePackage)) {
				basePackages.add(adaptBasePackage(basePackage));
			}
		}
		for (Class<?> markerClass : annotation.basePackageClasses()) {
			basePackages.add(adaptBasePackage(ClassUtils.getPackageName(markerClass)));
		}
		return basePackages;
	}

	private static String adaptBasePackage(String basePackage) {
		return (basePackage.endsWith(".") ? basePackage : basePackage + ".");
	}

}
