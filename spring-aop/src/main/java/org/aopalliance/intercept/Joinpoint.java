package org.aopalliance.intercept;

import java.lang.reflect.AccessibleObject;

/**
 * 此接口表示通用运行时连接点 (在AOP术语中).
 *
 * <p>运行时连接点是在静态连接点(i.e. 在程序中的位置)上发生的事件. 例如, 调用是方法上的运行时连接点 (静态连接点).
 * 通常可以使用{@link #getStaticPart()}方法检索给定连接点的静态部分.
 *
 * <p>在拦截框架的背景下, 然后，运行时连接点是对可访问对象的访问的具体化 (方法, 构造方法, 字段), i.e. 连接点的静态部分.
 * 它被传递给安装在静态连接点上的拦截器.
 */
public interface Joinpoint {

	/**
	 * 继续进入链中的下一个拦截器.
	 * <p>此方法的实现和语义取决于实际的连接点类型(子接口).
	 * 
	 * @return 查看子接口的proceed()定义
	 * @throws Throwable 如果连接点抛出异常
	 */
	Object proceed() throws Throwable;

	/**
	 * 返回保存当前连接点的静态部分的对象.
	 * <p>例如, 调用的目标对象.
	 * 
	 * @return 对象 (可以是 null, 如果可访问对象是 static)
	 */
	Object getThis();

	/**
	 * 返回此连接点的静态部分.
	 * <p>静态部分是一个可访问的对象，其上安装了一系列拦截器.
	 */
	AccessibleObject getStaticPart();

}
