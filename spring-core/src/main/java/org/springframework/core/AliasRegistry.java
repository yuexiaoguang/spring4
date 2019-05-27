package org.springframework.core;

/**
 * 用于管理别名的通用接口.
 * 用作{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的超级接口.
 */
public interface AliasRegistry {

	/**
	 * 给定名称, 为其注册别名.
	 * 
	 * @param name 规范名称
	 * @param alias 要注册的别名
	 * 
	 * @throws IllegalStateException 如果别名已被使用且可能未被覆盖
	 */
	void registerAlias(String name, String alias);

	/**
	 * 从此注册表中删除指定的别名.
	 * 
	 * @param alias 要删除的别名
	 * 
	 * @throws IllegalStateException 如果没有找到这样的别名
	 */
	void removeAlias(String alias);

	/**
	 * 确定此给定名称是否定义为别名 (而不是实际注册的组件的名称).
	 * 
	 * @param name 要检查的名称
	 * 
	 * @return 给定名称是否为别名
	 */
	boolean isAlias(String name);

	/**
	 * 如果已定义, 则返回给定名称的别名.
	 * 
	 * @param name 名称
	 * 
	 * @return 别名, 或空数组
	 */
	String[] getAliases(String name);

}
