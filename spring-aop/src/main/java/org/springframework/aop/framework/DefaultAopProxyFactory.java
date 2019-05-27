package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;

/**
 * 默认的{@link AopProxyFactory}实现, 创建CGLIB代理或JDK动态代理.
 *
 * <p>如果对于给定的{@link AdvisedSupport}实例，满足以下条件，则创建CGLIB代理:
 * <ul>
 * <li>设置了{@code optimize}标志
 * <li>设置了{@code proxyTargetClass}标志
 * <li>没有指定代理接口
 * </ul>
 *
 * <p>一般来说, 指定{@code proxyTargetClass}以强制执行CGLIB代理, 或指定一个或多个接口以使用JDK动态代理.
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * 确定提供的{@link AdvisedSupport}是否仅指定了{@link org.springframework.aop.SpringProxy}接口
	 * (或根本没有指定代理接口).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
