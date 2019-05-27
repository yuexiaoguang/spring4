package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * 枚举确定自动装配状态: 即, bean是否应该使用setter注入由Spring容器自动注入其依赖项.
 * 这是Spring DI的核心概念.
 *
 * <p>可用于基于注解的配置, 例如对于AspectJ AnnotationBeanConfigurer切面.
 */
public enum Autowire {

	/**
	 * 没有自动装配.
	 */
	NO(AutowireCapableBeanFactory.AUTOWIRE_NO),

	/**
	 * 按名称自动装配bean属性.
	 */
	BY_NAME(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME),

	/**
	 * 按类型自动装配bean属性.
	 */
	BY_TYPE(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);


	private final int value;


	Autowire(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

	/**
	 * 返回这是否代表实际的自动装配值.
	 * 
	 * @return 是否指定了实际的自动装配 (either BY_NAME or BY_TYPE)
	 */
	public boolean isAutowire() {
		return (this == BY_NAME || this == BY_TYPE);
	}

}
