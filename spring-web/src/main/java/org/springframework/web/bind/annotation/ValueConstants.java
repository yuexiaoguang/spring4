package org.springframework.web.bind.annotation;

/**
 * 绑定注解之间共享的公共值常量.
 */
public interface ValueConstants {

	/**
	 * 常量定义一个没有默认值的值  - 作为{@code null}的替代, 因为不能在注解属性中使用{@code null}.
	 * <p>这是16个unicode字符的人工排列, 其唯一目的是永远不匹配用户声明的值.
	 */
	String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

}
