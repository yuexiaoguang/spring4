package org.springframework.remoting.support;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 用于从AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}创建{@link RemoteInvocation}的策略接口.
 *
 * <p>Used by {@link org.springframework.remoting.rmi.RmiClientInterceptor} (for RMI invokers)
 * and by {@link org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor}.
 */
public interface RemoteInvocationFactory {

	/**
	 * 从给定的AOP MethodInvocation创建可序列化的RemoteInvocation对象.
	 * <p>可以实现将自定义上下文信息添加到远程调用, 例如用户凭据.
	 * 
	 * @param methodInvocation 原始的AOP MethodInvocation对象
	 * 
	 * @return RemoteInvocation对象
	 */
	RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation);

}
