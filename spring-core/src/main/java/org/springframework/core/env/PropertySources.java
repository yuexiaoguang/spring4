package org.springframework.core.env;

/**
 * 包含一个或多个{@link PropertySource}对象的持有者.
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * 返回是否包含具有给定名称的属性源.
	 * 
	 * @param name 要查找的{@linkplain PropertySource#getName() 属性源的名称}
	 */
	boolean contains(String name);

	/**
	 * 返回具有给定名称的属性源, 或{@code null}.
	 * 
	 * @param name 要查找的{@linkplain PropertySource#getName() 属性源的名称}
	 */
	PropertySource<?> get(String name);

}
