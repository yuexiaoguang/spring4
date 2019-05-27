package org.springframework.remoting.rmi;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean}用于RMI代理, 支持传统的RMI服务和RMI调用器.
 * 使用指定的服务接口公开代理服务以用作bean引用.
 * 代理会在远程调用失败时, 抛出Spring未受检的RemoteAccessException, 而不是RMI的RemoteException.
 *
 * <p>服务URL必须是有效的RMI URL, 例如"rmi://localhost:1099/myservice".
 * RMI调用器在RmiInvocationHandler级别工作, 对任何服务使用相同的调用程序stub.
 * 服务接口不必扩展{@code java.rmi.Remote}或抛出{@code java.rmi.RemoteException}.
 * 当然, in和out参数必须是可序列化的.
 *
 * <p>对于传统的RMI服务, 此代理工厂通常与RMI服务接口一起使用.
 * 或者, 此工厂还可以使用匹配的非RMI业务接口代理远程RMI服务,
 * i.e. 一个镜像RMI服务方法但不声明RemoteExceptions的接口.
 * 在后一种情况下, RMI stub引发的RemoteExceptions将自动转换为Spring未受检的RemoteAccessException.
 *
 * <p>与Hessian和Burlap相比, RMI的主要优势在于序列化.
 * 实际上, 任何可序列化的Java对象都可以毫不费力地传输.
 * Hessian和Burlap有自己的 (反)序列化机制, 但是基于HTTP, 因此比RMI更容易设置.
 * 或者, 考虑Spring的HTTP调用器将Java序列化与基于HTTP的传输相结合.
 */
public class RmiProxyFactoryBean extends RmiClientInterceptor implements FactoryBean<Object>, BeanClassLoaderAware {

	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (getServiceInterface() == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(getBeanClassLoader());
	}


	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
