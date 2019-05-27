package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.util.ObjectUtils;

/**
 * 使用构建器模式构建 {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}.
 * 主要用于实现 Spring 2.0 {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers}.
 */
public class BeanDefinitionBuilder {

	public static BeanDefinitionBuilder genericBeanDefinition() {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder();
		builder.beanDefinition = new GenericBeanDefinition();
		return builder;
	}

	/**
	 * @param beanClass 正在为其创建定义的bean的{@code Class}
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(Class<?> beanClass) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder();
		builder.beanDefinition = new GenericBeanDefinition();
		builder.beanDefinition.setBeanClass(beanClass);
		return builder;
	}

	/**
	 * @param beanClassName 正在为其创建定义的bean的类名
	 */
	public static BeanDefinitionBuilder genericBeanDefinition(String beanClassName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder();
		builder.beanDefinition = new GenericBeanDefinition();
		builder.beanDefinition.setBeanClassName(beanClassName);
		return builder;
	}

	/**
	 * @param beanClass 正在为其创建定义的bean的{@code Class}
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass) {
		return rootBeanDefinition(beanClass, null);
	}

	/**
	 * @param beanClass 正在为其创建定义的bean的{@code Class}
	 * @param factoryMethodName 用于构造bean实例的方法的名称
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass, String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder();
		builder.beanDefinition = new RootBeanDefinition();
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * @param beanClassName 正在为其创建定义的bean的类名
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName) {
		return rootBeanDefinition(beanClassName, null);
	}

	/**
	 * @param beanClassName 正在为其创建定义的bean的类名
	 * @param factoryMethodName 用于构造bean实例的方法的名称
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName, String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder();
		builder.beanDefinition = new RootBeanDefinition();
		builder.beanDefinition.setBeanClassName(beanClassName);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * @param parentName 父级bean的名称
	 */
	public static BeanDefinitionBuilder childBeanDefinition(String parentName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder();
		builder.beanDefinition = new ChildBeanDefinition(parentName);
		return builder;
	}


	/**
	 * 正在创建的{@code BeanDefinition}实例.
	 */
	private AbstractBeanDefinition beanDefinition;

	/**
	 * 构造函数args的当前位置.
	 */
	private int constructorArgIndex;


	/**
	 * 强制使用工厂方法.
	 */
	private BeanDefinitionBuilder() {
	}

	/**
	 * 以原始(未经验证的)形式返回当前BeanDefinition对象.
	 */
	public AbstractBeanDefinition getRawBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * 验证并返回创建的BeanDefinition对象.
	 */
	public AbstractBeanDefinition getBeanDefinition() {
		this.beanDefinition.validate();
		return this.beanDefinition;
	}


	/**
	 * 设置此bean定义的父级定义的名称.
	 */
	public BeanDefinitionBuilder setParentName(String parentName) {
		this.beanDefinition.setParentName(parentName);
		return this;
	}

	/**
	 * 设置要用于此定义的静态工厂方法的名称, 以在此bean的类上调用.
	 */
	public BeanDefinitionBuilder setFactoryMethod(String factoryMethod) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		return this;
	}

	/**
	 * 设置要用于此定义的非静态工厂方法的名称, 包括要调用其方法的工厂实例的bean名称.
	 * @since 4.3.6
	 */
	public BeanDefinitionBuilder setFactoryMethodOnBean(String factoryMethod, String factoryBean) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		this.beanDefinition.setFactoryBeanName(factoryBean);
		return this;
	}

	/**
	 * 添加索引构造函数arg值. 内部跟踪当前索引, 所有添加都在当前点.
	 * 
	 * @deprecated since Spring 2.5, in favor of {@link #addConstructorArgValue}.
	 * This variant just remains around for Spring Security 2.x compatibility.
	 */
	@Deprecated
	public BeanDefinitionBuilder addConstructorArg(Object value) {
		return addConstructorArgValue(value);
	}

	/**
	 * 添加索引构造函数arg值. 内部跟踪当前索引, 所有添加都在当前点.
	 */
	public BeanDefinitionBuilder addConstructorArgValue(Object value) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, value);
		return this;
	}

	/**
	 * 添加对命名bean的引用作为构造函数arg.
	 */
	public BeanDefinitionBuilder addConstructorArgReference(String beanName) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * 在给定名称下添加提供的属性值.
	 */
	public BeanDefinitionBuilder addPropertyValue(String name, Object value) {
		this.beanDefinition.getPropertyValues().add(name, value);
		return this;
	}

	/**
	 * 在指定的属性下添加对指定bean名称的引用.
	 * 
	 * @param name 要添加引用的属性的名称
	 * @param beanName 被引用的bean的名称
	 */
	public BeanDefinitionBuilder addPropertyReference(String name, String beanName) {
		this.beanDefinition.getPropertyValues().add(name, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * 设置此定义的init方法.
	 */
	public BeanDefinitionBuilder setInitMethodName(String methodName) {
		this.beanDefinition.setInitMethodName(methodName);
		return this;
	}

	/**
	 * 设置此定义的destroy方法.
	 */
	public BeanDefinitionBuilder setDestroyMethodName(String methodName) {
		this.beanDefinition.setDestroyMethodName(methodName);
		return this;
	}


	/**
	 * 设置此定义的作用域.
	 */
	public BeanDefinitionBuilder setScope(String scope) {
		this.beanDefinition.setScope(scope);
		return this;
	}

	/**
	 * 设置此定义是否为抽象的.
	 */
	public BeanDefinitionBuilder setAbstract(boolean flag) {
		this.beanDefinition.setAbstract(flag);
		return this;
	}

	/**
	 * 设置是否应该延迟地初始化此定义的bean.
	 */
	public BeanDefinitionBuilder setLazyInit(boolean lazy) {
		this.beanDefinition.setLazyInit(lazy);
		return this;
	}

	/**
	 * 设置此定义的自动装配模式.
	 */
	public BeanDefinitionBuilder setAutowireMode(int autowireMode) {
		beanDefinition.setAutowireMode(autowireMode);
		return this;
	}

	/**
	 * 设置此定义的依赖项检查模式.
	 */
	public BeanDefinitionBuilder setDependencyCheck(int dependencyCheck) {
		beanDefinition.setDependencyCheck(dependencyCheck);
		return this;
	}

	/**
	 * 将指定的bean名称追加到此定义所依赖的bean列表中.
	 */
	public BeanDefinitionBuilder addDependsOn(String beanName) {
		if (this.beanDefinition.getDependsOn() == null) {
			this.beanDefinition.setDependsOn(beanName);
		}
		else {
			String[] added = ObjectUtils.addObjectToArray(this.beanDefinition.getDependsOn(), beanName);
			this.beanDefinition.setDependsOn(added);
		}
		return this;
	}

	/**
	 * 设置此定义的角色.
	 */
	public BeanDefinitionBuilder setRole(int role) {
		this.beanDefinition.setRole(role);
		return this;
	}

}
