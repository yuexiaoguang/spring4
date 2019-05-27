package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 当{@code BeanFactory}被要求提供无法找到定义的bean实例时抛出异常.
 * 这可能指向不存在的bean, 非唯一bean, 或没有关联bean定义的手动注册的单例实例.
 */
@SuppressWarnings("serial")
public class NoSuchBeanDefinitionException extends BeansException {

	private String beanName;

	private ResolvableType resolvableType;


	/**
	 * @param name 丢失的bean的名称
	 */
	public NoSuchBeanDefinitionException(String name) {
		super("No bean named '" + name + "' available");
		this.beanName = name;
	}

	/**
	 * @param name 丢失的bean的名称
	 * @param message detailed message describing the problem
	 */
	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named '" + name + "' available: " + message);
		this.beanName = name;
	}

	/**
	 * @param type 丢失的bean的必需类型
	 */
	public NoSuchBeanDefinitionException(Class<?> type) {
		this(ResolvableType.forClass(type));
	}

	/**
	 * @param type 丢失的bean的必需类型
	 * @param message detailed message describing the problem
	 */
	public NoSuchBeanDefinitionException(Class<?> type, String message) {
		this(ResolvableType.forClass(type), message);
	}

	/**
	 * @param type 丢失的bean的完整类型声明
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type) {
		super("No qualifying bean of type '" + type + "' available");
		this.resolvableType = type;
	}

	/**
	 * @param type 丢失的bean的完整类型声明
	 * @param message detailed message describing the problem
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type, String message) {
		super("No qualifying bean of type '" + type + "' available: " + message);
		this.resolvableType = type;
	}

	/**
	 * @param type 丢失的bean的必需类型
	 * @param dependencyDescription 原始依赖的描述
	 * @param message detailed message describing the problem
	 * @deprecated as of 4.3.4, in favor of {@link #NoSuchBeanDefinitionException(ResolvableType, String)}
	 */
	@Deprecated
	public NoSuchBeanDefinitionException(Class<?> type, String dependencyDescription, String message) {
		super("No qualifying bean" + (!StringUtils.hasLength(dependencyDescription) ?
				" of type '" + ClassUtils.getQualifiedName(type) + "'" : "") + " found for dependency" +
				(StringUtils.hasLength(dependencyDescription) ? " [" + dependencyDescription + "]" : "") +
				": " + message);
		this.resolvableType = ResolvableType.forClass(type);
	}


	/**
	 * 返回丢失的bean的名称, 如果是按名称查找失败.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回丢失的bean必需的类型, 如果是按类型查找失败.
	 */
	public Class<?> getBeanType() {
		return (this.resolvableType != null ? this.resolvableType.resolve() : null);
	}

	/**
	 * 返回丢失的bean所需的{@link ResolvableType}, 如果是按类型查找失败.
	 * @since 4.3.4
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * 返回当只需要一个匹配bean时找到的bean数量.
	 * 对于常规的NoSuchBeanDefinitionException, 这将总是返回 0.
	 */
	public int getNumberOfBeansFound() {
		return 0;
	}

}
