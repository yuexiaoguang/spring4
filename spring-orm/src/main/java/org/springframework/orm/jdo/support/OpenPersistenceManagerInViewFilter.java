package org.springframework.orm.jdo.support;

import java.io.IOException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.orm.jdo.PersistenceManagerFactoryUtils;
import org.springframework.orm.jdo.PersistenceManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet过滤器, 它将JDO PersistenceManager绑定到线程以进行整个请求处理.
 * 用于"Open PersistenceManager in View"模式, i.e. 允许在Web视图中延迟加载, 尽管原始事务已经完成.
 *
 * <p>此过滤器使JDO PersistenceManagers可通过当前线程使用, 该线程将由事务管理器自动检测.
 * 它适用于通过{@link org.springframework.orm.jdo.JdoTransactionManager}
 * 或{@link org.springframework.transaction.jta.JtaTransactionManager}进行的服务层事务, 以及非事务性只读执行.
 *
 * <p>在Spring的根Web应用程序上下文中查找PersistenceManagerFactory.
 * 支持{@code web.xml}中的"persistenceManagerFactoryBeanName"过滤器 init-param; 默认的bean名称是"persistenceManagerFactory".
 */
public class OpenPersistenceManagerInViewFilter extends OncePerRequestFilter {

	public static final String DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME = "persistenceManagerFactory";

	private String persistenceManagerFactoryBeanName = DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME;


	/**
	 * 设置PersistenceManagerFactory的bean名称, 以从Spring的根应用程序上下文中获取.
	 * 默认"persistenceManagerFactory".
	 */
	public void setPersistenceManagerFactoryBeanName(String persistenceManagerFactoryBeanName) {
		this.persistenceManagerFactoryBeanName = persistenceManagerFactoryBeanName;
	}

	/**
	 * 返回PersistenceManagerFactory的bean名称, 以从Spring的根应用程序上下文中获取.
	 */
	protected String getPersistenceManagerFactoryBeanName() {
		return this.persistenceManagerFactoryBeanName;
	}


	/**
	 * 返回"false", 以便过滤器可以将打开的{@code PersistenceManager}重新绑定到每个异步调度的线程,
	 * 并推迟关闭它直到最后一次异步调度.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回"false", 以便过滤器可以为每个错误调度提供{@code PersistenceManager}.
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		PersistenceManagerFactory pmf = lookupPersistenceManagerFactory(request);
		boolean participate = false;

		if (TransactionSynchronizationManager.hasResource(pmf)) {
			// 不要修改PersistenceManager: 只需设置参与标志.
			participate = true;
		}
		else {
			logger.debug("Opening JDO PersistenceManager in OpenPersistenceManagerInViewFilter");
			PersistenceManager pm = PersistenceManagerFactoryUtils.getPersistenceManager(pmf, true);
			TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				PersistenceManagerHolder pmHolder = (PersistenceManagerHolder)
						TransactionSynchronizationManager.unbindResource(pmf);
				logger.debug("Closing JDO PersistenceManager in OpenPersistenceManagerInViewFilter");
				PersistenceManagerFactoryUtils.releasePersistenceManager(pmHolder.getPersistenceManager(), pmf);
			}
		}
	}

	/**
	 * 查找此过滤器应使用的PersistenceManagerFactory, 将当前HTTP请求作为参数.
	 * <p>默认实现在没有参数的情况下委托给{@code lookupPersistenceManagerFactory}.
	 * 
	 * @return 要使用的PersistenceManagerFactory
	 */
	protected PersistenceManagerFactory lookupPersistenceManagerFactory(HttpServletRequest request) {
		return lookupPersistenceManagerFactory();
	}

	/**
	 * 查找此过滤器应使用的PersistenceManagerFactory.
	 * 默认实现在Spring的根应用程序上下文中查找具有指定名称的bean.
	 * 
	 * @return 要使用的PersistenceManagerFactory
	 */
	protected PersistenceManagerFactory lookupPersistenceManagerFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using PersistenceManagerFactory '" + getPersistenceManagerFactoryBeanName() +
					"' for OpenPersistenceManagerInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getPersistenceManagerFactoryBeanName(), PersistenceManagerFactory.class);
	}

}
