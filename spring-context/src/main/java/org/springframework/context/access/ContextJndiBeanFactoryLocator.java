package org.springframework.context.access;

import javax.naming.NamingException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.util.StringUtils;

/**
 * BeanFactoryLocator实现, 它从JNDI环境变量中指定的一个或多个类路径位置创建BeanFactory.
 *
 * <p>此默认实现创建 {@link org.springframework.context.support.ClassPathXmlApplicationContext}.
 * 子类可以覆盖 {@link #createBeanFactory}以进行自定义实例化.
 */
public class ContextJndiBeanFactoryLocator extends JndiLocatorSupport implements BeanFactoryLocator {

	/**
	 * 任何数量的这些字符都被视为单个String值中多个Bean工厂配置路径之间的分隔符.
	 */
	public static final String BEAN_FACTORY_PATH_DELIMITERS = ",; \t\n";


	/**
	 * 加载/使用bean工厂, 由工厂Key指定, 该工厂Key是JNDI地址, 格式为 {@code java:comp/env/ejb/BeanFactoryPath}.
	 * 此JNDI位置的内容必须是包含一个或多个类路径资源名称的字符串
	 * (由分隔符 '{@code ,; \t\n}'分隔.
	 * 生成的BeanFactory (或ApplicationContext) 将从组合资源中创建.
	 */
	@Override
	public BeanFactoryReference useBeanFactory(String factoryKey) throws BeansException {
		try {
			String beanFactoryPath = lookup(factoryKey, String.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Bean factory path from JNDI environment variable [" + factoryKey +
						"] is: " + beanFactoryPath);
			}
			String[] paths = StringUtils.tokenizeToStringArray(beanFactoryPath, BEAN_FACTORY_PATH_DELIMITERS);
			return createBeanFactory(paths);
		}
		catch (NamingException ex) {
			throw new BootstrapException("Define an environment variable [" + factoryKey + "] containing " +
					"the class path locations of XML bean definition files", ex);
		}
	}

	/**
	 * 给定一个应该组合的类路径资源字符串数组, 创建BeanFactory实例.
	 * 这被拆分为一个单独的方法, 以便子类可以覆盖实际的BeanFactory实现类.
	 * <p>默认情况下委托给{@code createApplicationContext}, 将结果包装在ContextBeanFactoryReference中.
	 * 
	 * @param resources 表示类路径资源名称的字符串数组
	 * 
	 * @return 创建的BeanFactory, 包装在BeanFactoryReference中
	 * (例如, 包装ApplicationContext的ContextBeanFactoryReference)
	 * @throws BeansException 如果工厂创建失败
	 */
	protected BeanFactoryReference createBeanFactory(String[] resources) throws BeansException {
		ApplicationContext ctx = createApplicationContext(resources);
		return new ContextBeanFactoryReference(ctx);
	}

	/**
	 * 给定应该组合的类路径资源字符串数组, 创建ApplicationContext实例
	 * 
	 * @param resources 表示类路径资源名称的字符串数组
	 * 
	 * @return 创建的ApplicationContext
	 * @throws BeansException 如果上下文创建失败
	 */
	protected ApplicationContext createApplicationContext(String[] resources) throws BeansException {
		return new ClassPathXmlApplicationContext(resources);
	}

}
