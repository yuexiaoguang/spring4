package org.springframework.aop.framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor}实现的基类, 应用一个Spring AOP {@link Advisor}到指定的bean.
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport implements BeanPostProcessor {

	protected Advisor advisor;

	protected boolean beforeExistingAdvisors = false;

	private final Map<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<Class<?>, Boolean>(256);


	/**
	 * 设置此后处理程序的切面是否应在遇到预增强的对象时, 在现有切面之前应用.
	 * <p>默认是 "false", 在现有切面之后应用, i.e. 尽可能接近目标方法.
	 * 切换为 "true", 为了让这个后处理器的切面包装现有切面.
	 * <p>Note: 检查具体的后处理器的javadoc是否可能默认更改此标志, 取决于其切面的性质.
	 */
	public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
		this.beforeExistingAdvisors = beforeExistingAdvisors;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AopInfrastructureBean) {
			// 忽略AOP基础结构，例如作用域代理.
			return bean;
		}

		if (bean instanceof Advised) {
			Advised advised = (Advised) bean;
			if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
				// 添加本地 Advisor 到现有代理的 Advisor 链...
				if (this.beforeExistingAdvisors) {
					advised.addAdvisor(0, this.advisor);
				}
				else {
					advised.addAdvisor(this.advisor);
				}
				return bean;
			}
		}

		if (isEligible(bean, beanName)) {
			ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
			if (!proxyFactory.isProxyTargetClass()) {
				evaluateProxyInterfaces(bean.getClass(), proxyFactory);
			}
			proxyFactory.addAdvisor(this.advisor);
			customizeProxyFactory(proxyFactory);
			return proxyFactory.getProxy(getProxyClassLoader());
		}

		// 不需要异步代理.
		return bean;
	}

	/**
	 * 检查给定的bean是否有资格使用此后处理器的{@link Advisor}进行增强.
	 * <p>委派{@link #isEligible(Class)} 进行目标类检查.
	 * 可以被重写, e.g. 按名称专门排除某些bean.
	 * <p>Note: 仅调用常规bean实例，但不调用实现{@link Advised}的现有代理实例，并允许将本地{@link Advisor}添加到现有代理的{@link Advisor}链.
	 * 对于后者, 将直接调用{@link #isEligible(Class)}, 使用现有代理后面的实际目标类 (由{@link AopUtils#getTargetClass(Object)}确定).
	 * 
	 * @param bean bean实例
	 * @param beanName bean的名称
	 */
	protected boolean isEligible(Object bean, String beanName) {
		return isEligible(bean.getClass());
	}

	/**
	 * 检查给定的类是否有资格使用此后处理器{@link Advisor}进行增强.
	 * <p>实现每个bean目标类的{@code canApply}结果的缓存.
	 * 
	 * @param targetClass 要检查的类
	 */
	protected boolean isEligible(Class<?> targetClass) {
		Boolean eligible = this.eligibleBeans.get(targetClass);
		if (eligible != null) {
			return eligible;
		}
		eligible = AopUtils.canApply(this.advisor, targetClass);
		this.eligibleBeans.put(targetClass, eligible);
		return eligible;
	}

	/**
	 * 为给定的bean准备一个{@link ProxyFactory}.
	 * <p>子类可以定制目标实例的处理，特别是目标类的公开.
	 * 之后将应用非目标类代理的接口和配置的切面的默认内省;
	 * {@link #customizeProxyFactory}允许在代理创建之前对这些部分进行后期自定义.
	 * 
	 * @param bean 用于创建代理的bean实例
	 * @param beanName 相应的bean名称
	 * 
	 * @return ProxyFactory, 使用此处理器的{@link ProxyConfig}设置和指定的bean初始化
	 * 
	 * @since 4.2.3
	 */
	protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);
		proxyFactory.setTarget(bean);
		return proxyFactory;
	}

	/**
	 * 子类可选择的实现它: 例如, 修改公开的接口.
	 * <p>默认实现为空.
	 * 
	 * @param proxyFactory 已配置了目标, 切面和接口的ProxyFactory, 将在此方法返回后立即用于创建代理
	 * 
	 * @since 4.2.3
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}

}
