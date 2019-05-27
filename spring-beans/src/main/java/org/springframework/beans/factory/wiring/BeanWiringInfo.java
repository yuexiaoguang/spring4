package org.springframework.beans.factory.wiring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * 保存bean装配有关特定类的元数据信息.
 * 与{@link org.springframework.beans.factory.annotation.Configurable}注解和AspectJ {@code AnnotationBeanConfigurerAspect}结合使用.
 */
public class BeanWiringInfo {

	/**
	 * 按名称自动装配bean属性.
	 */
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * 按类型自动装配bean属性.
	 */
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;


	private String beanName = null;

	private boolean isDefaultBeanName = false;

	private int autowireMode = AutowireCapableBeanFactory.AUTOWIRE_NO;

	private boolean dependencyCheck = false;


	/**
	 * 创建一个默认的BeanWiringInfo, 它建议bean类期望的工厂和后处理器回调的简单初始化.
	 */
	public BeanWiringInfo() {
	}

	/**
	 * 创建一个指向给定bean名称的新BeanWiringInfo.
	 * 
	 * @param beanName 从中获取属性值的bean定义的名称
	 * 
	 * @throws IllegalArgumentException 如果提供的beanName是{@code null}, 则为空, 或者完全由空格组成
	 */
	public BeanWiringInfo(String beanName) {
		this(beanName, false);
	}

	/**
	 * 创建一个指向给定bean名称的新BeanWiringInfo.
	 * 
	 * @param beanName 从中获取属性值的bean定义的名称
	 * @param isDefaultBeanName 给定的bean名称是否是建议的默认bean名称, 不一定与实际的bean定义匹配
	 * 
	 * @throws IllegalArgumentException 如果提供的beanName是{@code null}, 则为空, 或者完全由空格组成
	 */
	public BeanWiringInfo(String beanName, boolean isDefaultBeanName) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
		this.isDefaultBeanName = isDefaultBeanName;
	}

	/**
	 * 创建一个指示自动装配的新的BeanWiringInfo.
	 * 
	 * @param autowireMode {@link #AUTOWIRE_BY_NAME}/ {@link #AUTOWIRE_BY_TYPE}其中之一
	 * @param dependencyCheck 是否对bean实例中的对象引用执行依赖性检查 (自动装配之后)
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code autowireMode}不是允许的值之一
	 */
	public BeanWiringInfo(int autowireMode, boolean dependencyCheck) {
		if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
			throw new IllegalArgumentException("Only constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE supported");
		}
		this.autowireMode = autowireMode;
		this.dependencyCheck = dependencyCheck;
	}


	/**
	 * 返回此BeanWiringInfo是否指示自动装配.
	 */
	public boolean indicatesAutowiring() {
		return (this.beanName == null);
	}

	/**
	 * 返回此BeanWiringInfo指向的特定bean名称.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回特定的bean名称是否是建议的默认bean名称, 不一定与工厂中的实际bean定义匹配.
	 */
	public boolean isDefaultBeanName() {
		return this.isDefaultBeanName;
	}

	/**
	 * 如果指示自动装配, 则返回{@link #AUTOWIRE_BY_NAME}/{@link #AUTOWIRE_BY_TYPE}其中之一.
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 返回是否对bean实例中的对象引用执行依赖性检查 (自动装配之后).
	 */
	public boolean getDependencyCheck() {
		return this.dependencyCheck;
	}

}
