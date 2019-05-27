package org.springframework.aop.framework;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;

/**
 * AOP联盟{@link org.aopalliance.intercept.MethodInvocation}接口的Spring实现,
 * 实现扩展的{@link org.springframework.aop.ProxyMethodInvocation}接口.
 *
 * <p>使用反射调用目标对象. 子类可以覆盖{@link #invokeJoinpoint()}方法来更改此行为,
 * 所以这对于更专用的MethodInvocation实现来说也是一个有用的基类.
 *
 * <p>可以克隆调用, 反复调用{@link #proceed()}(每个克隆一次), 使用 {@link #invocableClone()}方法.
 * 也可以将自定义属性附加到调用, 使用 {@link #setUserAttribute} / {@link #getUserAttribute} 方法.
 *
 * <p><b>NOTE:</b>此类被视为内部类，不应直接访问. 公开的唯一原因是与现有框架集成的兼容性 (e.g. Pitchfork).
 * 出于其他目的, 使用 {@link ProxyMethodInvocation}接口替代.
 */
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

	protected final Object proxy;

	protected final Object target;

	protected final Method method;

	protected Object[] arguments;

	private final Class<?> targetClass;

	/**
	 * 此调用的用户特定属性的延迟初始化的Map.
	 */
	private Map<String, Object> userAttributes;

	/**
	 * 需要动态检查的MethodInterceptor和InterceptorAndDynamicMethodMatcher的列表.
	 */
	protected final List<?> interceptorsAndDynamicMethodMatchers;

	/**
	 * 从正在调用的当前拦截器的0开始的索引.
	 * -1 直到调用: 那么是当前的拦截器.
	 */
	private int currentInterceptorIndex = -1;


	/**
	 * @param proxy 调用的代理对象
	 * @param target 要调用的目标对象
	 * @param method 要调用的方法
	 * @param arguments 用于调用方法的参数
	 * @param targetClass 目标类, 对于 MethodMatcher 调用
	 * @param interceptorsAndDynamicMethodMatchers 应该应用的拦截器, 以及需要在运行时进行评估的InterceptorAndDynamicMethodMatcher.
	 * 必须已经找到此结构中包含的MethodMatcher, 并已尽可能的静态匹配. 传递数组可能会快10％左右, 但会使代码复杂化.
	 * 它只适用于静态切点.
	 */
	protected ReflectiveMethodInvocation(
			Object proxy, Object target, Method method, Object[] arguments,
			Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {

		this.proxy = proxy;
		this.target = target;
		this.targetClass = targetClass;
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
	}


	@Override
	public final Object getProxy() {
		return this.proxy;
	}

	@Override
	public final Object getThis() {
		return this.target;
	}

	@Override
	public final AccessibleObject getStaticPart() {
		return this.method;
	}

	/**
	 * 返回在代理接口上调用的方法.
	 * 可能或可能不对应于在该接口的底层实现上调用的方法.
	 */
	@Override
	public final Method getMethod() {
		return this.method;
	}

	@Override
	public final Object[] getArguments() {
		return (this.arguments != null ? this.arguments : new Object[0]);
	}

	@Override
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}


	@Override
	public Object proceed() throws Throwable {
		//	从-1的索引开始, 并提前递增.
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			return invokeJoinpoint();
		}

		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// 在这里评估动态方法匹配器: 静态部分已被评估并发现匹配.
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			if (dm.methodMatcher.matches(this.method, this.targetClass, this.arguments)) {
				return dm.interceptor.invoke(this);
			}
			else {
				// 动态匹配失败.
				// 跳过此拦截器并调用链中的下一个.
				return proceed();
			}
		}
		else {
			// 这是一个拦截器, 所以只是调用它: 在构造此对象之前，将对切点进行静态评估.
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}

	/**
	 * 使用反射调用连接点.
	 * 子类可以覆盖它以使用自定义调用.
	 * 
	 * @return 连接点的返回值
	 * @throws Throwable 如果调用joinpoint导致异常
	 */
	protected Object invokeJoinpoint() throws Throwable {
		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
	}


	/**
	 * 此实现返回此调用对象的浅复制副本, 包括原始参数数组的独立副本.
	 * <p>在这种情况下, 我们想要一个浅复制副本: 想要使用相同的拦截器链和其他对象引用, 但我们想要一个当前拦截器索引的独立值.
	 */
	@Override
	public MethodInvocation invocableClone() {
		Object[] cloneArguments = null;
		if (this.arguments != null) {
			// 构建参数数组的独立副本.
			cloneArguments = new Object[this.arguments.length];
			System.arraycopy(this.arguments, 0, cloneArguments, 0, this.arguments.length);
		}
		return invocableClone(cloneArguments);
	}

	/**
	 * 此实现返回此调用对象的浅复制副本, 使用克隆的给定参数数组.
	 * <p>在这种情况下，我们想要一个浅复制副本: 我们想要使用相同的拦截器链和其他对象引用, 但我们想要一个当前拦截器索引的独立值.
	 */
	@Override
	public MethodInvocation invocableClone(Object... arguments) {
		// 强制初始化用户属性 Map, 用于在克隆中使用共享Map引用.
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<String, Object>();
		}

		// 创建MethodInvocation克隆.
		try {
			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
			clone.arguments = arguments;
			return clone;
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(
					"Should be able to clone object of type [" + getClass() + "]: " + ex);
		}
	}


	@Override
	public void setUserAttribute(String key, Object value) {
		if (value != null) {
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<String, Object>();
			}
			this.userAttributes.put(key, value);
		}
		else {
			if (this.userAttributes != null) {
				this.userAttributes.remove(key);
			}
		}
	}

	@Override
	public Object getUserAttribute(String key) {
		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
	}

	/**
	 * 返回与此调用关联的用户属性.
	 * 此方法提供ThreadLocal的调用绑定替代方法.
	 * <p>此映射是延迟初始化的，不在AOP框架本身中使用.
	 * 
	 * @return 与此调用关联的任何用户属性 (never {@code null})
	 */
	public Map<String, Object> getUserAttributes() {
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<String, Object>();
		}
		return this.userAttributes;
	}


	@Override
	public String toString() {
		// 不要在目标上执行toString, 它可能是代理的.
		StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
		sb.append(this.method).append("; ");
		if (this.target == null) {
			sb.append("target is null");
		}
		else {
			sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
		}
		return sb.toString();
	}
}
