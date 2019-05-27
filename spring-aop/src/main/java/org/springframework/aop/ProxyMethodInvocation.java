package org.springframework.aop;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 扩展AOP联盟{@link org.aopalliance.intercept.MethodInvocation}接口, 允许访问通过方法调用进行的代理.
 *
 * <p>用代理替换返回值时很有用, 例如, 如果调用目标返回自身.
 */
public interface ProxyMethodInvocation extends MethodInvocation {

	/**
	 * 返回此方法调用使用的代理.
	 * 
	 * @return 原始代理对象
	 */
	Object getProxy();

	/**
	 * 创建此对象的克隆.
	 * 如果在此对象上调用{@code proceed()}之前完成克隆, 每个克隆可以调用{@code proceed()} 一次以多次调用连接点 (以及增强链的其余部分).
	 * 
	 * @return 这个调用的克隆. 每个克隆可以调用{@code proceed()}一次.
	 */
	MethodInvocation invocableClone();

	/**
	 * 创建此对象的克隆.
	 * 如果在此对象上调用{@code proceed()}之前完成克隆, 每个克隆可以调用{@code proceed()} 一次以多次调用连接点 (以及增强链的其余部分).
	 * 
	 * @param arguments 克隆调用应该使用的参数, 覆盖原始参数
	 * 
	 * @return 这个调用的克隆. 每个克隆可以调用{@code proceed()}一次.
	 */
	MethodInvocation invocableClone(Object... arguments);

	/**
	 * 设置要在此链中的增强的后续调用中使用的参数.
	 * 
	 * @param arguments 参数数组
	 */
	void setArguments(Object... arguments);

	/**
	 * 将具有给定值的指定用户属性添加到此调用.
	 * <p>这些属性不在AOP框架内使用. 它们只是作为调用对象的一部分保存，用于特殊拦截器.
	 * 
	 * @param key 属性的名称
	 * @param value 属性的值, 或 {@code null}重置它
	 */
	void setUserAttribute(String key, Object value);

	/**
	 * 返回指定用户属性的值.
	 * 
	 * @param key 属性的名称
	 * 
	 * @return 属性的值, 或 {@code null}未设置
	 */
	Object getUserAttribute(String key);

}
