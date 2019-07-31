package org.springframework.web.jsf;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

/**
 * 为给定的JSF {@link FacesContext}检索Spring的根{@link WebApplicationContext}的便捷方法.
 * 这对于从基于JSF的自定义代码访问Spring应用程序上下文非常有用.
 *
 * <p>类似于用于ServletContext的Spring的WebApplicationContextUtils.
 */
public abstract class FacesContextUtils {

	/**
	 * 查找此Web应用程序的根{@link WebApplicationContext},
	 * 通常通过{@link org.springframework.web.context.ContextLoaderListener}加载.
	 * <p>将重新抛出在根上下文启动时发生的异常, 以区分失败的上下文启动和根本没有上下文.
	 * 
	 * @param fc 用于查找Web应用程序上下文的FacesContext
	 * 
	 * @return 此Web应用程序的根WebApplicationContext, 或{@code null}
	 */
	public static WebApplicationContext getWebApplicationContext(FacesContext fc) {
		Assert.notNull(fc, "FacesContext must not be null");
		Object attr = fc.getExternalContext().getApplicationMap().get(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Root context attribute is not of type WebApplicationContext: " + attr);
		}
		return (WebApplicationContext) attr;
	}

	/**
	 * 查找此Web应用程序的根{@link WebApplicationContext},
	 * 通常通过{@link org.springframework.web.context.ContextLoaderListener}加载.
	 * <p>将重新抛出在根上下文启动时发生的异常, 以区分失败的上下文启动和根本没有上下文.
	 * 
	 * @param fc 用于查找Web应用程序上下文的FacesContext
	 * 
	 * @return 此Web应用程序的根WebApplicationContext
	 * @throws IllegalStateException 如果找不到根WebApplicationContext
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(FacesContext fc) throws IllegalStateException {
		WebApplicationContext wac = getWebApplicationContext(fc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}

	/**
	 * 返回给定会话的最佳可用互斥锁: 即, 用于同步给定会话的对象.
	 * <p>返回会话互斥锁属性; 通常, 这意味着需要在{@code web.xml}中定义HttpSessionMutexListener.
	 * 如果未找到互斥锁属性, 则回退到会话引用本身.
	 * <p>会话互斥锁在会话的整个生命周期内保证是同一个对象, 在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下可用.
	 * 它用作同步锁定当前会话的安全引用.
	 * <p>在许多情况下, Session引用本身也是一个安全的互斥锁, 因为它始终是同一个活动逻辑会话的相同对象引用.
	 * 但是, 不能在不同的servlet容器中保证这一点; 唯一100% 安全的方式是会话互斥锁.
	 * 
	 * @param fc 要查找会话互斥锁的FacesContext
	 * 
	 * @return 互斥锁对象 (never {@code null})
	 */
	public static Object getSessionMutex(FacesContext fc) {
		Assert.notNull(fc, "FacesContext must not be null");
		ExternalContext ec = fc.getExternalContext();
		Object mutex = ec.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = ec.getSession(true);
		}
		return mutex;
	}

}
