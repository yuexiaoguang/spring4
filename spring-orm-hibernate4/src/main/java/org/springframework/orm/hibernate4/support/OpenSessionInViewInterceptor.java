package org.springframework.orm.hibernate4.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate4.SessionFactoryUtils;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * Spring Web请求拦截器, 它将Hibernate {@code Session}绑定到线程以进行整个请求处理.
 *
 * <p>此类是"Open Session in View"模式的具体表达, 该模式允许在Web视图中延迟加载关联, 尽管原始事务已经完成.
 *
 * <p>此拦截器通过当前线程使Hibernate会话可用, 该线程将由事务管理器自动检测.
 * 它适用于通过{@link org.springframework.orm.hibernate4.HibernateTransactionManager}进行的服务层事务,
 * 以及非事务性执行 (如果配置正确).
 *
 * <p>与{@link OpenSessionInViewFilter}相反, 此拦截器在Spring应用程序上下文中配置, 因此可以利用bean连接.
 *
 * <p><b>WARNING:</b> 将此拦截器应用于现有逻辑可能会导致之前未出现的问题,
 * 通过使用单个Hibernate {@code Session}来处理整个请求.
 * 特别是, 持久对象与Hibernate {@code Session}的重新关联必须在请求处理的最开始时进行,
 * 以避免与已加载的相同对象的实例发生冲突.
 */
public class OpenSessionInViewInterceptor implements AsyncWebRequestInterceptor {

	/**
	 * 附加到{@code SessionFactory} {@code toString()}表示的"参与现有会话处理"请求属性的后缀.
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";

	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;


	/**
	 * 设置应该用于创建Hibernate会话的Hibernate SessionFactory.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 返回应该用于创建Hibernate Session的Hibernate SessionFactory.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}


	/**
	 * 打开一个新的Hibernate {@code Session}, 并通过
	 * {@link org.springframework.transaction.support.TransactionSynchronizationManager}将其绑定到线程.
	 */
	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		String participateAttributeName = getParticipateAttributeName();

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		if (asyncManager.hasConcurrentResult()) {
			if (applySessionBindingInterceptor(asyncManager, participateAttributeName)) {
				return;
			}
		}

		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// 不要修改Session: 只需相应地标记请求.
			Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		}
		else {
			logger.debug("Opening Hibernate Session in OpenSessionInViewInterceptor");
			Session session = openSession();
			SessionHolder sessionHolder = new SessionHolder(session);
			TransactionSynchronizationManager.bindResource(getSessionFactory(), sessionHolder);

			AsyncRequestInterceptor asyncRequestInterceptor =
					new AsyncRequestInterceptor(getSessionFactory(), sessionHolder);
			asyncManager.registerCallableInterceptor(participateAttributeName, asyncRequestInterceptor);
			asyncManager.registerDeferredResultInterceptor(participateAttributeName, asyncRequestInterceptor);
		}
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) {
	}

	/**
	 * 从线程解除Hibernate {@code Session}的绑定并关闭它.
	 */
	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
		if (!decrementParticipateCount(request)) {
			SessionHolder sessionHolder =
					(SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
			logger.debug("Closing Hibernate Session in OpenSessionInViewInterceptor");
			SessionFactoryUtils.closeSession(sessionHolder.getSession());
		}
	}

	private boolean decrementParticipateCount(WebRequest request) {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count == null) {
			return false;
		}
		// 不要修改Session: 只需清除标记.
		if (count > 1) {
			request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
		}
		else {
			request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		}
		return true;
	}

	@Override
	public void afterConcurrentHandlingStarted(WebRequest request) {
		if (!decrementParticipateCount(request)) {
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
	}

	/**
	 * 打开此拦截器使用的SessionFactory的Session.
	 * <p>默认实现委托给{@link SessionFactory#openSession}方法, 并将{@link Session}的刷新模式设置为"MANUAL".
	 * 
	 * @return 要使用的Session
	 * @throws DataAccessResourceFailureException 如果无法创建会话
	 */
	protected Session openSession() throws DataAccessResourceFailureException {
		try {
			Session session = getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);
			return session;
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	/**
	 * 返回标识已拦截请求的请求属性的名称.
	 * <p>默认实现采用{@code SessionFactory}实例的{@code toString()}表示形式, 并附加{@link #PARTICIPATE_SUFFIX}.
	 */
	protected String getParticipateAttributeName() {
		return getSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}
}
