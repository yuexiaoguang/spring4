package org.springframework.orm.jdo.support;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.jdo.PersistenceManagerFactoryUtils;
import org.springframework.orm.jdo.PersistenceManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

/**
 * Spring Web请求拦截器, 它将JDO PersistenceManager绑定到线程以进行整个请求处理.
 * 用于"Open PersistenceManager in View"模式, i.e. 允许在Web视图中延迟加载, 尽管原始事务已经完成.
 *
 * <p>此拦截器通过当前线程使JDO PersistenceManagers可用, 该线程将由事务管理器自动检测.
 * 它适用于通过{@link org.springframework.orm.jdo.JdoTransactionManager}
 * 或{@link org.springframework.transaction.jta.JtaTransactionManager}进行的服务层事务, 以及非事务性只读执行.
 *
 * <p>与{@link OpenPersistenceManagerInViewFilter}相反, 此拦截器设置在Spring应用程序上下文中, 因此可以利用bean连接.
 */
public class OpenPersistenceManagerInViewInterceptor implements WebRequestInterceptor {

	/**
	 * 附加到"参与现有持久性管理器处理"请求属性的PersistenceManagerFactory toString表示的后缀.
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";


	protected final Log logger = LogFactory.getLog(getClass());

	private PersistenceManagerFactory persistenceManagerFactory;


	/**
	 * 设置应该用于创建PersistenceManagers的JDO PersistenceManagerFactory.
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		this.persistenceManagerFactory = pmf;
	}

	/**
	 * 返回应该用于创建PersistenceManagers的JDO PersistenceManagerFactory.
	 */
	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return persistenceManagerFactory;
	}


	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		if (TransactionSynchronizationManager.hasResource(getPersistenceManagerFactory())) {
			// 不要修改 PersistenceManager: 只需相应地标记请求.
			String participateAttributeName = getParticipateAttributeName();
			Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		}
		else {
			logger.debug("Opening JDO PersistenceManager in OpenPersistenceManagerInViewInterceptor");
			PersistenceManager pm =
					PersistenceManagerFactoryUtils.getPersistenceManager(getPersistenceManagerFactory(), true);
			TransactionSynchronizationManager.bindResource(
					getPersistenceManagerFactory(), new PersistenceManagerHolder(pm));
		}
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) {
	}

	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count != null) {
			// 不要修改PersistenceManager: 只需清除标记即可.
			if (count > 1) {
				request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
			}
			else {
				request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			}
		}
		else {
			PersistenceManagerHolder pmHolder = (PersistenceManagerHolder)
					TransactionSynchronizationManager.unbindResource(getPersistenceManagerFactory());
			logger.debug("Closing JDO PersistenceManager in OpenPersistenceManagerInViewInterceptor");
			PersistenceManagerFactoryUtils.releasePersistenceManager(
					pmHolder.getPersistenceManager(), getPersistenceManagerFactory());
		}
	}

	/**
	 * 返回标识已过滤请求的请求属性的名称.
	 * 默认实现采用PersistenceManagerFactory实例的toString表示并附加".FILTERED".
	 */
	protected String getParticipateAttributeName() {
		return getPersistenceManagerFactory().toString() + PARTICIPATE_SUFFIX;
	}
}
