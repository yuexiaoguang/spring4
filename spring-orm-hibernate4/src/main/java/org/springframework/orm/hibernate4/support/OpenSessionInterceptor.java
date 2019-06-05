package org.springframework.orm.hibernate4.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate4.SessionFactoryUtils;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 简单的AOP联盟{@link MethodInterceptor}实现,
 * 为每个方法调用绑定一个新的Hibernate {@link Session}, 如果之前没有绑定.
 *
 * <p>这是一个简单的Hibernate Session范围拦截器, 沿着{@link OpenSessionInViewInterceptor}, 只是用于AOP设置而不是MVC设置.
 * 它打开一个新的{@link Session}, 刷新模式为"MANUAL", 因为Session仅用于读取, 除非参与事务.
 */
public class OpenSessionInterceptor implements MethodInterceptor, InitializingBean {

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

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		SessionFactory sf = getSessionFactory();
		if (!TransactionSynchronizationManager.hasResource(sf)) {
			// 绑定到当前方法的范围的新会话...
			Session session = openSession();
			try {
				TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
				return invocation.proceed();
			}
			finally {
				SessionFactoryUtils.closeSession(session);
				TransactionSynchronizationManager.unbindResource(sf);
			}
		}
		else {
			// 找到预绑定的会话 -> 简单地继续.
			return invocation.proceed();
		}
	}

	/**
	 * 打开此拦截器使用的SessionFactory的Session.
	 * <p>默认实现委托给{@link SessionFactory#openSession}方法, 并将{@link Session}的刷新模式设置为 "MANUAL".
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
}
