package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;

import org.springframework.core.Ordered;

/**
 * {@link BeanInfoFactory}实现, 用于评估bean类是否具有“非标准”JavaBeans setter方法, 
 * 因此是Spring（包可见）{@code ExtendedBeanInfo}实现的反射的候选者.
 *
 * <p>以{@link Ordered#LOWEST_PRECEDENCE}排序, 以允许其他用户定义的{@link BeanInfoFactory}类型优先.
 */
public class ExtendedBeanInfoFactory implements BeanInfoFactory, Ordered {

	/**
	 * 返回给定bean类的{@link ExtendedBeanInfo}.
	 */
	@Override
	public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		return (supports(beanClass) ? new ExtendedBeanInfo(Introspector.getBeanInfo(beanClass)) : null);
	}

	/**
	 * 返回给定的bean类是声明, 还是继承bean属性或索引属性非void返回的setter方法.
	 */
	private boolean supports(Class<?> beanClass) {
		for (Method method : beanClass.getMethods()) {
			if (ExtendedBeanInfo.isCandidateWriteMethod(method)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
