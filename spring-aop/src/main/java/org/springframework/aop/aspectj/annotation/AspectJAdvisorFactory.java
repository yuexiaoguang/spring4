package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.AopConfigException;

/**
 * 可以使用AspectJ注解语法注解的类创建Spring AOP Advisor的工厂接口.
 */
public interface AspectJAdvisorFactory {

	/**
	 * 确定给定的类是否是切面, 正如AspectJ的{@link org.aspectj.lang.reflect.AjTypeSystem}报告的那样.
	 * <p>如果假定切面无效，将简单地返回{@code false} (例如具体切面类的扩展).
	 * 对于Spring AOP无法处理的某些切面, 将返回true, 例如具有不受支持的实例化模型的那些.
	 * 使用{@link #validate}方法处理这些情况.
	 * 
	 * @param clazz 假设的注解风格的AspectJ类
	 * 
	 * @return 该类是否被AspectJ识别为切面类
	 */
	boolean isAspect(Class<?> clazz);

	/**
	 * 给定的类是否是有效的AspectJ切面类?
	 * 
	 * @param aspectClass 要验证的假设的AspectJ注解风格的类
	 * 
	 * @throws AopConfigException 如果该类是无效的切面(永远不合法)
	 * @throws NotAnAtAspectException 如果该类根本不是一个切面 (可能合法也可能不合法, 取决于上下文)
	 */
	void validate(Class<?> aspectClass) throws AopConfigException;

	/**
	 * 在指定的切面实例上为所有带注解的At-AspectJ方法构建Spring AOP Advisor.
	 * 
	 * @param aspectInstanceFactory 切面实例工厂  (不是切面实例本身，以避免急切的实例化)
	 * 
	 * @return 这个类的切面列表
	 */
	List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);

	/**
	 * 为给定的AspectJ增强方法构建Spring AOP Advisor.
	 * 
	 * @param candidateAdviceMethod 候选的增强方法
	 * @param aspectInstanceFactory 切面实例工厂
	 * @param declarationOrder 切面的声明顺序
	 * @param aspectName 切面的名称
	 * 
	 * @return {@code null}如果该方法不是AspectJ增强方法; 或者如果它是一个将被其他增强使用的切点, 但不会自己创建一个Spring增强
	 */
	Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrder, String aspectName);

	/**
	 * 为给定的AspectJ增强方法构建Spring AOP增强.
	 * 
	 * @param candidateAdviceMethod 候选的增强方法
	 * @param expressionPointcut AspectJ表达式切点
	 * @param aspectInstanceFactory 切面实例工厂
	 * @param declarationOrder 切面的声明顺序
	 * @param aspectName 切面的名称
	 * 
	 * @return {@code null}如果该方法不是AspectJ增强方法; 或者如果它是一个将被其他增强使用的切点, 但不会自己创建一个Spring增强
	 */
	Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName);

}
