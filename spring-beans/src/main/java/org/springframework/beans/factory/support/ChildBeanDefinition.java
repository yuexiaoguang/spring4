package org.springframework.beans.factory.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.util.ObjectUtils;

/**
 * bean的Bean定义, 从父级继承设置.
 * 子级bean定义对父级bean定义具有固定依赖性.
 *
 * <p>子级bean定义将从父级继承构造函数参数值, 属性值, 覆盖方法, 并带有添加新值的选项.
 * 如果指定了init方法, destroy方法和/或静态工厂方法, 它们将覆盖相应的父级设置.
 * 其余设置将始终从子级定义中获取:
 * 取决于, autowire模式, 依赖项检查, 单例, 延迟初始化.
 *
 * <p><b>NOTE:</b> 从Spring 2.5开始, 以编程方式注册bean定义的首选方法是{@link GenericBeanDefinition}类,
 * 它允许通过 {@link GenericBeanDefinition#setParentName}方法动态定义父级依赖项.
 * 这有效地取代了大多数用例的ChildBeanDefinition类.
 */
@SuppressWarnings("serial")
public class ChildBeanDefinition extends AbstractBeanDefinition {

	private String parentName;


	/**
	 * 为给定的父级创建新的ChildBeanDefinition, 通过其bean属性和配置方法进行配置.
	 * 
	 * @param parentName 父级bean的名称
	 */
	public ChildBeanDefinition(String parentName) {
		super();
		this.parentName = parentName;
	}

	/**
	 * @param parentName 父级bean的名称
	 * @param pvs 子级的额外属性值
	 */
	public ChildBeanDefinition(String parentName, MutablePropertyValues pvs) {
		super(null, pvs);
		this.parentName = parentName;
	}

	/**
	 * @param parentName 父级bean的名称
	 * @param cargs 要应用的构造函数参数值
	 * @param pvs 子级的额外属性值
	 */
	public ChildBeanDefinition(
			String parentName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
	}

	/**
	 * @param parentName 父级bean的名称
	 * @param beanClass 要实例化的bean的类
	 * @param cargs 要应用的构造函数参数值
	 * @param pvs 要应用的属性值
	 */
	public ChildBeanDefinition(
			String parentName, Class<?> beanClass, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
		setBeanClass(beanClass);
	}

	/**
	 * 获取bean类名称以避免实时的加载bean类.
	 * 
	 * @param parentName 父级bean的名称
	 * @param beanClassName 要实例化的类的名称
	 * @param cargs 要应用的构造函数参数值
	 * @param pvs 要应用的属性值
	 */
	public ChildBeanDefinition(
			String parentName, String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {

		super(cargs, pvs);
		this.parentName = parentName;
		setBeanClassName(beanClassName);
	}

	/**
	 * 深克隆给定的bean定义.
	 * 
	 * @param original 要从中复制的原始bean定义
	 */
	public ChildBeanDefinition(ChildBeanDefinition original) {
		super(original);
	}


	@Override
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	@Override
	public String getParentName() {
		return this.parentName;
	}

	@Override
	public void validate() throws BeanDefinitionValidationException {
		super.validate();
		if (this.parentName == null) {
			throw new BeanDefinitionValidationException("'parentName' must be set in ChildBeanDefinition");
		}
	}


	@Override
	public AbstractBeanDefinition cloneBeanDefinition() {
		return new ChildBeanDefinition(this);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ChildBeanDefinition)) {
			return false;
		}
		ChildBeanDefinition that = (ChildBeanDefinition) other;
		return (ObjectUtils.nullSafeEquals(this.parentName, that.parentName) && super.equals(other));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.parentName) * 29 + super.hashCode();
	}

	@Override
	public String toString() {
		return "Child bean with parent '" + this.parentName + "': " + super.toString();
	}

}
