package org.springframework.beans.support;

/**
 * 用于按属性排序bean实例的定义.
 */
public interface SortDefinition {

	/**
	 * 返回要比较的bean属性的名称.
	 * 也可以是嵌套的bean属性路径.
	 */
	String getProperty();

	/**
	 * 返回是否应忽略String值中的大写和小写.
	 */
	boolean isIgnoreCase();

	/**
	 * 返回是升序 (true) 还是降序 (false).
	 */
	boolean isAscending();

}
