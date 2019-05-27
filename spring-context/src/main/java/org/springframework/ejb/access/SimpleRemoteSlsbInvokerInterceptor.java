package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBObject;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.rmi.RmiClientInterceptorUtils;

/**
 * 远程无状态的会话Bean的基础调用器.
 * 专为EJB 2.x设计, 但也适用于EJB 3会话Bean.
 *
 * <p>为每次调用"创建"一个新的EJB实例, 或者为所有调用缓存会话bean实例 (see {@link #setCacheSessionBean}).
 * 有关如何指定目标EJB的JNDI位置的信息, 请参阅{@link org.springframework.jndi.JndiObjectLocator}.
 *
 * <p>在bean容器中, 此类通常最适合用作单例.
 * 但是, 如果该bean容器预先实例化单例 (就像 XML ApplicationContext变体一样),
 * 如果在EJB容器加载目标EJB之前加载bean容器, 则可能会出现问题.
 * 这是因为默认情况下, JNDI查找将在此类的init方法中执行并缓存, 但EJB尚未绑定到目标位置.
 * 最好的解决方案是将lookupHomeOnStartup属性设置为false, 在这种情况下, 首次访问EJB时将获取home.
 * (出于向后兼容性原因, 此标志仅在默认情况下为true).
 *
 * <p>此调用程序通常与RMI业务接口一起使用, 该接口充当EJB组件接口的超级接口.
 * 或者, 此调用器还可以使用匹配的非RMI业务接口代理远程SLSB, i.e. 一个镜像EJB业务方法但不声明RemoteExceptions的接口.
 * 在后一种情况下, EJB stub抛出的RemoteException将自动转换为Spring未受检的RemoteAccessException.
 */
public class SimpleRemoteSlsbInvokerInterceptor extends AbstractRemoteSlsbInvokerInterceptor
		implements DisposableBean {

	private boolean cacheSessionBean = false;

	private Object beanInstance;

	private final Object beanInstanceMonitor = new Object();


	/**
	 * 设置是否缓存实际的会话bean对象.
	 * <p>默认情况下为标准EJB合规性.
	 * 打开此标志以优化服务器的会话Bean访问, 已知该服务器允许缓存实际会话Bean对象.
	 */
	public void setCacheSessionBean(boolean cacheSessionBean) {
		this.cacheSessionBean = cacheSessionBean;
	}


	/**
	 * 此实现为每次调用 "创建" 一个新的EJB实例.
	 * 可以为自定义调用策略重写.
	 * <p>或者, 覆盖{@link #getSessionBeanInstance} 和 {@link #releaseSessionBeanInstance}以更改EJB实例创建,
	 * 例如, 保存单个共享的EJB组件实例.
	 */
	@Override
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		Object ejb = null;
		try {
			ejb = getSessionBeanInstance();
			return RmiClientInterceptorUtils.invokeRemoteMethod(invocation, ejb);
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("Failed to locate remote EJB [" + getJndiName() + "]", ex);
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (targetEx instanceof RemoteException) {
				RemoteException rex = (RemoteException) targetEx;
				throw RmiClientInterceptorUtils.convertRmiAccessException(
					invocation.getMethod(), rex, isConnectFailure(rex), getJndiName());
			}
			else if (targetEx instanceof CreateException) {
				throw RmiClientInterceptorUtils.convertRmiAccessException(
					invocation.getMethod(), targetEx, "Could not create remote EJB [" + getJndiName() + "]");
			}
			throw targetEx;
		}
		finally {
			if (ejb instanceof EJBObject) {
				releaseSessionBeanInstance((EJBObject) ejb);
			}
		}
	}

	/**
	 * 返回用于委托调用的EJB组件实例.
	 * <p>默认实现委托给 {@link #newSessionBeanInstance}.
	 * 
	 * @return EJB组件实例
	 * @throws NamingException 如果被JNDI抛出
	 * @throws InvocationTargetException 如果由create方法抛出
	 */
	protected Object getSessionBeanInstance() throws NamingException, InvocationTargetException {
		if (this.cacheSessionBean) {
			synchronized (this.beanInstanceMonitor) {
				if (this.beanInstance == null) {
					this.beanInstance = newSessionBeanInstance();
				}
				return this.beanInstance;
			}
		}
		else {
			return newSessionBeanInstance();
		}
	}

	/**
	 * 释放给定的EJB实例.
	 * <p>默认实现委托给 {@link #removeSessionBeanInstance}.
	 * 
	 * @param ejb 要释放的EJB组件实例
	 */
	protected void releaseSessionBeanInstance(EJBObject ejb) {
		if (!this.cacheSessionBean) {
			removeSessionBeanInstance(ejb);
		}
	}

	/**
	 * 重置缓存的会话Bean实例.
	 */
	@Override
	protected void refreshHome() throws NamingException {
		super.refreshHome();
		if (this.cacheSessionBean) {
			synchronized (this.beanInstanceMonitor) {
				this.beanInstance = null;
			}
		}
	}

	/**
	 * 删除缓存的会话bean实例.
	 */
	@Override
	public void destroy() {
		if (this.cacheSessionBean) {
			synchronized (this.beanInstanceMonitor) {
				if (this.beanInstance instanceof EJBObject) {
					removeSessionBeanInstance((EJBObject) this.beanInstance);
				}
			}
		}
	}

}
