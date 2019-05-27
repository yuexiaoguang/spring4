package org.springframework.remoting.support;

import java.lang.reflect.InvocationTargetException;

/**
 * 用于在目标对象上执行{@link RemoteInvocation}的策略接口.
 *
 * <p>Used by {@link org.springframework.remoting.rmi.RmiServiceExporter} (for RMI invokers)
 * and by {@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}.
 */
public interface RemoteInvocationExecutor {

	/**
	 * 对给定的目标对象执行此调用.
	 * 通常在服务器上收到RemoteInvocation时调用.
	 * 
	 * @param invocation the RemoteInvocation
	 * @param targetObject 要调用的目标对象
	 * 
	 * @return 调用结果
	 * @throws NoSuchMethodException 如果方法名称无法解析
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 */
	Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

}
