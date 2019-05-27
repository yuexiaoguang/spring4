package org.springframework.remoting.rmi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.support.RemoteInvocationUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}用于访问传统的RMI服务或RMI调用者.
 * 服务URL必须是有效的RMI URL (e.g. "rmi://localhost:1099/myservice").
 *
 * <p>RMI调用器在RmiInvocationHandler级别工作, 对任何服务使用相同的调用程序stub.
 * 服务接口不必扩展{@code java.rmi.Remote}或抛出{@code java.rmi.RemoteException}.
 * Spring的未受检的RemoteAccessException将在远程调用失败时抛出.
 * 当然, in和out参数必须是可序列化的.
 *
 * <p>对于传统的RMI服务, 此调用器通常与RMI服务接口一起使用.
 * 或者, 此调用程序还可以使用匹配的非RMI业务接口代理远程RMI服务,
 * i.e. 一个镜像RMI服务方法但不声明RemoteException的接口.
 * 在后一种情况下, RMI stub引发的RemoteException将自动转换为Spring未受检的RemoteAccessException.
 */
public class RmiClientInterceptor extends RemoteInvocationBasedAccessor
		implements MethodInterceptor {

	private boolean lookupStubOnStartup = true;

	private boolean cacheStub = true;

	private boolean refreshStubOnConnectFailure = false;

	private RMIClientSocketFactory registryClientSocketFactory;

	private Remote cachedStub;

	private final Object stubMonitor = new Object();


	/**
	 * 设置是否在启动时查找RMI stub. 默认"true".
	 * <p>可以关闭以允许RMI服务器延迟启动.
	 * 在这种情况下, 将在首次访问时获取RMI stub.
	 */
	public void setLookupStubOnStartup(boolean lookupStubOnStartup) {
		this.lookupStubOnStartup = lookupStubOnStartup;
	}

	/**
	 * 设置是否在找到RMI stub后缓存它.
	 * 默认"true".
	 * <p>可以关闭以允许热重启RMI服务器.
	 * 在这种情况下, 将为每次调用获取RMI存根.
	 */
	public void setCacheStub(boolean cacheStub) {
		this.cacheStub = cacheStub;
	}

	/**
	 * 设置是否在连接失败时刷新RMI存根.
	 * 默认"false".
	 * <p>可以打开以允许热重启RMI服务器.
	 * 如果缓存的RMI stub引发RMI异常, 指示远程连接失败, 则将获取新的代理并重试调用.
	 */
	public void setRefreshStubOnConnectFailure(boolean refreshStubOnConnectFailure) {
		this.refreshStubOnConnectFailure = refreshStubOnConnectFailure;
	}

	/**
	 * 设置自定义RMI客户端套接字工厂以用于访问RMI注册表.
	 */
	public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
		this.registryClientSocketFactory = registryClientSocketFactory;
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 如有必要, 在启动时获取RMI stub.
	 * 
	 * @throws RemoteLookupFailureException 如果RMI stub创建失败
	 */
	public void prepare() throws RemoteLookupFailureException {
		// Cache RMI stub on initialization?
		if (this.lookupStubOnStartup) {
			Remote remoteObj = lookupStub();
			if (logger.isDebugEnabled()) {
				if (remoteObj instanceof RmiInvocationHandler) {
					logger.debug("RMI stub [" + getServiceUrl() + "] is an RMI invoker");
				}
				else if (getServiceInterface() != null) {
					boolean isImpl = getServiceInterface().isInstance(remoteObj);
					logger.debug("Using service interface [" + getServiceInterface().getName() +
						"] for RMI stub [" + getServiceUrl() + "] - " +
						(!isImpl ? "not " : "") + "directly implemented");
				}
			}
			if (this.cacheStub) {
				this.cachedStub = remoteObj;
			}
		}
	}

	/**
	 * 通常通过查找来创建RMI stub.
	 * <p>如果"cacheStub"为"true", 则调用拦截器初始化;
	 * 或者每次调用{@link #getStub()}时调用.
	 * <p>默认实现通过{@code java.rmi.Naming}查找服务URL. 这可以在子类中重写.
	 * 
	 * @return 存储在此拦截器中的RMI stub
	 * @throws RemoteLookupFailureException 如果RMI stub创建失败
	 */
	protected Remote lookupStub() throws RemoteLookupFailureException {
		try {
			Remote stub = null;
			if (this.registryClientSocketFactory != null) {
				// RMIClientSocketFactory指定用于注册表访问.
				// 不幸的是, 由于RMI API限制, 这意味着我们需要自己解析RMI URL, 并直接执行LocateRegistry.getRegistry / Registry.lookup调用.
				URL url = new URL(null, getServiceUrl(), new DummyURLStreamHandler());
				String protocol = url.getProtocol();
				if (protocol != null && !"rmi".equals(protocol)) {
					throw new MalformedURLException("Invalid URL scheme '" + protocol + "'");
				}
				String host = url.getHost();
				int port = url.getPort();
				String name = url.getPath();
				if (name != null && name.startsWith("/")) {
					name = name.substring(1);
				}
				Registry registry = LocateRegistry.getRegistry(host, port, this.registryClientSocketFactory);
				stub = registry.lookup(name);
			}
			else {
				// 可以继续使用标准RMI查找API...
				stub = Naming.lookup(getServiceUrl());
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Located RMI stub with URL [" + getServiceUrl() + "]");
			}
			return stub;
		}
		catch (MalformedURLException ex) {
			throw new RemoteLookupFailureException("Service URL [" + getServiceUrl() + "] is invalid", ex);
		}
		catch (NotBoundException ex) {
			throw new RemoteLookupFailureException(
					"Could not find RMI service [" + getServiceUrl() + "] in RMI registry", ex);
		}
		catch (RemoteException ex) {
			throw new RemoteLookupFailureException("Lookup of RMI stub failed", ex);
		}
	}

	/**
	 * 返回要使用的RMI stub. 每次调用时调用.
	 * <p>默认实现返回在初始化时创建的 stub.
	 * 否则, 它会调用{@link #lookupStub}来为每次调用获取一个新的 stub.
	 * 这可以在子类中重写, 例如, 为了在重新创建stub之前将stub缓存一段给定的时间, 或者测试stub是否仍然存在.
	 * 
	 * @return 用于调用的RMI stub
	 * @throws RemoteLookupFailureException 如果RMI stub创建失败
	 */
	protected Remote getStub() throws RemoteLookupFailureException {
		if (!this.cacheStub || (this.lookupStubOnStartup && !this.refreshStubOnConnectFailure)) {
			return (this.cachedStub != null ? this.cachedStub : lookupStub());
		}
		else {
			synchronized (this.stubMonitor) {
				if (this.cachedStub == null) {
					this.cachedStub = lookupStub();
				}
				return this.cachedStub;
			}
		}
	}


	/**
	 * 获取RMI stub并委托给{@code doInvoke}.
	 * 如果配置为在连接失败时刷新, 它将在相应的RMI异常上调用{@link #refreshAndRetry}.
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Remote stub = getStub();
		try {
			return doInvoke(invocation, stub);
		}
		catch (RemoteConnectFailureException ex) {
			return handleRemoteConnectFailure(invocation, ex);
		}
		catch (RemoteException ex) {
			if (isConnectFailure(ex)) {
				return handleRemoteConnectFailure(invocation, ex);
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * 确定给定的RMI异常是否表示连接失败.
	 * <p>默认实现委托给{@link RmiClientInterceptorUtils#isConnectFailure}.
	 * 
	 * @param ex 要检查的RMI异常
	 * 
	 * @return 是否应将异常视为连接失败
	 */
	protected boolean isConnectFailure(RemoteException ex) {
		return RmiClientInterceptorUtils.isConnectFailure(ex);
	}

	/**
	 * 刷新stub, 并在必要时重试远程调用.
	 * <p>如果未配置为在连接失败时刷新, 则此方法只是重新抛出原始异常.
	 * 
	 * @param invocation 失败的调用
	 * @param ex 远程调用引发的异常
	 * 
	 * @return 新调用的结果值, 如果成功
	 * @throws Throwable 新调用引发的异常, 如果还是失败
	 */
	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshStubOnConnectFailure) {
			String msg = "Could not connect to RMI service [" + getServiceUrl() + "] - retrying";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn(msg);
			}
			return refreshAndRetry(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 刷新RMI stub并重试给定的调用.
	 * 连接失败时调用.
	 * 
	 * @param invocation AOP方法调用
	 * 
	 * @return 调用结果
	 * @throws Throwable 调用失败
	 */
	protected Object refreshAndRetry(MethodInvocation invocation) throws Throwable {
		Remote freshStub = null;
		synchronized (this.stubMonitor) {
			this.cachedStub = null;
			freshStub = lookupStub();
			if (this.cacheStub) {
				this.cachedStub = freshStub;
			}
		}
		return doInvoke(invocation, freshStub);
	}

	/**
	 * 在给定的RMI stub上执行给定的调用.
	 * 
	 * @param invocation AOP方法调用
	 * @param stub 要调用的RMI stub
	 * 
	 * @return 调用结果
	 * @throws Throwable 调用失败
	 */
	protected Object doInvoke(MethodInvocation invocation, Remote stub) throws Throwable {
		if (stub instanceof RmiInvocationHandler) {
			// RMI invoker
			try {
				return doInvoke(invocation, (RmiInvocationHandler) stub);
			}
			catch (RemoteException ex) {
				throw RmiClientInterceptorUtils.convertRmiAccessException(
					invocation.getMethod(), ex, isConnectFailure(ex), getServiceUrl());
			}
			catch (InvocationTargetException ex) {
				Throwable exToThrow = ex.getTargetException();
				RemoteInvocationUtils.fillInClientStackTraceIfPossible(exToThrow);
				throw exToThrow;
			}
			catch (Throwable ex) {
				throw new RemoteInvocationFailureException("Invocation of method [" + invocation.getMethod() +
						"] failed in RMI service [" + getServiceUrl() + "]", ex);
			}
		}
		else {
			// traditional RMI stub
			try {
				return RmiClientInterceptorUtils.invokeRemoteMethod(invocation, stub);
			}
			catch (InvocationTargetException ex) {
				Throwable targetEx = ex.getTargetException();
				if (targetEx instanceof RemoteException) {
					RemoteException rex = (RemoteException) targetEx;
					throw RmiClientInterceptorUtils.convertRmiAccessException(
							invocation.getMethod(), rex, isConnectFailure(rex), getServiceUrl());
				}
				else {
					throw targetEx;
				}
			}
		}
	}

	/**
	 * 将给定的AOP方法调用应用于给定的 {@link RmiInvocationHandler}.
	 * <p>默认实现委托给{@link #createRemoteInvocation}.
	 * 
	 * @param methodInvocation 当前的AOP方法调用
	 * @param invocationHandler 要调用的RmiInvocationHandler
	 * 
	 * @return 调用结果
	 * @throws RemoteException 通信错误
	 * @throws NoSuchMethodException 如果方法名称无法解析
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 */
	protected Object doInvoke(MethodInvocation methodInvocation, RmiInvocationHandler invocationHandler)
		throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "RMI invoker proxy for service URL [" + getServiceUrl() + "]";
		}

		return invocationHandler.invoke(createRemoteInvocation(methodInvocation));
	}


	/**
	 * 刚刚禁止标准{@code java.net.URL} URLStreamHandler查找的虚拟URLStreamHandler, 以便能够使用标准URL类来解析"rmi:..." URLs.
	 */
	private static class DummyURLStreamHandler extends URLStreamHandler {

		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			throw new UnsupportedOperationException();
		}
	}
}
