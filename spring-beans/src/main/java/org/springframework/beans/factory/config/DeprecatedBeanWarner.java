package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Bean工厂后处理器, 记录{@link Deprecated @Deprecated} bean的警告.
 */
public class DeprecatedBeanWarner implements BeanFactoryPostProcessor {

	/**
	 * Logger available to subclasses.
	 */
	protected transient Log logger = LogFactory.getLog(getClass());

	/**
	 * 该名称将通过Commons Logging传递给底层记录器实现, 根据记录器的配置被解释为日志类别.
	 * <p>这可以指定为不记录此warner类的类别, 而是记录到特定的命名类别.
	 */
	public void setLoggerName(String loggerName) {
		this.logger = LogFactory.getLog(loggerName);
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (isLogEnabled()) {
			String[] beanNames = beanFactory.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				String nameToLookup = beanName;
				if (beanFactory.isFactoryBean(beanName)) {
					nameToLookup = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
				}
				Class<?> beanType = ClassUtils.getUserClass(beanFactory.getType(nameToLookup));
				if (beanType != null && beanType.isAnnotationPresent(Deprecated.class)) {
					BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
					logDeprecatedBean(beanName, beanType, beanDefinition);
				}
			}
		}
	}

	/**
	 * 记录使用{@link Deprecated @Deprecated}注解的bean的警告.
	 * 
	 * @param beanName 已弃用的bean的名称
	 * @param beanType 用户指定的弃用bean类型
	 * @param beanDefinition 已弃用的bean的定义
	 */
	protected void logDeprecatedBean(String beanName, Class<?> beanType, BeanDefinition beanDefinition) {
		StringBuilder builder = new StringBuilder();
		builder.append(beanType);
		builder.append(" ['");
		builder.append(beanName);
		builder.append('\'');
		String resourceDescription = beanDefinition.getResourceDescription();
		if (StringUtils.hasLength(resourceDescription)) {
			builder.append(" in ");
			builder.append(resourceDescription);
		}
		builder.append("] has been deprecated");
		writeToLog(builder.toString());
	}

	/**
	 * 实际上写入底层日志.
	 * <p>默认实现将消息记录在 "warn"级别.
	 * 
	 * @param message 要写入的消息
	 */
	protected void writeToLog(String message) {
		logger.warn(message);
	}

	/**
	 * 是否启用{@link #logger}字段.
	 * <p>启用 "warn" 级别时, 默认值为{@code true}.
	 * 子类可以覆盖它以更改日志记录发生的级别.
	 */
	protected boolean isLogEnabled() {
		return logger.isWarnEnabled();
	}

}
