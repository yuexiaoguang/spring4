package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.SystemException;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiObjectLocator;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.util.ReflectionUtils;

/**
 * 用于从JNDI访问RMI服务的{@link org.aopalliance.intercept.MethodInterceptor}.
 * 通常用于 RMI-IIOP (CORBA), 但也可用于EJB主对象 (例如, a Stateful Session Bean home).
 * 与普通的JNDI查找相比, 此访问器还通过PortableRemoteObject执行缩小.
 *
 * <p>对于传统的RMI服务, 此调用器通常与RMI服务接口一起使用.
 * 或者, 此调用程序还可以使用匹配的非RMI业务接口代理远程RMI服务,
 * i.e. 一个镜像RMI服务方法但不声明RemoteExceptions的接口.
 * 在后一种情况下, RMI stub引发的RemoteException将自动转换为Spring未受检的RemoteAccessException.
 *
 * <p>JNDI环境可以指定为"jndiEnvironment"属性, 或者在{@code jndi.properties}文件中配置或作为系统属性配置.
 * For example:
 *
 * <pre class="code">&lt;property name="jndiEnvironment"&gt;
 * 	 &lt;props>
 *		 &lt;prop key="java.naming.factory.initial"&gt;com.sun.jndi.cosnaming.CNCtxFactory&lt;/prop&gt;
 *		 &lt;prop key="java.naming.provider.url"&gt;iiop://localhost:1050&lt;/prop&gt;
 *	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 */
public class JndiRmiClientInterceptor extends JndiObjectLocator implements MethodInterceptor, InitializingBean {

	private Class<?> serviceInterface;

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();

	private boolean lookupStubOnStartup = true;

	private boolean cacheStub = true;

	private boolean refreshStubOnConnectFailure = false;

	private boolean exposeAccessContext = false;

	private Object cachedStub;

	private final Object stubMonitor = new Object();


	/**
	 * 设置要访问的服务的接口.
	 * 该接口必须适用于特定服务和远程工具.
	 * <p>通常需要能够创建合适的服务代理, 但如果查找返回类型的stub, 也可以是可选的.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回要访问的服务的接口.
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * 设置用于此访问器的RemoteInvocationFactory.
	 * 默认是{@link DefaultRemoteInvocationFactory}.
	 * <p>自定义调用工厂可以向调用添加更多上下文信息, 例如用户凭据.
	 */
	public void setRemoteInvocationFactory(RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory = remoteInvocationFactory;
	}

	/**
	 * 返回此访问器使用的RemoteInvocationFactory.
	 */
	public RemoteInvocationFactory getRemoteInvocationFactory() {
		return this.remoteInvocationFactory;
	}

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
	 * 默认 "true".
	 * <p>可以关闭以允许热重启RMI服务器.
	 * 在这种情况下, 将为每次调用获取RMI stub.
	 */
	public void setCacheStub(boolean cacheStub) {
		this.cacheStub = cacheStub;
	}

	/**
	 * 设置是否在连接失败时刷新RMI stub.
	 * 默认 "false".
	 * <p>可以打开以允许热重启RMI服务器.
	 * 如果缓存的RMI存根引发RMI异常, 指示远程连接失败, 则将获取新的代理并重试调用.
	 */
	public void setRefreshStubOnConnectFailure(boolean refreshStubOnConnectFailure) {
		this.refreshStubOnConnectFailure = refreshStubOnConnectFailure;
	}

	/**
	 * 设置是否为所有对目标RMI stub的访问公开JNDI环境上下文, i.e. 对于公开的对象引用上的所有方法调用.
	 * <p>默认 "false", i.e. 仅公开用于对象查找的JNDI上下文.
	 * 将此标志切换为 "true", 以便为每个RMI调用公开JNDI环境 (包括授权上下文), 这是WebLogic对具有授权要求的RMI stub的需要.
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}


	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 如有必要, 在启动时获取RMI stub.
	 * 
	 * @throws RemoteLookupFailureException 如果RMI stub创建失败
	 */
	public void prepare() throws RemoteLookupFailureException {
		// 初始化时缓存RMI stub?
		if (this.lookupStubOnStartup) {
			Object remoteObj = lookupStub();
			if (logger.isDebugEnabled()) {
				if (remoteObj instanceof RmiInvocationHandler) {
					logger.debug("JNDI RMI object [" + getJndiName() + "] is an RMI invoker");
				}
				else if (getServiceInterface() != null) {
					boolean isImpl = getServiceInterface().isInstance(remoteObj);
					logger.debug("Using service interface [" + getServiceInterface().getName() +
							"] for JNDI RMI object [" + getJndiName() + "] - " +
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
	 * <p>如果"cacheStub"为"true", 则拦截器初始化时调用; 或者每次通过{@link #getStub()}调用时调用.
	 * <p>默认实现从JNDI环境中检索服务. 这可以在子类中重写.
	 * 
	 * @return 存储在此拦截器中的RMI stub
	 * @throws RemoteLookupFailureException 如果RMI stub创建失败
	 */
	protected Object lookupStub() throws RemoteLookupFailureException {
		try {
			Object stub = lookup();
			if (getServiceInterface() != null && !(stub instanceof RmiInvocationHandler)) {
				try {
					stub = PortableRemoteObject.narrow(stub, getServiceInterface());
				}
				catch (ClassCastException ex) {
					throw new RemoteLookupFailureException(
							"Could not narrow RMI stub to service interface [" + getServiceInterface().getName() + "]", ex);
				}
			}
			return stub;
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("JNDI lookup for RMI service [" + getJndiName() + "] failed", ex);
		}
	}

	/**
	 * 返回要使用的RMI stub. 每次调用时调用.
	 * <p>默认实现返回在初始化时创建的 stub.
	 * 否则, 它会调用{@link #lookupStub}来为每次调用获取一个新的stub.
	 * 这可以在子类中重写, 例如, 为了在重新创建stub之前将stub缓存一段给定的时间, 或者测试stub是否仍然存在.
	 * 
	 * @return 用于调用的RMI stub
	 * @throws NamingException 如果stub创建失败
	 * @throws RemoteLookupFailureException 如果RMI stub创建失败
	 */
	protected Object getStub() throws NamingException, RemoteLookupFailureException {
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
	 * 获取RMI stub并委托给 {@link #doInvoke}.
	 * 如果配置为在连接失败时刷新, 它将在相应的RMI异常上调用 {@link #refreshAndRetry}.
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object stub;
		try {
			stub = getStub();
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("JNDI lookup for RMI service [" + getJndiName() + "] failed", ex);
		}

		Context ctx = (this.exposeAccessContext ? getJndiTemplate().getContext() : null);
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
		catch (SystemException ex) {
			if (isConnectFailure(ex)) {
				return handleRemoteConnectFailure(invocation, ex);
			}
			else {
				throw ex;
			}
		}
		finally {
			getJndiTemplate().releaseContext(ctx);
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
	 * 确定给定的CORBA异常是否表示连接失败.
	 * <p>默认实现检查CORBA的 {@link org.omg.CORBA.OBJECT_NOT_EXIST}异常.
	 * 
	 * @param ex 要检查的RMI异常
	 * 
	 * @return 是否应将异常视为连接失败
	 */
	protected boolean isConnectFailure(SystemException ex) {
		return (ex instanceof OBJECT_NOT_EXIST);
	}

	/**
	 * 刷新stub, 并在必要时重试远程调用.
	 * <p>如果未配置为在连接失败时刷新, 则此方法只是重新抛出原始异常.
	 * 
	 * @param invocation 失败的调用
	 * @param ex 远程调用引发的异常
	 * 
	 * @return 新调用的结果值, 如果成功
	 * @throws Throwable 新调用引发的异常, 如果还是失败.
	 */
	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshStubOnConnectFailure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not connect to RMI service [" + getJndiName() + "] - retrying", ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn("Could not connect to RMI service [" + getJndiName() + "] - retrying");
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
		Object freshStub;
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
	protected Object doInvoke(MethodInvocation invocation, Object stub) throws Throwable {
		if (stub instanceof RmiInvocationHandler) {
			// RMI invoker
			try {
				return doInvoke(invocation, (RmiInvocationHandler) stub);
			}
			catch (RemoteException ex) {
				throw convertRmiAccessException(ex, invocation.getMethod());
			}
			catch (SystemException ex) {
				throw convertCorbaAccessException(ex, invocation.getMethod());
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			catch (Throwable ex) {
				throw new RemoteInvocationFailureException("Invocation of method [" + invocation.getMethod() +
						"] failed in RMI service [" + getJndiName() + "]", ex);
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
					throw convertRmiAccessException((RemoteException) targetEx, invocation.getMethod());
				}
				else if (targetEx instanceof SystemException) {
					throw convertCorbaAccessException((SystemException) targetEx, invocation.getMethod());
				}
				else {
					throw targetEx;
				}
			}
		}
	}

	/**
	 * 将给定的AOP方法调用应用于给定的{@link RmiInvocationHandler}.
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
			return "RMI invoker proxy for service URL [" + getJndiName() + "]";
		}

		return invocationHandler.invoke(createRemoteInvocation(methodInvocation));
	}

	/**
	 * 为给定的AOP方法调用创建一个新的RemoteInvocation对象.
	 * <p>默认实现委托给配置的{@link #setRemoteInvocationFactory RemoteInvocationFactory}.
	 * 这可以在子类中重写, 以便提供自定义RemoteInvocation子类, 包含其他调用参数 (e.g. 用户凭据).
	 * <p>请注意, 最好将自定义RemoteInvocationFactory构建为可重用策略, 而不是覆盖此方法.
	 * 
	 * @param methodInvocation 当前的AOP方法调用
	 * 
	 * @return RemoteInvocation对象
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return getRemoteInvocationFactory().createRemoteInvocation(methodInvocation);
	}

	/**
	 * 如果方法签名未声明RemoteException, 则转换在远程访问Spring的RemoteAccessException期间发生的给定RMI RemoteException.
	 * 否则, 返回原始的RemoteException.
	 * 
	 * @param method 被调用的方法
	 * @param ex 发生的RemoteException
	 * 
	 * @return 抛出给调用者的异常
	 */
	private Exception convertRmiAccessException(RemoteException ex, Method method) {
		return RmiClientInterceptorUtils.convertRmiAccessException(method, ex, isConnectFailure(ex), getJndiName());
	}

	/**
	 * 如果方法签名未声明RemoteException, 则转换在远程访问Spring的RemoteAccessException期间发生的给定CORBA SystemException.
	 * 否则, 返回包含在RemoteException中的SystemException.
	 * 
	 * @param method 被调用的方法
	 * @param ex 发生的RemoteException
	 * 
	 * @return 抛出给调用者的异常
	 */
	private Exception convertCorbaAccessException(SystemException ex, Method method) {
		if (ReflectionUtils.declaresException(method, RemoteException.class)) {
			// 传统的RMI服务: 在标准的RemoteExceptions中包装CORBA异常.
			return new RemoteException("Failed to access CORBA service [" + getJndiName() + "]", ex);
		}
		else {
			if (isConnectFailure(ex)) {
				return new RemoteConnectFailureException("Could not connect to CORBA service [" + getJndiName() + "]", ex);
			}
			else {
				return new RemoteAccessException("Could not access CORBA service [" + getJndiName() + "]", ex);
			}
		}
	}

}
