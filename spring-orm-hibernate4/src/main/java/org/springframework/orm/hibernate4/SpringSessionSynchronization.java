package org.springframework.orm.hibernate4;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 在预先绑定的Hibernate会话的Spring管理事务结束时, 资源清理的回调.
 */
public class SpringSessionSynchronization implements TransactionSynchronization, Ordered {

	private final SessionHolder sessionHolder;

	private final SessionFactory sessionFactory;

	private final boolean newSession;

	private boolean holderActive = true;


	public SpringSessionSynchronization(SessionHolder sessionHolder, SessionFactory sessionFactory) {
		this(sessionHolder, sessionFactory, false);
	}

	public SpringSessionSynchronization(SessionHolder sessionHolder, SessionFactory sessionFactory, boolean newSession) {
		this.sessionHolder = sessionHolder;
		this.sessionFactory = sessionFactory;
		this.newSession = newSession;
	}


	private Session getCurrentSession() {
		return this.sessionHolder.getSession();
	}


	@Override
	public int getOrder() {
		return SessionFactoryUtils.SESSION_SYNCHRONIZATION_ORDER;
	}

	@Override
	public void suspend() {
		if (this.holderActive) {
			TransactionSynchronizationManager.unbindResource(this.sessionFactory);
			// 在这里实时断开会话, 使释放模式"on_close"在JBoss上运行.
			getCurrentSession().disconnect();
		}
	}

	@Override
	public void resume() {
		if (this.holderActive) {
			TransactionSynchronizationManager.bindResource(this.sessionFactory, this.sessionHolder);
		}
	}

	@Override
	public void flush() {
		try {
			SessionFactoryUtils.logger.debug("Flushing Hibernate Session on explicit request");
			getCurrentSession().flush();
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}

	@Override
	public void beforeCommit(boolean readOnly) throws DataAccessException {
		if (!readOnly) {
			Session session = getCurrentSession();
			// 读写事务 -> 刷新Hibernate会话.
			// 进一步检查: 只有不是FlushMode.MANUAL时才刷新.
			if (!session.getFlushMode().equals(FlushMode.MANUAL)) {
				try {
					SessionFactoryUtils.logger.debug("Flushing Hibernate Session on transaction synchronization");
					session.flush();
				}
				catch (HibernateException ex) {
					throw SessionFactoryUtils.convertHibernateAccessException(ex);
				}
			}
		}
	}

	@Override
	public void beforeCompletion() {
		try {
			Session session = this.sessionHolder.getSession();
			if (this.sessionHolder.getPreviousFlushMode() != null) {
				// 如果是预绑定会话, 请恢复以前的刷新模式.
				session.setFlushMode(this.sessionHolder.getPreviousFlushMode());
			}
			// 实时地断开会话, 使释放模式"on_close"很好地工作.
			session.disconnect();
		}
		finally {
			// 如果它是一个新的会话, 此时取消绑定...
			if (this.newSession) {
				TransactionSynchronizationManager.unbindResource(this.sessionFactory);
				this.holderActive = false;
			}
		}
	}

	@Override
	public void afterCommit() {
	}

	@Override
	public void afterCompletion(int status) {
		try {
			if (status != STATUS_COMMITTED) {
				// 清除会话中的所有挂起的插入/更新/删除.
				// 必要的预绑定会话, 以避免不一致的状态.
				this.sessionHolder.getSession().clear();
			}
		}
		finally {
			this.sessionHolder.setSynchronizedWithTransaction(false);
			// 如果是新会话, 则此时调用close()...
			if (this.newSession) {
				SessionFactoryUtils.closeSession(this.sessionHolder.getSession());
			}
		}
	}

}
