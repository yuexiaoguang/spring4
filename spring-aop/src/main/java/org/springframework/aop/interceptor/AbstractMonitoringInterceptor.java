package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 监控拦截器的基类, 如性能监视器.
 * 提供可配置的 "prefix" 和 "suffix"属性, 有助于对性能监视结果进行分类/分组.
 *
 * <p>在他们的{@link #invokeUnderTrace}实现中, 子类应调用{@link #createInvocationTraceName}方法为给定跟踪创建名称,
 * 包括有关方法调用的信息以及前缀/后缀.
 */
@SuppressWarnings("serial")
public abstract class AbstractMonitoringInterceptor extends AbstractTraceInterceptor {

	private String prefix = "";

	private String suffix = "";

	private boolean logTargetClassInvocation = false;


	/**
	 * 设置将附加到跟踪数据的文本.
	 * <p>Default is none.
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回将附加到跟踪数据的文本.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置将附加到跟踪数据的文本.
	 * <p>Default is none.
	 */
	public void setSuffix(String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回将附加到跟踪数据的文本.
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * 设置是否在目标类上记录调用, 如果适用的话(i.e. 如果该方法实际委托给目标类).
	 * <p>默认 "false", 根据代理接口/类名记录调用.
	 */
	public void setLogTargetClassInvocation(boolean logTargetClassInvocation) {
		this.logTargetClassInvocation = logTargetClassInvocation;
	}


	/**
	 * 为给定的{@code MethodInvocation}创建一个{@code String}名称，该名称可用于跟踪/记录目的.
	 * 此名称由配置的前缀组成, 后跟被调用方法的完全限定名称, 后跟配置的后缀.
	 */
	protected String createInvocationTraceName(MethodInvocation invocation) {
		StringBuilder sb = new StringBuilder(getPrefix());
		Method method = invocation.getMethod();
		Class<?> clazz = method.getDeclaringClass();
		if (this.logTargetClassInvocation && clazz.isInstance(invocation.getThis())) {
			clazz = invocation.getThis().getClass();
		}
		sb.append(clazz.getName());
		sb.append('.').append(method.getName());
		sb.append(getSuffix());
		return sb.toString();
	}
}
