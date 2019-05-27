package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.util.Assert;

/**
 * {@link RmiInvocationHandler}的服务器端实现.
 * 每个远程对象都存在此类的实例. 由{@link RmiServiceExporter}自动为非RMI服务实现创建.
 *
 * <p>这是一个SPI类, 不能由应用程序直接使用.
 */
class RmiInvocationWrapper implements RmiInvocationHandler {

	private final Object wrappedObject;

	private final RmiBasedExporter rmiExporter;


	/**
	 * @param wrappedObject 用RmiInvocationHandler包装的对象
	 * @param rmiExporter 处理实际的调用的RMI导出器
	 */
	public RmiInvocationWrapper(Object wrappedObject, RmiBasedExporter rmiExporter) {
		Assert.notNull(wrappedObject, "Object to wrap is required");
		Assert.notNull(rmiExporter, "RMI exporter is required");
		this.wrappedObject = wrappedObject;
		this.rmiExporter = rmiExporter;
	}


	/**
	 * 将导出器的服务接口公开为目标接口.
	 */
	@Override
	public String getTargetInterfaceName() {
		Class<?> ifc = this.rmiExporter.getServiceInterface();
		return (ifc != null ? ifc.getName() : null);
	}

	/**
	 * 将实际调用处理委托给RMI导出器.
	 */
	@Override
	public Object invoke(RemoteInvocation invocation)
		throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		return this.rmiExporter.invoke(invocation, this.wrappedObject);
	}

}
