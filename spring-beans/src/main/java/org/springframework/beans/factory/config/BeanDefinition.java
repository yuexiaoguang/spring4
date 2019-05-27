package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;

/**
 * BeanDefinition描述了一个bean实例, 它具有属性值, 构造函数参数值, 以及具体实现提供的更多信息.
 *
 * <p>这只是一个最小的接口:
 * 主要目的是允许{@link BeanFactoryPostProcessor}(如{@link PropertyPlaceholderConfigurer})反射并修改属性值和其他bean元数据.
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * 标准单例范围的范围标识符: "singleton".
	 * <p>请注意, 扩展bean工厂可能支持更多范围.
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * 标准原型范围的范围标识符: "prototype".
	 * <p>请注意, 扩展bean工厂可能支持更多范围.
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * 表明{@code BeanDefinition}是应用程序的主要部分. 通常对应于用户定义的bean.
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * 表明{@code BeanDefinition}是某些较大配置的支持部分,
	 * 通常是外部 {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 * {@code SUPPORT} bean被认为非常重要, 可以在更仔细地查看特定的
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}时注意,
	 * 但是在查看应用程序的整体配置时不是.
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * 表明{@code BeanDefinition}提供完全后台角色并且与最终用户无关.
	 * 注册完全在{@link org.springframework.beans.factory.parsing.ComponentDefinition}内部工作的bean时使用.
	 */
	int ROLE_INFRASTRUCTURE = 2;


	// Modifiable attributes

	/**
	 * 设置此bean定义的父定义的名称.
	 */
	void setParentName(String parentName);

	/**
	 * 返回此bean定义的父定义的名称.
	 */
	String getParentName();

	/**
	 * 指定此bean定义的bean类名称.
	 * <p>可以在bean工厂后处理期间修改类名, 通常用解析后的变体替换原始类名.
	 */
	void setBeanClassName(String beanClassName);

	/**
	 * 返回此bean定义的当前bean类名.
	 * <p>请注意, 这不必是运行时使用的实际类名, 如果子定义覆盖/继承其父类的类名.
	 * 此外, 这可能只是调用工厂方法的类, 或者在工厂bean引用调用方法的情况下甚至可能为空.
	 * 因此, 不要在运行时将其视为最终的bean类型, 而是仅在单个bean定义级别将其用于解析目的.
	 */
	String getBeanClassName();

	/**
	 * 覆盖此bean的目标范围, 指定新的范围名称.
	 */
	void setScope(String scope);

	/**
	 * 返回此bean的当前目标作用域的名称, 或{@code null}.
	 */
	String getScope();

	/**
	 * 设置是否应该延迟初始化此bean.
	 * <p>如果是{@code false}, bean将在启动时由bean工厂实例化, 这些工厂执行单例的初始化.
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * 返回是否应该延迟初始化此bean, i.e. 在启动时不实时的实例化. 仅适用于单例bean.
	 */
	boolean isLazyInit();

	/**
	 * 设置此bean依赖其初始化的bean的名称.
	 * bean工厂将保证首先初始化这些bean.
	 */
	void setDependsOn(String... dependsOn);

	/**
	 * 返回此bean依赖的bean的名称.
	 */
	String[] getDependsOn();

	/**
	 * 设置此bean是否可以自动连接到其他bean.
	 * <p>请注意, 此标志旨在仅影响基于类型的自动装配.
	 * 它不会影响名称的显式引用, 即使指定的bean未标记为autowire候选者, 也将得到解析.
	 * 因此, 如果名称匹配, 按名称自动装配将注入bean.
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 返回此bean是否可以自动连接到其他bean中.
	 */
	boolean isAutowireCandidate();

	/**
	 * 设置此bean是否为主要的autowire候选者.
	 * <p>如果这个值对于多个匹配的候选者中的一个bean来说是{@code true}, 那么它将成为主要的.
	 */
	void setPrimary(boolean primary);

	/**
	 * 返回此bean是否是主要的autowire候选者.
	 */
	boolean isPrimary();

	/**
	 * 指定要使用的工厂bean.
	 * 这是调用指定工厂方法的bean的名称.
	 */
	void setFactoryBeanName(String factoryBeanName);

	/**
	 * 返回工厂bean名称.
	 */
	String getFactoryBeanName();

	/**
	 * 指定工厂方法. 将使用构造函数参数调用此方法, 如果没有指定, 则没有参数.
	 * 将在指定的工厂bean上调用该方法, 或者作为本地bean类的静态方法.
	 */
	void setFactoryMethodName(String factoryMethodName);

	/**
	 * 返回工厂方法.
	 */
	String getFactoryMethodName();

	/**
	 * 返回此bean的构造函数参数值.
	 * <p>可以在bean工厂后处理期间修改返回的实例.
	 * 
	 * @return the ConstructorArgumentValues object (never {@code null})
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * 返回要应用于bean的新实例的属性值.
	 * <p>可以在bean工厂后处理期间修改返回的实例.
	 * 
	 * @return the MutablePropertyValues object (never {@code null})
	 */
	MutablePropertyValues getPropertyValues();


	// Read-only attributes

	/**
	 * 返回是否为<b>Singleton</b>, 并在所有调用上返回单个共享实例.
	 */
	boolean isSingleton();

	/**
	 * 返回是否为<b>Prototype</b>, 每次调用都返回一个独立的实例.
	 */
	boolean isPrototype();

	/**
	 * 返回此bean是否为 "abstract", 即不实例化.
	 */
	boolean isAbstract();

	/**
	 * 获取此 {@code BeanDefinition}的角色.
	 * 角色提供框架和工具, 以指示特定{@code BeanDefinition}的角色和重要性.
	 */
	int getRole();

	/**
	 * 返回此bean定义的可读描述.
	 */
	String getDescription();

	/**
	 * 返回此bean定义来自的资源的描述 (为了在出现错误时显示上下文).
	 */
	String getResourceDescription();

	/**
	 * 返回原始BeanDefinition, 或{@code null}.
	 * 允许检索修饰的bean定义.
	 * <p>请注意, 此方法返回直接创建者. 遍历创建者链以查找用户定义的原始BeanDefinition.
	 */
	BeanDefinition getOriginatingBeanDefinition();

}
