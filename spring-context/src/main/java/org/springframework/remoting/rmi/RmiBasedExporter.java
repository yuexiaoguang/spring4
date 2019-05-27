package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;

/**
 * 基于RMI的远程导出器的超类.
 * 提供使用RmiInvocationWrapper自动包装给定普通Java服务对象的工具, 公开{@link RmiInvocationHandler}远程接口.
 *
 * <p>使用RMI调用器机制, RMI通信在{@link RmiInvocationHandler}级别运行, 共享用于任意数量服务的公共调用器stub.
 * 服务接口不需要扩展{@code java.rmi.Remote}或在所有服务方法上声明{@code java.rmi.RemoteException}.
 * 但是, in和out参数仍然必须是可序列化的.
 */
public abstract class RmiBasedExporter extends RemoteInvocationBasedExporter {

	/**
	 * 确定要导出的对象: 服务对象本身, 或非RMI服务对象时的RmiInvocationWrapper.
	 * 
	 * @return 要导出的RMI对象
	 */
	protected Remote getObjectToExport() {
		// determine remote object
		if (getService() instanceof Remote &&
				(getServiceInterface() == null || Remote.class.isAssignableFrom(getServiceInterface()))) {
			// conventional RMI service
			return (Remote) getService();
		}
		else {
			// RMI invoker
			if (logger.isDebugEnabled()) {
				logger.debug("RMI service [" + getService() + "] is an RMI invoker");
			}
			return new RmiInvocationWrapper(getProxyForService(), this);
		}
	}

	/**
	 * 这里重新定义为RmiInvocationWrapper可见.
	 * 只需委托给相应的超类方法.
	 */
	@Override
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		return super.invoke(invocation, targetObject);
	}

}
