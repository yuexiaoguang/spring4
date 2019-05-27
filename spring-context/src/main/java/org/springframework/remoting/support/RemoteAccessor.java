package org.springframework.remoting.support;

/**
 * 访问远程服务的类的抽象基类.
 * 提供了"serviceInterface" bean属性.
 *
 * <p>请注意, 正在使用的服务接口将显示一些可远程访问的迹象, 例如它提供的方法调用的粒度.
 * 此外, 它必须具有可序列化的参数等.
 *
 * <p>在远程调用失败的情况下, 访问器应该抛出Spring的通用{@link org.springframework.remoting.RemoteAccessException},
 * 只要服务接口没有声明{@code java.rmi.RemoteException}.
 */
public abstract class RemoteAccessor extends RemotingSupport {

	private Class<?> serviceInterface;


	/**
	 * 设置要访问的服务的接口.
	 * 接口必须适合特定的服务和远程策略.
	 * <p>通常需要能够创建合适的服务代理, 但如果查找返回类型的代理, 也可以是可选的.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回要访问的服务接口.
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

}
