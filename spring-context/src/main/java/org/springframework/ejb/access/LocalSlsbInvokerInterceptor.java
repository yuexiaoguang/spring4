package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 本地无状态会话Bean的调用器.
 * 专为EJB 2.x设计, 但也适用于EJB 3会话Bean.
 *
 * <p>缓存主对象, 因为本地EJB home永远不会过时.
 * 有关如何指定目标EJB的JNDI位置的信息, 请参阅{@link org.springframework.jndi.JndiObjectLocator}.
 *
 * <p>在bean容器中, 此类通常最适合用作单例.
 * 但是, 如果该bean容器预先实例化单例 (就像 XML ApplicationContext变体一样),
 * 如果在EJB容器加载目标EJB之前加载bean容器, 则可能会出现问题.
 * 这是因为默认情况下, JNDI查找将在此类的init方法中执行并缓存, 但EJB尚未绑定到目标位置.
 * 最好的解决方案是将lookupHomeOnStartup属性设置为false, 在这种情况下, 首次访问EJB时将获取home.
 * (出于向后兼容性原因, 此标志仅在默认情况下为true).
 */
public class LocalSlsbInvokerInterceptor extends AbstractSlsbInvokerInterceptor {

	private volatile boolean homeAsComponent = false;


	/**
	 * 此实现为每次调用 "创建" 一个新的EJB实例.
	 * 可以重写以自定义调用策略.
	 * <p>或者, 重写{@link #getSessionBeanInstance} 和 {@link #releaseSessionBeanInstance} 以更改EJB实例创建,
	 * 例如保存单个共享EJB实例.
	 */
	@Override
	public Object invokeInContext(MethodInvocation invocation) throws Throwable {
		Object ejb = null;
		try {
			ejb = getSessionBeanInstance();
			Method method = invocation.getMethod();
			if (method.getDeclaringClass().isInstance(ejb)) {
				// 直接实现
				return method.invoke(ejb, invocation.getArguments());
			}
			else {
				// 非直接实现
				Method ejbMethod = ejb.getClass().getMethod(method.getName(), method.getParameterTypes());
				return ejbMethod.invoke(ejb, invocation.getArguments());
			}
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (logger.isDebugEnabled()) {
				logger.debug("Method of local EJB [" + getJndiName() + "] threw exception", targetEx);
			}
			if (targetEx instanceof CreateException) {
				throw new EjbAccessException("Could not create local EJB [" + getJndiName() + "]", targetEx);
			}
			else {
				throw targetEx;
			}
		}
		catch (NamingException ex) {
			throw new EjbAccessException("Failed to locate local EJB [" + getJndiName() + "]", ex);
		}
		catch (IllegalAccessException ex) {
			throw new EjbAccessException("Could not access method [" + invocation.getMethod().getName() +
				"] of local EJB [" + getJndiName() + "]", ex);
		}
		finally {
			if (ejb instanceof EJBLocalObject) {
				releaseSessionBeanInstance((EJBLocalObject) ejb);
			}
		}
	}

	/**
	 * 检查直接充当EJB组件的EJB3样式的主对象.
	 */
	@Override
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		if (this.homeAsComponent) {
			return null;
		}
		if (!(home instanceof EJBLocalHome)) {
			// An EJB3 Session Bean...
			this.homeAsComponent = true;
			return null;
		}
		return super.getCreateMethod(home);
	}

	/**
	 * 返回一个EJB实例以委托调用.
	 * 默认实现委托给 newSessionBeanInstance.
	 * 
	 * @throws NamingException 如果被JNDI抛出
	 * @throws InvocationTargetException 如果由create方法抛出
	 */
	protected Object getSessionBeanInstance() throws NamingException, InvocationTargetException {
		return newSessionBeanInstance();
	}

	/**
	 * 释放给定的EJB实例.
	 * 默认实现委托给 removeSessionBeanInstance.
	 * 
	 * @param ejb 要释放的EJB实例
	 */
	protected void releaseSessionBeanInstance(EJBLocalObject ejb) {
		removeSessionBeanInstance(ejb);
	}

	/**
	 * 返回无状态的会话bean的新实例.
	 * 可以重写以更改算法.
	 * 
	 * @throws NamingException 如果被JNDI抛出
	 * @throws InvocationTargetException 如果由create方法抛出
	 */
	protected Object newSessionBeanInstance() throws NamingException, InvocationTargetException {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to create reference to local EJB");
		}
		Object ejbInstance = create();
		if (logger.isDebugEnabled()) {
			logger.debug("Obtained reference to local EJB: " + ejbInstance);
		}
		return ejbInstance;
	}

	/**
	 * 删除给定的EJB实例.
	 * 
	 * @param ejb 要删除的EJB实例
	 */
	protected void removeSessionBeanInstance(EJBLocalObject ejb) {
		if (ejb != null && !this.homeAsComponent) {
			try {
				ejb.remove();
			}
			catch (Throwable ex) {
				logger.warn("Could not invoke 'remove' on local EJB proxy", ex);
			}
		}
	}

}
