package org.springframework.core.style;

/**
 * 根据Spring约定封装值String样式算法的策略.
 */
public interface ValueStyler {

	/**
	 * 设置给定值的样式, 返回String表示.
	 * 
	 * @param value 要设置样式的Object值
	 * 
	 * @return 样式的String
	 */
	String style(Object value);

}
