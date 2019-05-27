package org.springframework.context.support;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;

/**
 * {@link org.springframework.context.ApplicationContext}实现,
 * 支持bean和消息的编程注册, 而不是从外部配置源读取bean定义.
 * 主要用于测试.
 */
public class StaticApplicationContext extends GenericApplicationContext {

	private final StaticMessageSource staticMessageSource;


	public StaticApplicationContext() throws BeansException {
		this(null);
	}

	public StaticApplicationContext(ApplicationContext parent) throws BeansException {
		super(parent);

		// 初始化并注册 StaticMessageSource.
		this.staticMessageSource = new StaticMessageSource();
		getBeanFactory().registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.staticMessageSource);
	}


	/**
	 * 重写将其变为无操作, 对测试用例更加宽容.
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * 返回此上下文使用的内部StaticMessageSource.
	 * 可用于在其上注册消息.
	 */
	public final StaticMessageSource getStaticMessageSource() {
		return this.staticMessageSource;
	}

	/**
	 * 在底层bean工厂中注册一个单例bean.
	 * <p>如需更高级的需求, 直接使用底层的BeanFactory注册.
	 */
	public void registerSingleton(String name, Class<?> clazz) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(clazz);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 在底层bean工厂中注册一个单例bean.
	 * <p>如需更高级的需求, 直接使用底层的BeanFactory注册.
	 */
	public void registerSingleton(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(clazz);
		bd.setPropertyValues(pvs);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 向底层bean工厂注册原型bean.
	 * <p>如需更高级的需求, 直接使用底层的BeanFactory注册.
	 */
	public void registerPrototype(String name, Class<?> clazz) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		bd.setBeanClass(clazz);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 向底层bean工厂注册原型bean.
	 * <p>如需更高级的需求, 直接使用底层的BeanFactory注册.
	 */
	public void registerPrototype(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		bd.setBeanClass(clazz);
		bd.setPropertyValues(pvs);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * 将给定消息与给定代码相关联.
	 * 
	 * @param code 查找代码
	 * @param locale 应在其中找到的区域设置消息
	 * @param defaultMessage 与此查找代码关联的消息
	 */
	public void addMessage(String code, Locale locale, String defaultMessage) {
		getStaticMessageSource().addMessage(code, locale, defaultMessage);
	}

}
