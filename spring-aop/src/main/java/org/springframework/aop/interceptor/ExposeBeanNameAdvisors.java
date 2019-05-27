package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.Advisor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.factory.NamedBean;

/**
 * 创建切面的便捷方法, 可以在使用Spring IoC容器创建的自动代理bean时使用, 将bean名称绑定到当前调用.
 * 可以使用AspectJ支持 {@code bean()} 切点指示符.
 *
 * <p>通常用于Spring自动代理, 在代理创建时已知bean名称.
 */
public abstract class ExposeBeanNameAdvisors {

	/**
	 * 绑定当前在ReflectiveMethodInvocation userAttributes Map中调用的bean的bean名称.
	 */
	private static final String BEAN_NAME_ATTRIBUTE = ExposeBeanNameAdvisors.class.getName() + ".BEAN_NAME";


	/**
	 * 查找当前调用的bean名称.
	 * 假设ExposeBeanNameAdvisor已包含在拦截器链中, 并且使用ExposeInvocationInterceptor公开调用.
	 * 
	 * @return bean名称 (never {@code null})
	 * @throws IllegalStateException 如果bean名称尚未公开
	 */
	public static String getBeanName() throws IllegalStateException {
		return getBeanName(ExposeInvocationInterceptor.currentInvocation());
	}

	/**
	 * 查找给定调用的bean名称. 假设ExposeBeanNameAdvisor已包含在拦截器链中.
	 * 
	 * @param mi 应该包含bean名称作为属性的MethodInvocation
	 * 
	 * @return bean名称 (never {@code null})
	 * @throws IllegalStateException 如果bean名称尚未公开
	 */
	public static String getBeanName(MethodInvocation mi) throws IllegalStateException {
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalArgumentException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
		String beanName = (String) pmi.getUserAttribute(BEAN_NAME_ATTRIBUTE);
		if (beanName == null) {
			throw new IllegalStateException("Cannot get bean name; not set on MethodInvocation: " + mi);
		}
		return beanName;
	}

	/**
	 * 创建一个将公开给定bean名称的新切面, 没有引入
	 * 
	 * @param beanName 要公开的bean名称
	 */
	public static Advisor createAdvisorWithoutIntroduction(String beanName) {
		return new DefaultPointcutAdvisor(new ExposeBeanNameInterceptor(beanName));
	}

	/**
	 * 创建一个将公开给定bean名称的新切面, 引入NamedBean接口以使bean名称可访问, 而不强制目标对象了解此Spring IoC概念.
	 * 
	 * @param beanName 要公开的bean名称
	 */
	public static Advisor createAdvisorIntroducingNamedBean(String beanName) {
		return new DefaultIntroductionAdvisor(new ExposeBeanNameIntroduction(beanName));
	}


	/**
	 * 将指定的bean名称公开为调用属性的拦截器.
	 */
	private static class ExposeBeanNameInterceptor implements MethodInterceptor {

		private final String beanName;

		public ExposeBeanNameInterceptor(String beanName) {
			this.beanName = beanName;
		}

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			if (!(mi instanceof ProxyMethodInvocation)) {
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}
			ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
			pmi.setUserAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);
			return mi.proceed();
		}
	}


	/**
	 * 将指定的bean名称公开为调用属性的引入.
	 */
	@SuppressWarnings("serial")
	private static class ExposeBeanNameIntroduction extends DelegatingIntroductionInterceptor implements NamedBean {

		private final String beanName;

		public ExposeBeanNameIntroduction(String beanName) {
			this.beanName = beanName;
		}

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			if (!(mi instanceof ProxyMethodInvocation)) {
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}
			ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
			pmi.setUserAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);
			return super.invoke(mi);
		}

		@Override
		public String getBeanName() {
			return this.beanName;
		}
	}

}
