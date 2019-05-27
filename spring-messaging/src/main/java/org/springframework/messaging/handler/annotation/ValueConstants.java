package org.springframework.messaging.handler.annotation;

/**
 * 常用注解值常量.
 */
public interface ValueConstants {

	/**
	 * 定义一个没有默认值的值 - 作为{@code null}的替代, 因为不能在注解属性中使用它.
	 * <p>这是16个unicode字符的人工排列, 其唯一目的是永远不匹配用户声明的值.
	 */
	String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

}
