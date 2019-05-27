package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.springframework.remoting.support.RemoteInvocation;

/**
 * 服务器上的RMI调用处理器实例的接口, 包装导出的服务.
 * 客户端使用实现此接口的stub来访问此类服务.
 *
 * <p>这是一个SPI接口, 不能由应用程序直接使用.
 */
public interface RmiInvocationHandler extends Remote {

	/**
	 * 返回此调用器操作的目标接口的名称.
	 * 
	 * @return 目标接口的名称, 或{@code null}
	 * @throws RemoteException 在通讯错误的情况下
	 */
	public String getTargetInterfaceName() throws RemoteException;

	/**
	 * 将给定的调用应用于目标对象.
	 * <p>由
	 * {@link RmiClientInterceptor#doInvoke(org.aopalliance.intercept.MethodInvocation, RmiInvocationHandler)}调用.
	 * 
	 * @param invocation 封装调用参数的对象
	 * 
	 * @return 调用的方法返回的对象
	 * @throws RemoteException 在通讯错误的情况下
	 * @throws NoSuchMethodException 如果方法名称无法解析
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 */
	public Object invoke(RemoteInvocation invocation)
			throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException;

}
