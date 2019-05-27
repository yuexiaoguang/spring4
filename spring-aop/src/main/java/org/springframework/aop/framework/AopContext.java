package org.springframework.aop.framework;

import org.springframework.core.NamedThreadLocal;

/**
 * 包含静态方法的类，用于获取有关当前AOP调用的信息.
 *
 * <p>如果AOP框架配置为公开当前代理(不是默认的)，则{@code currentProxy()} 方法可用. 它返回正在使用的AOP代理.
 * 目标对象或增强可以使用它来进行增强的调用, 与 {@code getEJBObject()}一样, 可以在EJB中使用. 还可以使用它来查找增强配置.
 *
 * <p>Spring的AOP框架默认不公开代理, 因为这样做有性能成本.
 *
 * <p>此类中的功能可能由需要访问调用资源的目标对象使用. 然而, 当有合理的替代方案时, 不应使用这种方法, 因为它使应用程序代码特别依赖于AOP和Spring AOP框架.
 */
public abstract class AopContext {

	/**
	 * 与此线程关联的AOP代理的ThreadLocal.
	 * 将包含{@code null}, 除非控制代理配置中的 "exposeProxy"属性已设置为"true".
	 */
	private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<Object>("Current AOP proxy");


	/**
	 * 尝试返回当前的AOP代理.
	 * 只有在通过AOP调用方法时, 此方法才可用, 并且AOP框架已设置为公开代理. 否则, 此方法将抛出 IllegalStateException.
	 * 
	 * @return Object 当前AOP 代理 (永远不会返回 {@code null})
	 * 
	 * @throws IllegalStateException 如果无法找到代理, 因为该方法是在AOP调用上下文之外调用的, 或者因为AOP框架尚未配置为公开代理
	 */
	public static Object currentProxy() throws IllegalStateException {
		Object proxy = currentProxy.get();
		if (proxy == null) {
			throw new IllegalStateException(
					"Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available.");
		}
		return proxy;
	}

	/**
	 * 通过 {@code currentProxy()}方法提供给定的代理.
	 * <p>请注意，调用者应小心保留旧值.
	 * 
	 * @param proxy 要公开的代理 (或 {@code null}重置)
	 * 
	 * @return 旧的代理, 如果未绑定, 可能为{@code null}
	 */
	static Object setCurrentProxy(Object proxy) {
		Object old = currentProxy.get();
		if (proxy != null) {
			currentProxy.set(proxy);
		}
		else {
			currentProxy.remove();
		}
		return old;
	}
}
