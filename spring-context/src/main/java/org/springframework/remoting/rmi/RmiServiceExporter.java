package org.springframework.remoting.rmi;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * RMI导出器, 将指定的服务公开为具有指定名称的RMI对象.
 * 可以通过普通RMI或{@link RmiProxyFactoryBean}访问此类服务.
 * 还支持通过RMI调用器公开任何非RMI服务, 可通过{@link RmiClientInterceptor} / {@link RmiProxyFactoryBean}自动检测此类调用器.
 *
 * <p>使用RMI调用器, RMI通信在{@link RmiInvocationHandler}级别上工作, 对于任何服务只需要一个stub.
 * 服务接口不必在所有方法上扩展{@code java.rmi.Remote}或抛出{@code java.rmi.RemoteException}, 但in和out参数必须是可序列化的.
 *
 * <p>与Hessian和Burlap相比, RMI的主要优势在于序列化.
 * 实际上, 任何可序列化的Java对象都可以毫不费力地传输.
 * Hessian和Burlap有自己的 (反)序列化机制, 但是基于HTTP, 因此比RMI更容易设置.
 * 或者, 考虑Spring的HTTP调用器将Java序列化与基于HTTP的传输相结合.
 *
 * <p>Note: RMI尽最大努力获取完全限定的主机名.
 * 如果无法确定, 它将回退并使用IP地址.
 * 根据您的网络配置, 在某些情况下, 它会将IP解析为环回地址.
 * 确保RMI将使用绑定到正确网络接口的主机名,
 * 应该将{@code java.rmi.server.hostname}属性传递给将使用"-D" JVM参数导出注册表和/或服务的JVM.
 * For example: {@code -Djava.rmi.server.hostname=myserver.com}
 */
public class RmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

	private String serviceName;

	private int servicePort = 0;  // anonymous port

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private String registryHost;

	private int registryPort = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory registryClientSocketFactory;

	private RMIServerSocketFactory registryServerSocketFactory;

	private boolean alwaysCreateRegistry = false;

	private boolean replaceExistingBinding = true;

	private Remote exportedObject;

	private boolean createdRegistry = false;


	/**
	 * 设置导出的RMI服务的名称, i.e. {@code rmi://host:port/NAME}
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * 设置导出的RMI服务将使用的端口.
	 * <p>默认 0 (匿名端口).
	 */
	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}

	/**
	 * 设置自定义RMI客户端套接字工厂以用于导出服务.
	 * <p>如果给定的对象也实现了{@code java.rmi.server.RMIServerSocketFactory}, 它也会自动注册为服务器套接字工厂.
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * 设置自定义RMI服务器套接字工厂以用于导出服务.
	 * <p>仅在客户端套接字工厂未实现{@code java.rmi.server.RMIServerSocketFactory}时才需要指定.
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * 指定RMI注册表以注册导出的服务.
	 * 通常与RmiRegistryFactoryBean结合使用.
	 * <p>或者, 可以在本地指定所有注册表属性.
	 * 然后, 此导出器将尝试找到指定的注册表, 并在适当时自动创建新的本地注册表.
	 * <p>默认是默认端口(1099)上的本地注册表, 必要时即时创建.
	 */
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	/**
	 * 为导出的RMI服务设置注册表的主机, i.e. {@code rmi://HOST:port/name}
	 * <p>默认 localhost.
	 */
	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	/**
	 * 为导出的RMI服务设置注册表的端口, i.e. {@code rmi://host:PORT/name}
	 * <p>默认{@code Registry.REGISTRY_PORT} (1099).
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * 设置自定义RMI客户端套接字工厂以用于RMI注册表.
	 * <p>如果给定的对象也实现了{@code java.rmi.server.RMIServerSocketFactory}, 它也会自动注册为服务器套接字工厂.
	 */
	public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
		this.registryClientSocketFactory = registryClientSocketFactory;
	}

	/**
	 * 设置自定义RMI服务器套接字工厂以用于RMI注册表.
	 * <p>仅在客户端套接字工厂未实现{@code java.rmi.server.RMIServerSocketFactory}时才需要指定.
	 */
	public void setRegistryServerSocketFactory(RMIServerSocketFactory registryServerSocketFactory) {
		this.registryServerSocketFactory = registryServerSocketFactory;
	}

	/**
	 * 设置是否始终在进程中创建注册表, 而不是尝试在指定端口上查找现有注册表.
	 * <p>默认 "false".
	 * 将此标志切换为 "true", 在任何情况下创建新注册表时, 避免查找现有注册表的开销.
	 */
	public void setAlwaysCreateRegistry(boolean alwaysCreateRegistry) {
		this.alwaysCreateRegistry = alwaysCreateRegistry;
	}

	/**
	 * 设置是否替换RMI注册表中的现有绑定,
	 * 也就是说, 如果注册表中存在命名冲突, 是否只是简单地覆盖指定服务的现有绑定.
	 * <p>默认 "true", 假设此导出器的服务名称的现有绑定是先前执行的意外遗留.
	 * 将此切换为 "false" 以使导出器在这种情况下失败, 表明已绑定了RMI对象.
	 */
	public void setReplaceExistingBinding(boolean replaceExistingBinding) {
		this.replaceExistingBinding = replaceExistingBinding;
	}


	@Override
	public void afterPropertiesSet() throws RemoteException {
		prepare();
	}

	/**
	 * 初始化此服务导出器, 将服务注册为RMI对象.
	 * <p>如果不存在, 则在指定端口上创建RMI注册表.
	 * 
	 * @throws RemoteException 如果服务注册失败
	 */
	public void prepare() throws RemoteException {
		checkService();

		if (this.serviceName == null) {
			throw new IllegalArgumentException("Property 'serviceName' is required");
		}

		// Check socket factories for exported object.
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// Check socket factories for RMI registry.
		if (this.registryClientSocketFactory instanceof RMIServerSocketFactory) {
			this.registryServerSocketFactory = (RMIServerSocketFactory) this.registryClientSocketFactory;
		}
		if (this.registryClientSocketFactory == null && this.registryServerSocketFactory != null) {
			throw new IllegalArgumentException(
					"RMIServerSocketFactory without RMIClientSocketFactory for registry not supported");
		}

		this.createdRegistry = false;

		// Determine RMI registry to use.
		if (this.registry == null) {
			this.registry = getRegistry(this.registryHost, this.registryPort,
				this.registryClientSocketFactory, this.registryServerSocketFactory);
			this.createdRegistry = true;
		}

		// Initialize and cache exported object.
		this.exportedObject = getObjectToExport();

		if (logger.isInfoEnabled()) {
			logger.info("Binding service '" + this.serviceName + "' to RMI registry: " + this.registry);
		}

		// Export RMI object.
		if (this.clientSocketFactory != null) {
			UnicastRemoteObject.exportObject(
					this.exportedObject, this.servicePort, this.clientSocketFactory, this.serverSocketFactory);
		}
		else {
			UnicastRemoteObject.exportObject(this.exportedObject, this.servicePort);
		}

		// Bind RMI object to registry.
		try {
			if (this.replaceExistingBinding) {
				this.registry.rebind(this.serviceName, this.exportedObject);
			}
			else {
				this.registry.bind(this.serviceName, this.exportedObject);
			}
		}
		catch (AlreadyBoundException ex) {
			// Already an RMI object bound for the specified service name...
			unexportObjectSilently();
			throw new IllegalStateException(
					"Already an RMI object bound for name '"  + this.serviceName + "': " + ex.toString());
		}
		catch (RemoteException ex) {
			// Registry binding failed: let's unexport the RMI object as well.
			unexportObjectSilently();
			throw ex;
		}
	}


	/**
	 * 查找或创建此导出器的RMI注册表.
	 * 
	 * @param registryHost 要使用的注册表主机 (如果指定了此项, 则不会隐式创建RMI注册表)
	 * @param registryPort 要使用的注册表端口
	 * @param clientSocketFactory 注册表的RMI客户端套接字工厂
	 * @param serverSocketFactory 注册表的RMI服务器套接字工厂
	 * 
	 * @return RMI注册表
	 * @throws RemoteException 如果无法找到或创建注册表
	 */
	protected Registry getRegistry(String registryHost, int registryPort,
			RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (registryHost != null) {
			// Host explicitly specified: only lookup possible.
			if (logger.isInfoEnabled()) {
				logger.info("Looking for RMI registry at port '" + registryPort + "' of host [" + registryHost + "]");
			}
			Registry reg = LocateRegistry.getRegistry(registryHost, registryPort, clientSocketFactory);
			testRegistry(reg);
			return reg;
		}

		else {
			return getRegistry(registryPort, clientSocketFactory, serverSocketFactory);
		}
	}

	/**
	 * 查找或创建RMI注册表.
	 * 
	 * @param registryPort 要使用的注册表端口
	 * @param clientSocketFactory 注册表的RMI客户端套接字工厂
	 * @param serverSocketFactory 注册表的RMI服务器套接字工厂
	 * 
	 * @return RMI注册表
	 * @throws RemoteException 如果无法找到或创建注册表
	 */
	protected Registry getRegistry(
			int registryPort, RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			if (this.alwaysCreateRegistry) {
				logger.info("Creating new RMI registry");
				return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
			}
			if (logger.isInfoEnabled()) {
				logger.info("Looking for RMI registry at port '" + registryPort + "', using custom socket factory");
			}
			synchronized (LocateRegistry.class) {
				try {
					// 检索现有的注册表.
					Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
					testRegistry(reg);
					return reg;
				}
				catch (RemoteException ex) {
					logger.debug("RMI registry access threw exception", ex);
					logger.info("Could not detect RMI registry - creating new one");
					// 假设没有找到注册表 -> create new one.
					return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
				}
			}
		}

		else {
			return getRegistry(registryPort);
		}
	}

	/**
	 * 查找或创建RMI注册表.
	 * 
	 * @param registryPort 要使用的注册表端口
	 * 
	 * @return RMI注册表
	 * @throws RemoteException 如果无法找到或创建注册表
	 */
	protected Registry getRegistry(int registryPort) throws RemoteException {
		if (this.alwaysCreateRegistry) {
			logger.info("Creating new RMI registry");
			return LocateRegistry.createRegistry(registryPort);
		}
		if (logger.isInfoEnabled()) {
			logger.info("Looking for RMI registry at port '" + registryPort + "'");
		}
		synchronized (LocateRegistry.class) {
			try {
				// 检索现有的注册表.
				Registry reg = LocateRegistry.getRegistry(registryPort);
				testRegistry(reg);
				return reg;
			}
			catch (RemoteException ex) {
				logger.debug("RMI registry access threw exception", ex);
				logger.info("Could not detect RMI registry - creating new one");
				// 假设没有找到注册表 -> create new one.
				return LocateRegistry.createRegistry(registryPort);
			}
		}
	}

	/**
	 * 测试给定的RMI注册表, 在其上调用一些操作以检查它是否仍处于活动状态.
	 * <p>默认实现调用{@code Registry.list()}.
	 * 
	 * @param registry 要测试的RMI注册表
	 * 
	 * @throws RemoteException 如果由注册表方法抛出
	 */
	protected void testRegistry(Registry registry) throws RemoteException {
		registry.list();
	}


	/**
	 * 在bean工厂关闭时从注册表解除绑定RMI服务.
	 */
	@Override
	public void destroy() throws RemoteException {
		if (logger.isInfoEnabled()) {
			logger.info("Unbinding RMI service '" + this.serviceName +
					"' from registry" + (this.createdRegistry ? (" at port '" + this.registryPort + "'") : ""));
		}
		try {
			this.registry.unbind(this.serviceName);
		}
		catch (NotBoundException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("RMI service '" + this.serviceName + "' is not bound to registry" +
						(this.createdRegistry ? (" at port '" + this.registryPort + "' anymore") : ""), ex);
			}
		}
		finally {
			unexportObjectSilently();
		}
	}

	/**
	 * 取消导出已注册的RMI对象, 记录出现的任何异常.
	 */
	private void unexportObjectSilently() {
		try {
			UnicastRemoteObject.unexportObject(this.exportedObject, true);
		}
		catch (NoSuchObjectException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("RMI object for service '" + this.serviceName + "' is not exported anymore", ex);
			}
		}
	}
}
