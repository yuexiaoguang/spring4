package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AliasRegistry;

/**
 * 保存bean定义的注册表的接口, 例如RootBeanDefinition和ChildBeanDefinition实例.
 * 通常由BeanFactories实现, BeanFactories内部使用AbstractBeanDefinition层次结构.
 *
 * <p>这是Spring的bean工厂包中唯一封装bean定义注册的接口.
 * 标准BeanFactory接口仅涵盖对完全配置的工厂实例的访问.
 *
 * <p>Spring的bean定义读取器希望能够在这个接口的实现上工作.
 * Spring核心中的已知实现者是DefaultListableBeanFactory和GenericApplicationContext.
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * 使用此注册表注册新的bean定义.
	 * 必须支持RootBeanDefinition和ChildBeanDefinition.
	 * 
	 * @param beanName 要注册的bean实例的名称
	 * @param beanDefinition 要注册的bean实例的定义
	 * 
	 * @throws BeanDefinitionStoreException 如果BeanDefinition无效, 或者已经有指定bean名称的BeanDefinition (不允许覆盖它)
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * 删除给定名称的BeanDefinition.
	 * 
	 * @param beanName 要注册的bean实例的名称
	 * 
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定bean名称的BeanDefinition.
	 * 
	 * @param beanName 要查找其定义的bean的名称
	 * 
	 * @return 给定名称的BeanDefinition (never {@code null})
	 * @throws NoSuchBeanDefinitionException 如果没有这样的bean定义
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 检查此注册表是否包含具有给定名称的bean定义.
	 * 
	 * @param beanName 要查找的bean的名称
	 * 
	 * @return 此注册表是否包含具有给定名称的bean定义
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回此注册表中定义的所有bean的名称.
	 * 
	 * @return 此注册表中定义的所有bean的名称, 或空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回注册表中定义的bean数量.
	 * 
	 * @return 注册表中定义的bean数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 确定给定的b​​ean名称是否已在此注册表中使用, i.e. 是否存在以此名称注册的本地bean或别名.
	 * 
	 * @param beanName 要检查的名称
	 * 
	 * @return 是否已使用给定的bean名称
	 */
	boolean isBeanNameInUse(String beanName);

}
