package org.springframework.orm.hibernate4.support;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate4.SessionFactoryUtils;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet过滤器, 它将Hibernate会话绑定到线程以处理整个请求.
 * 用于"Open Session in View"模式, i.e. 允许在Web视图中延迟加载, 尽管原始事务已经完成.
 *
 * <p>此过滤器通过当前线程使Hibernate会话可用, 该线程将由事务管理器自动检测.
 * 它适用于通过{@link org.springframework.orm.hibernate4.HibernateTransactionManager}进行的服务层事务, 以及非事务性执行 (如果配置正确).
 *
 * <p><b>NOTE</b>: 默认情况下, 此过滤器<i>不</i>刷新Hibernate Session, 刷新模式设置为{@code FlushMode.NEVER}.
 * 它假定与关注刷新的服务层事务结合使用:
 * 在读写事务期间, 活动的事务管理器将临时更改刷新模式为{@code FlushMode.AUTO},
 * 并在每个事务结束时将刷新模式重置为{@code FlushMode.NEVER}.
 *
 * <p><b>WARNING:</b> 将此过滤器应用于现有逻辑可能会导致之前未出现的问题, 通过使用单个Hibernate会话来处理整个请求.
 * 特别是, 持久对象与Hibernate会话的重新关联必须在请求处理的最开始时进行, 以避免与已加载的相同对象的实例发生冲突.
 *
 * <p>在Spring的根Web应用程序上下文中查找SessionFactory.
 * 支持{@code web.xml}中的"sessionFactoryBeanName"过滤器 init-param.
 */
public class OpenSessionInViewFilter extends OncePerRequestFilter {

	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";

	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;


	/**
	 * 设置SessionFactory的bean名称, 以从Spring的根应用程序上下文中获取.
	 * 默认"sessionFactory".
	 */
	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	/**
	 * 返回SessionFactory的bean名称, 以从Spring的根应用程序上下文中获取.
	 */
	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}


	/**
	 * 返回"false", 以便过滤器可以将打开的Hibernate {@code Session}重新绑定到每个异步调度的线程,
	 * 并推迟关闭它直到最后一次异步调度.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回"false", 以便过滤器可以为每个错误调度提供Hibernate {@code Session}.
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		SessionFactory sessionFactory = lookupSessionFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			// 不要修改 Session: 只需设置参与标志即可.
			participate = true;
		}
		else {
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applySessionBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening Hibernate Session in OpenSessionInViewFilter");
				Session session = openSession(sessionFactory);
				SessionHolder sessionHolder = new SessionHolder(session);
				TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);

				AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(sessionFactory, sessionHolder);
				asyncManager.registerCallableInterceptor(key, interceptor);
				asyncManager.registerDeferredResultInterceptor(key, interceptor);
			}
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				SessionHolder sessionHolder =
						(SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
				if (!isAsyncStarted(request)) {
					logger.debug("Closing Hibernate Session in OpenSessionInViewFilter");
					SessionFactoryUtils.closeSession(sessionHolder.getSession());
				}
			}
		}
	}

	/**
	 * 查找此过滤器应使用的SessionFactory, 将当前HTTP请求作为参数.
	 * <p>默认实现委托给没有参数的{@link #lookupSessionFactory()}变体.
	 * 
	 * @param request 当前HTTP请求
	 * 
	 * @return 要使用的SessionFactory
	 */
	protected SessionFactory lookupSessionFactory(HttpServletRequest request) {
		return lookupSessionFactory();
	}

	/**
	 * 查找此过滤器应使用的SessionFactory.
	 * <p>默认实现在Spring的根应用程序上下文中查找具有指定名称的bean.
	 * 
	 * @return 要使用的SessionFactory
	 */
	protected SessionFactory lookupSessionFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using SessionFactory '" + getSessionFactoryBeanName() + "' for OpenSessionInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getSessionFactoryBeanName(), SessionFactory.class);
	}

	/**
	 * 打开此过滤器使用的SessionFactory的会话.
	 * <p>默认实现委托给{@link SessionFactory#openSession}方法, 并将{@link Session}的刷新模式设置为"MANUAL".
	 * 
	 * @param sessionFactory 此过滤器使用的SessionFactory
	 * 
	 * @return 要使用的Session
	 * @throws DataAccessResourceFailureException 如果无法创建会话
	 */
	protected Session openSession(SessionFactory sessionFactory) throws DataAccessResourceFailureException {
		try {
			Session session = sessionFactory.openSession();
			session.setFlushMode(FlushMode.MANUAL);
			return session;
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}

}
