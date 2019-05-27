package org.springframework.context.annotation;

/**
 * 各种作用域代理选项.
 *
 * <p>有关stroped代理的确切内容的更完整讨论, 请参阅名为 '<em>Scoped beans as dependencies</em>'的Spring参考文档部分.
 */
public enum ScopedProxyMode {

	/**
	 * 默认通常等于 {@link #NO}, 除非在组件扫描指令级别配置了不同的默认值.
	 */
	DEFAULT,

	/**
	 * 不要创建作用域代理.
	 * <p>当与非单例作用域实例一起使用时,此代理模式通常不常用,
	 * 如果要将其用作 {@link #INTERFACES} 或 {@link #TARGET_CLASS}代理模式, 则应该使用它.
	 */
	NO,

	/**
	 * 创建一个JDK动态代理, 实现目标对象的类所暴露的所有接口.
	 */
	INTERFACES,

	/**
	 * 创建基于类的代理 (使用 CGLIB).
	 */
	TARGET_CLASS;

}
