package org.springframework.messaging.handler;

import org.springframework.core.Ordered;

/**
 * 表示一个Spring管理的bean, 它具有交叉功能, 可以应用于一个或多个具有基于注解的消息处理方法的Spring bean.
 *
 * <p>具有注解处理器方法的组件构造型,
 * 例如{@link org.springframework.stereotype.Controller @Controller},
 * 通常需要跨所有或部分此类带注解的组件的交叉功能.
 * 这方面的一个主要例子是需要"全局"带注解的异常处理器方法, 但这个概念更普遍适用.
 */
public interface MessagingAdviceBean extends Ordered {

	/**
	 * 返回包含的增强 bean的类型.
	 * <p>如果bean类型是CGLIB生成的类, 则返回原始用户定义的类.
	 */
	Class<?> getBeanType();

	/**
	 * 返回增强 bean实例, 如果需要, 通过BeanFactory解析通过名称指定的bean.
	 */
	Object resolveBean();

	/**
	 * 此{@link MessagingAdviceBean}是否适用于给定的bean类型.
	 * 
	 * @param beanType 要检查的bean的类型
	 */
	boolean isApplicableToBeanType(Class<?> beanType);

}
