package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.rmi.RmiClientInterceptorUtils;

/**
 * 代理远程无状态的会话Bean的拦截器的基类.
 * 专为EJB 2.x设计, 但也适用于EJB 3会话Bean.
 *
 * <p>这样的拦截器必须是增强链中的最后一个拦截器.
 * 在这种情况下, 没有目标对象.
 */
public abstract class AbstractRemoteSlsbInvokerInterceptor extends AbstractSlsbInvokerInterceptor {

	private Class<?> homeInterface;

	private boolean refreshHomeOnConnectFailure = false;

	private volatile boolean homeAsComponent = false;



	/**
	 * 在执行返回实际SLSB代理的无参数SLSB {@code create()}调用之前, 设置此调用程序将缩小的home接口.
	 * <p>默认无, 这适用于所有不基于CORBA的J2EE服务器.
	 * 已知普通的{@code javax.ejb.EJBHome}接口足以使WebSphere 5.0 Remote SLSB正常工作.
	 * 在其他服务器上, 可能需要目标SLSB的特定home接口.
	 */
	public void setHomeInterface(Class<?> homeInterface) {
		if (homeInterface != null && !homeInterface.isInterface()) {
			throw new IllegalArgumentException(
					"Home interface class [" + homeInterface.getClass() + "] is not an interface");
		}
		this.homeInterface = homeInterface;
	}

	/**
	 * 设置是否在连接失败时刷新EJB主目录.
	 * 默认 "false".
	 * <p>可以打开以允许EJB服务器的热重启.
	 * 如果缓存的EJB主目录抛出指示远程连接失败的RMI异常, 则将获取一个新的主目录并重试该调用.
	 */
	public void setRefreshHomeOnConnectFailure(boolean refreshHomeOnConnectFailure) {
		this.refreshHomeOnConnectFailure = refreshHomeOnConnectFailure;
	}

	@Override
	protected boolean isHomeRefreshable() {
		return this.refreshHomeOnConnectFailure;
	}


	/**
	 * 如果指定了home接口, 则此重写的查找实现在JNDI查找后执行缩小操作.
	 */
	@Override
	protected Object lookup() throws NamingException {
		Object homeObject = super.lookup();
		if (this.homeInterface != null) {
			try {
				homeObject = PortableRemoteObject.narrow(homeObject, this.homeInterface);
			}
			catch (ClassCastException ex) {
				throw new RemoteLookupFailureException(
						"Could not narrow EJB home stub to home interface [" + this.homeInterface.getName() + "]", ex);
			}
		}
		return homeObject;
	}

	/**
	 * 检查直接充当EJB组件的EJB3样式的主对象.
	 */
	@Override
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		if (this.homeAsComponent) {
			return null;
		}
		if (!(home instanceof EJBHome)) {
			// An EJB3 Session Bean...
			this.homeAsComponent = true;
			return null;
		}
		return super.getCreateMethod(home);
	}


	/**
	 * 获取EJB主对象并委托给{@code doInvoke}.
	 * <p>如果配置为在连接失败时刷新, 它将在相应的RMI异常上调用{@link #refreshAndRetry}.
	 */
	@Override
	public Object invokeInContext(MethodInvocation invocation) throws Throwable {
		try {
			return doInvoke(invocation);
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
	 * <p>默认实现委托给 RmiClientInterceptorUtils.
	 * 
	 * @param ex 要检查的RMI异常
	 * 
	 * @return 是否应将异常视为连接失败
	 */
	protected boolean isConnectFailure(RemoteException ex) {
		return RmiClientInterceptorUtils.isConnectFailure(ex);
	}

	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshHomeOnConnectFailure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not connect to remote EJB [" + getJndiName() + "] - retrying", ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn("Could not connect to remote EJB [" + getJndiName() + "] - retrying");
			}
			return refreshAndRetry(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 刷新EJB主对象并重试给定的调用.
	 * 连接失败时调用.
	 * 
	 * @param invocation AOP方法调用
	 * 
	 * @return 调用结果
	 * @throws Throwable 调用失败
	 */
	protected Object refreshAndRetry(MethodInvocation invocation) throws Throwable {
		try {
			refreshHome();
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("Failed to locate remote EJB [" + getJndiName() + "]", ex);
		}
		return doInvoke(invocation);
	}


	/**
	 * 在当前EJB主目录上执行给定的调用.
	 * 由子类实现的模板方法.
	 * 
	 * @param invocation AOP方法调用
	 * 
	 * @return 调用结果
	 * @throws Throwable 调用失败
	 */
	protected abstract Object doInvoke(MethodInvocation invocation) throws Throwable;


	/**
	 * 返回无状态的会话bean的新实例.
	 * 由具体的远程SLSB调用子类调用.
	 * <p>可以重写以更改算法.
	 * 
	 * @throws NamingException 由JNDI抛出
	 * @throws InvocationTargetException 如果由create方法抛出
	 */
	protected Object newSessionBeanInstance() throws NamingException, InvocationTargetException {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to create reference to remote EJB");
		}
		Object ejbInstance = create();
		if (logger.isDebugEnabled()) {
			logger.debug("Obtained reference to remote EJB: " + ejbInstance);
		}
		return ejbInstance;
	}

	/**
	 * 删除给定的EJB实例.
	 * 由具体的远程SLSB调用子类调用.
	 * 
	 * @param ejb 要删除的EJB实例
	 */
	protected void removeSessionBeanInstance(EJBObject ejb) {
		if (ejb != null && !this.homeAsComponent) {
			try {
				ejb.remove();
			}
			catch (Throwable ex) {
				logger.warn("Could not invoke 'remove' on remote EJB proxy", ex);
			}
		}
	}
}
