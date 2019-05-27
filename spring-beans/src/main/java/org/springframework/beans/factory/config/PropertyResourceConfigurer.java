package org.springframework.beans.factory.config;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.util.ObjectUtils;

/**
 * 允许从属性资源配置各个bean属性值,
 * i.e. 属性文件. 对于以系统管理员为目标的自定义配置文件非常有用, 这些文件覆盖在应用程序上下文中配置的bean属性.
 *
 * <p>分配中提供了两个具体实现:
 * <ul>
 * <li>{@link PropertyOverrideConfigurer}用于 "beanName.property=value"格式重写
 * (将值从属性文件推送到bean定义中)
 * <li>{@link PropertyPlaceholderConfigurer} 用于替换 "${...}"占位符
 * (将值从属性文件中提取到bean定义中)
 * </ul>
 *
 * <p>通过覆盖{@link #convertPropertyValue}方法, 可以在读取属性值后对其进行转换.
 * 例如, 可以在处理加密值之前, 相应地检测和解密加密值.
 */
public abstract class PropertyResourceConfigurer extends PropertiesLoaderSupport
		implements BeanFactoryPostProcessor, PriorityOrdered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 设置此对象的顺序值以进行排序.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 给定bean工厂的{@linkplain #mergeProperties Merge},
	 * {@linkplain #convertProperties convert}和 {@linkplain #processProperties process}属性.
	 * 
	 * @throws BeanInitializationException 如果无法加载任何属性
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			Properties mergedProps = mergeProperties();

			// Convert the merged properties, if necessary.
			convertProperties(mergedProps);

			// Let the subclass process the properties.
			processProperties(beanFactory, mergedProps);
		}
		catch (IOException ex) {
			throw new BeanInitializationException("Could not load properties", ex);
		}
	}

	/**
	 * 转换给定的合并属性, 必要时转换属性值. 然后将处理结果.
	 * <p>默认实现将为每个属性值调用 {@link #convertPropertyValue}, 用转换后的值替换原始值.
	 * 
	 * @param props the Properties to convert
	 */
	protected void convertProperties(Properties props) {
		Enumeration<?> propertyNames = props.propertyNames();
		while (propertyNames.hasMoreElements()) {
			String propertyName = (String) propertyNames.nextElement();
			String propertyValue = props.getProperty(propertyName);
			String convertedValue = convertProperty(propertyName, propertyValue);
			if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
				props.setProperty(propertyName, convertedValue);
			}
		}
	}

	/**
	 * 将给定属性从源转换为应该应用的值.
	 * <p>默认实现调用 {@link #convertPropertyValue(String)}.
	 * 
	 * @param propertyName 为其定义值的属性的名称
	 * @param propertyValue 属性源中的原始值
	 * 
	 * @return 转换后的值, 用于处理
	 */
	protected String convertProperty(String propertyName, String propertyValue) {
		return convertPropertyValue(propertyValue);
	}

	/**
	 * 将给定属性值从属性源转换为应该应用的值.
	 * <p>默认实现只返回原始值.
	 * 可以在子类中重写, 例如检测加密值并相应地解密它们.
	 * 
	 * @param originalValue 属性源中的原始值 (属性文件或本地 "properties")
	 * 
	 * @return 转换后的值, 用于处理
	 */
	protected String convertPropertyValue(String originalValue) {
		return originalValue;
	}


	/**
	 * 将给定的Properties应用于给定的BeanFactory.
	 * 
	 * @param beanFactory 应用程序上下文使用的BeanFactory
	 * @param props 要应用的属性
	 * 
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	protected abstract void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException;

}
