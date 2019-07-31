package org.springframework.web.util;

import java.io.Serializable;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Servlet HttpSessionListener, 在创建HttpSession时自动公开会话互斥锁.
 * 要在{@code web.xml}中注册为监听器.
 *
 * <p>会话互斥锁在会话的整个生命周期内保证是同一个对象, 在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下可用.
 * 它用作同步锁定当前会话的安全引用.
 *
 * <p>在许多情况下, HttpSession引用本身也是一个安全的互斥锁, 因为它对于同一个活动逻辑会话始终是相同的对象引用.
 * 但是, 不能在不同的servlet容器中保证这一点; 唯一100% 安全的方式是会话互斥锁.
 */
public class HttpSessionMutexListener implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		event.getSession().setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Mutex());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		event.getSession().removeAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
	}


	/**
	 * 要注册的互斥锁.
	 * 除了要同步的普通对象之外, 不需要任何其他内容.
	 * 应该是可序列化的, 以允许HttpSession持久化.
	 */
	@SuppressWarnings("serial")
	private static class Mutex implements Serializable {
	}

}
