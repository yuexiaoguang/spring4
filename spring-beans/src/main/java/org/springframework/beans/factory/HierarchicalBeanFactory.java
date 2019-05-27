package org.springframework.beans.factory;

/**
 * 由bean工厂实现的子接口, 可以是层次结构的一部分.
 *
 * <p>可以在ConfigurableBeanFactory接口中找到允许以可配置方式设置父级bean工厂的相应{@code setParentBeanFactory}方法.
 */
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * 返回父bean工厂, 或{@code null}.
	 */
	BeanFactory getParentBeanFactory();

	/**
	 * 返回本地bean工厂是否包含给定名称的bean, 忽略祖先上下文中定义的bean.
	 * <p>这是{@code containsBean}的替代方案, 忽略祖先bean工厂中给定名称的bean.
	 * 
	 * @param name 要查询的bean的名称
	 * 
	 * @return 是否在本地工厂中定义了具有给定名称的bean
	 */
	boolean containsLocalBean(String name);

}
