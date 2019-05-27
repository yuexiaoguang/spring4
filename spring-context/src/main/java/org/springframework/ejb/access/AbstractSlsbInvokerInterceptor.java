package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.jndi.JndiObjectLocator;

/**
 * AOP拦截器的基类调用本地或远程无状态的会话Bean.
 * 专为EJB 2.x设计, 但也适用于EJB 3会话Bean.
 *
 * <p>这样的拦截器必须是增强链中的最后一个拦截器.
 * 在这种情况下, 没有直接的目标对象:
 * 调用以特殊方式处理, 在通过EJB主目录检索的EJB实例上执行.
 */
public abstract class AbstractSlsbInvokerInterceptor extends JndiObjectLocator
		implements MethodInterceptor {

	private boolean lookupHomeOnStartup = true;

	private boolean cacheHome = true;

	private boolean exposeAccessContext = false;

	/**
	 * EJB的主对象, 可能被缓存.
	 * 类型必须是Object, 因为它可以是EJBHome或EJBLocalHome.
	 */
	private Object cachedHome;

	/**
	 * EJB 主目录上需要的无参数 create()方法, 可能是缓存的.
	 */
	private Method createMethod;

	private final Object homeMonitor = new Object();


	/**
	 * 设置是否在启动时查找EJB主对象.
	 * 默认 "true".
	 * <p>可以关闭以允许EJB服务器延迟启动.
	 * 在这种情况下, EJB主对象将在首次访问时获取.
	 */
	public void setLookupHomeOnStartup(boolean lookupHomeOnStartup) {
		this.lookupHomeOnStartup = lookupHomeOnStartup;
	}

	/**
	 * 设置是否在找到EJB主对象后对其进行缓存.
	 * 默认 "true".
	 * <p>可以关闭以允许EJB服务器的热重启.
	 * 在这种情况下, 将为每次调用获取EJB主对象.
	 */
	public void setCacheHome(boolean cacheHome) {
		this.cacheHome = cacheHome;
	}

	/**
	 * 设置是否为所有对目标EJB的访问公开JNDI环境上下文, i.e. 对于公开的对象引用上的所有方法调用.
	 * <p>默认 "false", i.e. 仅公开用于对象查找的JNDI上下文.
	 * 将此标志切换为 "true", 以便为每个EJB调用公开JNDI环境 (包括授权上下文), 因为WebLogic对具有授权要求的EJB的需要.
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}


	/**
	 * 如有必要, 在启动时获取EJB home.
	 */
	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.lookupHomeOnStartup) {
			// 查找EJB home并创建方法
			refreshHome();
		}
	}

	/**
	 * 如果适用, 刷新缓存的主对象.
	 * 还将create方法缓存在home对象上.
	 * 
	 * @throws NamingException 如果由JNDI查找抛出
	 */
	protected void refreshHome() throws NamingException {
		synchronized (this.homeMonitor) {
			Object home = lookup();
			if (this.cacheHome) {
				this.cachedHome = home;
				this.createMethod = getCreateMethod(home);
			}
		}
	}

	/**
	 * 确定给定EJB主对象的create方法.
	 * 
	 * @param home EJB主对象
	 * 
	 * @return create方法
	 * @throws EjbAccessException 如果无法检索该方法
	 */
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		try {
			// 缓存必须在home接口上声明的EJB create() 方法.
			return home.getClass().getMethod("create");
		}
		catch (NoSuchMethodException ex) {
			throw new EjbAccessException("EJB home [" + home + "] has no no-arg create() method");
		}
	}

	/**
	 * 返回要使用的EJB主对象. 每次调用时调用.
	 * <p>默认实现返回在初始化时创建的home; 否则, 它调用lookup来获取每个调用的新代理.
	 * <p>可以在子类中重写, 例如在重新创建它之前将主对象缓存一段给定的时间, 或者测试主对象是否仍然存活.
	 * 
	 * @return 用于调用的EJB主对象
	 * @throws NamingException 如果代理创建失败
	 */
	protected Object getHome() throws NamingException {
		if (!this.cacheHome || (this.lookupHomeOnStartup && !isHomeRefreshable())) {
			return (this.cachedHome != null ? this.cachedHome : lookup());
		}
		else {
			synchronized (this.homeMonitor) {
				if (this.cachedHome == null) {
					this.cachedHome = lookup();
					this.createMethod = getCreateMethod(this.cachedHome);
				}
				return this.cachedHome;
			}
		}
	}

	/**
	 * 返回缓存的EJB主对象是否可能需要按需刷新. 默认 "false".
	 */
	protected boolean isHomeRefreshable() {
		return false;
	}


	/**
	 * 如有必要, 准备线程上下文, 并委托给 {@link #invokeInContext}.
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Context ctx = (this.exposeAccessContext ? getJndiTemplate().getContext() : null);
		try {
			return invokeInContext(invocation);
		}
		finally {
			getJndiTemplate().releaseContext(ctx);
		}
	}

	/**
	 * 在相应准备的线程上下文中, 在当前EJB主目录上执行给定的调用.
	 * 由子类实现的模板方法.
	 * 
	 * @param invocation AOP方法调用
	 * 
	 * @return 调用结果
	 * @throws Throwable 在调用失败的情况下
	 */
	protected abstract Object invokeInContext(MethodInvocation invocation) throws Throwable;


	/**
	 * 在缓存的EJB主对象上调用 {@code create()}方法.
	 * 
	 * @return 新的EJBObject 或 EJBLocalObject
	 * @throws NamingException 如果被JNDI抛出
	 * @throws InvocationTargetException 如果由create方法抛出
	 */
	protected Object create() throws NamingException, InvocationTargetException {
		try {
			Object home = getHome();
			Method createMethodToUse = this.createMethod;
			if (createMethodToUse == null) {
				createMethodToUse = getCreateMethod(home);
			}
			if (createMethodToUse == null) {
				return home;
			}
			// 在EJB主对象上调用create() 方法.
			return createMethodToUse.invoke(home, (Object[]) null);
		}
		catch (IllegalAccessException ex) {
			throw new EjbAccessException("Could not access EJB home create() method", ex);
		}
	}

}
