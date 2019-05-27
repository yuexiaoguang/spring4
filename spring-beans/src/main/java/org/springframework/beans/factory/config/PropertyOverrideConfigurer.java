package org.springframework.beans.factory.config;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanInitializationException;

/**
 * 属性资源配置器, 它覆盖应用程序上下文定义中的bean属性值.
 * 它将属性文件中的值推送到bean定义中.
 *
 * <p>配置行应具有以下形式:
 *
 * <pre class="code">beanName.property=value</pre>
 *
 * 属性文件示例:
 *
 * <pre class="code">dataSource.driverClassName=com.mysql.jdbc.Driver
 * dataSource.url=jdbc:mysql:mydb</pre>
 *
 * 与PropertyPlaceholderConfigurer相比, 原始定义对于此类bean属性可以具有默认值或根本没有值.
 * 如果覆盖属性文件没有某个bean属性的条目, 则使用默认上下文定义.
 *
 * <p>请注意, 上下文定义不知道被覆盖; 因此, 在查看XML定义文件时, 这并不是很明显.
 * 此外，请注意指定的覆盖值始终是文字值; 它们不会被翻译成bean引用.
 * 当XML bean定义中的原始值指定bean引用时, 这也适用.
 *
 * <p>如果有多个PropertyOverrideConfigurers为同一个bean属性定义不同的值, 最后一个有效 (由于覆盖机制).
 *
 * <p>通过覆盖{@code convertPropertyValue}方法, 可以在读取属性值后转换它们.
 * 例如, 可以在处理加密值之前相应地检测和解密加密值.
 */
public class PropertyOverrideConfigurer extends PropertyResourceConfigurer {

	public static final String DEFAULT_BEAN_NAME_SEPARATOR = ".";


	private String beanNameSeparator = DEFAULT_BEAN_NAME_SEPARATOR;

	private boolean ignoreInvalidKeys = false;

	/**
	 * 包含覆盖的Bean的名称
	 */
	private final Set<String> beanNames = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));


	/**
	 * 在bean名称和属性路径之间设置分隔符.
	 * 默认 (".").
	 */
	public void setBeanNameSeparator(String beanNameSeparator) {
		this.beanNameSeparator = beanNameSeparator;
	}

	/**
	 * 设置是否忽略无效键. 默认 "false".
	 * <p>如果忽略无效键, 那么不会遵循 'beanName.property' 格式的键 (或引用无效的bean名称或属性)只会在debug级别记录.
	 * 这允许在属性文件中具有任意其他键.
	 */
	public void setIgnoreInvalidKeys(boolean ignoreInvalidKeys) {
		this.ignoreInvalidKeys = ignoreInvalidKeys;
	}


	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException {

		for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements();) {
			String key = (String) names.nextElement();
			try {
				processKey(beanFactory, key, props.getProperty(key));
			}
			catch (BeansException ex) {
				String msg = "Could not process key '" + key + "' in PropertyOverrideConfigurer";
				if (!this.ignoreInvalidKeys) {
					throw new BeanInitializationException(msg, ex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug(msg, ex);
				}
			}
		}
	}

	/**
	 * 将给定键作为 'beanName.property' 条目处理.
	 */
	protected void processKey(ConfigurableListableBeanFactory factory, String key, String value)
			throws BeansException {

		int separatorIndex = key.indexOf(this.beanNameSeparator);
		if (separatorIndex == -1) {
			throw new BeanInitializationException("Invalid key '" + key +
					"': expected 'beanName" + this.beanNameSeparator + "property'");
		}
		String beanName = key.substring(0, separatorIndex);
		String beanProperty = key.substring(separatorIndex+1);
		this.beanNames.add(beanName);
		applyPropertyValue(factory, beanName, beanProperty, value);
		if (logger.isDebugEnabled()) {
			logger.debug("Property '" + key + "' set to value [" + value + "]");
		}
	}

	/**
	 * 将给定的属性值应用于相应的bean.
	 */
	protected void applyPropertyValue(
			ConfigurableListableBeanFactory factory, String beanName, String property, String value) {

		BeanDefinition bd = factory.getBeanDefinition(beanName);
		while (bd.getOriginatingBeanDefinition() != null) {
			bd = bd.getOriginatingBeanDefinition();
		}
		PropertyValue pv = new PropertyValue(property, value);
		pv.setOptional(this.ignoreInvalidKeys);
		bd.getPropertyValues().addPropertyValue(pv);
	}


	/**
	 * 是否有这个bean的覆盖?
	 * 仅在处理至少一次后才有效.
	 * 
	 * @param beanName 要查询其状态的bean的名称
	 * 
	 * @return 是否存在已命名的bean的属性覆盖
	 */
	public boolean hasPropertyOverridesFor(String beanName) {
		return this.beanNames.contains(beanName);
	}

}
