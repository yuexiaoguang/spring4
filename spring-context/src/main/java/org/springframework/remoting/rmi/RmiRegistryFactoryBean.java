package org.springframework.remoting.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link FactoryBean}找到{@link java.rmi.registry.Registry}, 并公开它为bean引用.
 * 如果不存在, 也可以动态创建本地RMI注册表.
 *
 * <p>可用于设置实际的Registry对象, 并将其传递给需要使用RMI的应用程序对象.
 * 需要使用RMI的此类对象的一个​​示例是Spring的{@link RmiServiceExporter},
 * 它可以使用传入的Registry引用, 也可以按照本地属性和默认值的规定回退到注册表.
 *
 * <p>还可以在给定端口上强制创建本地RMI注册表, 例如JMX连接器.
 * 如果与{@link org.springframework.jmx.support.ConnectorServerFactoryBean}一起使用,
 * 建议将连接器定义 (ConnectorServerFactoryBean) 标记为"依赖"注册表定义 (RmiRegistryFactoryBean),
 * 保证首先启动注册表.
 *
 * <p>Note: 该类的实现镜像{@link RmiServiceExporter}中的相应逻辑, 并提供相同的自定义钩子.
 * 为方便起见, RmiServiceExporter实现了自己的注册表查找:
 * 简单地依赖注册表默认值是很常见的.
 */
public class RmiRegistryFactoryBean implements FactoryBean<Registry>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private String host;

	private int port = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private boolean alwaysCreate = false;

	private boolean created = false;


	/**
	 * 为导出的RMI服务设置注册表的主机, i.e. {@code rmi://HOST:port/name}
	 * <p>默认 localhost.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 返回导出的RMI服务的注册表主机.
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 为导出的RMI服务设置注册表的端口, i.e. {@code rmi://host:PORT/name}
	 * <p>默认{@code Registry.REGISTRY_PORT} (1099).
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 返回导出的RMI服务的注册表端口.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置用于RMI注册表的自定义RMI客户端套接字工厂.
	 * <p>如果给定的对象也实现了{@code java.rmi.server.RMIServerSocketFactory}, 它也会自动注册为服务器套接字工厂.
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * 设置用于RMI注册表的自定义RMI服务器套接字工厂.
	 * <p>仅在客户端套接字工厂未实现{@code java.rmi.server.RMIServerSocketFactory}时才需要指定.
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * 设置是否始终在进程中创建注册表, 而不是尝试在指定端口上查找现有注册表.
	 * <p>默认"false".
	 * 将此标志切换为"true", 在任何情况下创建新注册表时, 避免查找现有注册表的开销.
	 */
	public void setAlwaysCreate(boolean alwaysCreate) {
		this.alwaysCreate = alwaysCreate;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		// Check socket factories for registry.
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// Fetch RMI registry to expose.
		this.registry = getRegistry(this.host, this.port, this.clientSocketFactory, this.serverSocketFactory);
	}


	/**
	 * 查找或创建RMI注册表.
	 * 
	 * @param registryHost 要使用的注册表主机 (如果指定了此项, 则不会隐式创建RMI注册表)
	 * @param registryPort 要使用的注册表端口
	 * @param clientSocketFactory 注册表的RMI客户端套接字工厂
	 * @param serverSocketFactory 注册表的RMI服务器套接字工厂
	 * 
	 * @return RMI注册表
	 * @throws java.rmi.RemoteException 如果无法找到或创建注册表
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
			if (this.alwaysCreate) {
				logger.info("Creating new RMI registry");
				this.created = true;
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
					this.created = true;
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
		if (this.alwaysCreate) {
			logger.info("Creating new RMI registry");
			this.created = true;
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
				this.created = true;
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


	@Override
	public Registry getObject() throws Exception {
		return this.registry;
	}

	@Override
	public Class<? extends Registry> getObjectType() {
		return (this.registry != null ? this.registry.getClass() : Registry.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 如果该bean实际创建了一个注册表, 则在bean工厂关闭时取消导出RMI注册表.
	 */
	@Override
	public void destroy() throws RemoteException {
		if (this.created) {
			logger.info("Unexporting RMI registry");
			UnicastRemoteObject.unexportObject(this.registry, true);
		}
	}
}
