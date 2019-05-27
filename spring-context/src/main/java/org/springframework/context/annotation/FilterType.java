package org.springframework.context.annotation;

/**
 * 可以与{@link ComponentScan @ComponentScan}一起使用的类型过滤器的枚举.
 */
public enum FilterType {

	/**
	 * 过滤使用给定注解标记的候选项.
	 */
	ANNOTATION,

	/**
	 * 过滤可分配给定类型的候选项.
	 */
	ASSIGNABLE_TYPE,

	/**
	 * 过滤与给定AspectJ类型模式表达式匹配的候选项.
	 */
	ASPECTJ,

	/**
	 * 过滤匹配给定正则表达式模式的候选项.
	 */
	REGEX,

	/** 
	 * 过滤使用给定的自定义{@link org.springframework.core.type.filter.TypeFilter}实现的候选项.
	 */
	CUSTOM

}
