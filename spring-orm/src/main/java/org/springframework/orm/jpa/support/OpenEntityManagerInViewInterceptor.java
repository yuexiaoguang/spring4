package org.springframework.orm.jpa.support;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * Spring Web请求拦截器, 它将JPA EntityManager绑定到线程以进行整个请求处理.
 * 用于"Open EntityManager in View"模式, i.e. 允许在Web视图中延迟加载, 尽管原始事务已经完成.
 *
 * <p>此拦截器通过当前线程使JPA EntityManager可用, 该线程将由事务管理器自动检测.
 * 它适用于通过{@link org.springframework.orm.jpa.JpaTransactionManager}
 * 或{@link org.springframework.transaction.jta.JtaTransactionManager}进行的服务层事务, 以及非事务性只读执行.
 *
 * <p>与{@link OpenEntityManagerInViewFilter}相反, 此拦截器设置在Spring应用程序上下文中, 因此可以利用bean连接.
 */
public class OpenEntityManagerInViewInterceptor extends EntityManagerFactoryAccessor implements AsyncWebRequestInterceptor {

	/**
	 * 附加到"参与现有实体管理器处理"请求属性的EntityManagerFactory toString表示的后缀.
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";


	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		String participateAttributeName = getParticipateAttributeName();

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		if (asyncManager.hasConcurrentResult()) {
			if (applyCallableInterceptor(asyncManager, participateAttributeName)) {
				return;
			}
		}

		if (TransactionSynchronizationManager.hasResource(getEntityManagerFactory())) {
			// 不要修改EntityManager: 只需相应地标记请求.
			Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		}
		else {
			logger.debug("Opening JPA EntityManager in OpenEntityManagerInViewInterceptor");
			try {
				EntityManager em = createEntityManager();
				EntityManagerHolder emHolder = new EntityManagerHolder(em);
				TransactionSynchronizationManager.bindResource(getEntityManagerFactory(), emHolder);

				AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(getEntityManagerFactory(), emHolder);
				asyncManager.registerCallableInterceptor(participateAttributeName, interceptor);
				asyncManager.registerDeferredResultInterceptor(participateAttributeName, interceptor);
			}
			catch (PersistenceException ex) {
				throw new DataAccessResourceFailureException("Could not create JPA EntityManager", ex);
			}
		}
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) {
	}

	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
		if (!decrementParticipateCount(request)) {
			EntityManagerHolder emHolder = (EntityManagerHolder)
					TransactionSynchronizationManager.unbindResource(getEntityManagerFactory());
			logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewInterceptor");
			EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
		}
	}

	private boolean decrementParticipateCount(WebRequest request) {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count == null) {
			return false;
		}
		// 不要编辑Session: 清空标记.
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
			TransactionSynchronizationManager.unbindResource(getEntityManagerFactory());
		}
	}

	/**
	 * 返回标识请求已过滤的请求属性的名称.
	 * 默认实现采用EntityManagerFactory实例的toString表示, 并附加".FILTERED".
	 */
	protected String getParticipateAttributeName() {
		return getEntityManagerFactory().toString() + PARTICIPATE_SUFFIX;
	}


	private boolean applyCallableInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}

}
