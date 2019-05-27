package org.springframework.aop.framework.autoproxy.target;

import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * TargetSourceCreator为每个定义为“lazy-init”的bean强制执行LazyInitTargetSource.
 * 这将导致为每个bean创建一个代理, 允许获取对这样的bean的引用, 而不实际初始化目标bean实例.
 *
 * <p>要为自动代理创建者注册为自定义TargetSourceCreator, 与特定bean的自定义拦截器结合使用或仅用于创建lazy-init代理.
 * 例如, 作为XML应用程序上下文定义中的自动检测基础结构bean:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator"&gt;
 *   &lt;property name="customTargetSourceCreators"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myLazyInitBean" class="mypackage.MyBeanClass" lazy-init="true"&gt;
 *   ...
 * &lt;/bean&gt;</pre>
 */
public class LazyInitTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	@Override
	protected boolean isPrototypeBased() {
		return false;
	}

	@Override
	protected AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {

		if (getBeanFactory() instanceof ConfigurableListableBeanFactory) {
			BeanDefinition definition =
					((ConfigurableListableBeanFactory) getBeanFactory()).getBeanDefinition(beanName);
			if (definition.isLazyInit()) {
				return new LazyInitTargetSource();
			}
		}
		return null;
	}

}
