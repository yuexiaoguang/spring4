package org.springframework.context.annotation;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * 由{@link Condition}使用的上下文信息.
 */
public interface ConditionContext {

	/**
	 * 如果条件匹配, 则返回将保存bean定义的{@link BeanDefinitionRegistry}; 如果注册表不可用, 则返回{@code null}.
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * 如果条件匹配, 则返回将保存bean定义的{@link ConfigurableListableBeanFactory}; 如果bean工厂不可用, 则返回{@code null}.
	 */
	ConfigurableListableBeanFactory getBeanFactory();

	/**
	 * 返回运行当前应用程序的{@link Environment}; 如果没有可用的环境, 则返回{@code null}.
	 */
	Environment getEnvironment();

	/**
	 * 返回当前正在使用的{@link ResourceLoader}; 如果无法获取资源加载器, 则返回{@code null}.
	 */
	ResourceLoader getResourceLoader();

	/**
	 * 返回应该用于加载其他类的{@link ClassLoader}; 如果应该使用默认的类加载器, 则返回{@code null}.
	 */
	ClassLoader getClassLoader();

}
