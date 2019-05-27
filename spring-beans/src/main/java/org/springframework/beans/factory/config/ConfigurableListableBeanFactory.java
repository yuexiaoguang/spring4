package org.springframework.beans.factory.config;

import java.util.Iterator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * 由大多数可列出的bean工厂实现的配置接口.
 * 除{@link ConfigurableBeanFactory}之外, 它提供了分析和修改bean定义以及预先实例化单例的工具.
 *
 * <p>{@link org.springframework.beans.factory.BeanFactory}的这个子接口不适用于普通的应用程序代码:
 * 对于一般用例, 请使用{@link org.springframework.beans.factory.BeanFactory}或{@link org.springframework.beans.factory.ListableBeanFactory}.
 * 这个接口只是为了允许框架内部的即插即用, 即使在需要访问bean工厂配置方法的时候.
 */
public interface ConfigurableListableBeanFactory
		extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

	/**
	 * 忽略自动装配的给定依赖关系类型: 例如, String.
	 * 默认无.
	 * 
	 * @param type 要忽略的依赖类型
	 */
	void ignoreDependencyType(Class<?> type);

	/**
	 * 忽略给定的自动装配的依赖接口.
	 * <p>这通常由应用程序上下文用于注册以其他方式解析的依赖项,
	 * 例如BeanFactory通过BeanFactoryAware, 或ApplicationContext通过ApplicationContextAware.
	 * <p>默认情况下, 只忽略BeanFactoryAware接口.
	 * 要忽略其他类型, 请为每种类型调用此方法.
	 * 
	 * @param ifc 要忽略的依赖接口
	 */
	void ignoreDependencyInterface(Class<?> ifc);

	/**
	 * 使用相应的自动装配值注册特殊依赖关系类型.
	 * <p>这适用于可自动装配但在工厂中未定义为bean的工厂/上下文引用:
	 * e.g. ApplicationContext类型的依赖关系解析为bean所在的ApplicationContext实例.
	 * <p>Note: 在普通的BeanFactory中没有注册这样的默认类型, 即使对于BeanFactory接口本身也没有.
	 * 
	 * @param dependencyType 要注册的依赖类型.
	 * 这通常是一个基础接口, 如BeanFactory, 如果声明为自动装配依赖项, 则解析它的扩展名 (e.g. ListableBeanFactory),
	 * 只要给定值实际实现扩展接口.
	 * @param autowiredValue 相应的自动装配值.
	 * 这也可能是{@link org.springframework.beans.factory.ObjectFactory}接口的实现, 它允许实际目标值的延迟解析.
	 */
	void registerResolvableDependency(Class<?> dependencyType, Object autowiredValue);

	/**
	 * 确定指定的bean是否有资格作为autowire候选者, 被注入到声明匹配类型的依赖项的其他bean中.
	 * <p>此方法也会检查祖先工厂.
	 * 
	 * @param beanName 要检查的bean的名称
	 * @param descriptor 要解析的依赖项的描述符
	 * 
	 * @return 该bean是否应被视为autowire候选者
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 */
	boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException;

	/**
	 * 返回指定bean的已注册BeanDefinition, 允许访问其属性值和构造函数参数值 (可以在bean工厂后处理期间修改).
	 * <p>返回的BeanDefinition对象不应是副本, 而应是在工厂中注册的原始定义对象.
	 * 这意味着它应该可以转换为更具体的实现类型.
	 * <p><b>NOTE:</b> 这种方法不考虑祖先工厂. 它仅用于访问此工厂的本地bean定义.
	 * 
	 * @param beanName bean的名称
	 * 
	 * @return 注册的BeanDefinition
	 * @throws NoSuchBeanDefinitionException 如果在此工厂中没有定义给定名称的bean
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回此工厂管理的所有bean名称的统一视图.
	 * <p>包括bean定义名称以及手动注册的单例实例的名称, bean定义名称始终排在第一位,
	 * 类似于bean名称的类型/注解特定检索的工作原理.
	 * 
	 * @return bean名称视图的复合迭代器
	 */
	Iterator<String> getBeanNamesIterator();

	/**
	 * 清除合并的bean定义缓存, 删除尚未被视为有资格进行完整元数据缓存的bean条目.
	 * <p>通常在更改原始bean定义后触发, e.g. 应用{@link BeanFactoryPostProcessor}之后.
	 * 请注意, 此时已创建的bean的元数据将保留.
	 */
	void clearMetadataCache();

	/**
	 * 冻结所有bean定义, 表明注册的bean定义不会被进一步修改或后处理.
	 * <p>这允许工厂积极地缓存bean定义元数据.
	 */
	void freezeConfiguration();

	/**
	 * 返回是否冻结此工厂的bean定义, i.e. 不应该进一步修改或后处理.
	 * 
	 * @return {@code true} 如果工厂的配置被认为是冻结的
	 */
	boolean isConfigurationFrozen();

	/**
	 * 确保实例化所有非lazy-init的单例, 也考虑{@link org.springframework.beans.factory.FactoryBean FactoryBeans}.
	 * 如果需要, 通常在工厂设置结束时调用.
	 * 
	 * @throws BeansException 如果无法创建其中一个单例bean.
	 * Note: 这可能已经使工厂已经初始化了一些bean! 在这种情况下, 请调用{@link #destroySingletons()}进行完全清理.
	 */
	void preInstantiateSingletons() throws BeansException;

}
