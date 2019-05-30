package org.springframework.orm.jpa.support;

import java.io.IOException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet过滤器, 它将JPA EntityManager绑定到线程以进行整个请求处理.
 * 用于"Open EntityManager in View"模式, i.e. 允许在Web视图中延迟加载, 尽管原始事务已经完成.
 *
 * <p>此过滤器使JPA EntityManagers可通过当前线程获得, 该线程将由事务管理器自动检测.
 * 它适用于通过{@link org.springframework.orm.jpa.JpaTransactionManager}
 * 或{@link org.springframework.transaction.jta.JtaTransactionManager}进行的服务层事务, 以及非事务性只读执行.
 *
 * <p>在Spring的根Web应用程序上下文中查找EntityManagerFactory.
 * 支持{@code web.xml}中的"entityManagerFactoryBeanName"过滤器 init-param; 默认的bean名称是"entityManagerFactory".
 * 作为替代方案, the "persistenceUnitName" init-param允许按逻辑单元名称进行检索 (如{@code persistence.xml}中所指定).
 */
public class OpenEntityManagerInViewFilter extends OncePerRequestFilter {

	/**
	 * 默认的EntityManagerFactory bean名称: "entityManagerFactory".
	 * 仅在未指定"persistenceUnitName"参数时适用.
	 */
	public static final String DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";


	private String entityManagerFactoryBeanName;

	private String persistenceUnitName;

	private volatile EntityManagerFactory entityManagerFactory;


	/**
	 * 设置从Spring的根应用程序上下文中获取的EntityManagerFactory的bean名称.
	 * <p>默认"entityManagerFactory". 请注意, 此默认仅适用于未指定"persistenceUnitName"参数的情况.
	 */
	public void setEntityManagerFactoryBeanName(String entityManagerFactoryBeanName) {
		this.entityManagerFactoryBeanName = entityManagerFactoryBeanName;
	}

	/**
	 * 返回从Spring的根应用程序上下文中获取的EntityManagerFactory的bean名称.
	 */
	protected String getEntityManagerFactoryBeanName() {
		return this.entityManagerFactoryBeanName;
	}

	/**
	 * 设置要访问的EntityManagerFactory的持久化单元的名称.
	 * <p>这是通过bean名称指定EntityManagerFactory的替代方法, 通过其持久化单元名称来解析它.
	 * 如果没有指定bean名称和没有持久化单元名称, 将检查是否存在默认bean名称为"entityManagerFactory"的bean;
	 * 如果没有, 将通过查找EntityManagerFactory类型的单个唯一bean来检索默认的EntityManagerFactory.
	 */
	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * 返回要访问的EntityManagerFactory的持久化单元的名称.
	 */
	protected String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}


	/**
	 * 返回"false", 以便过滤器可以将打开的{@code EntityManager}重新绑定到每个异步调度的线程, 并推迟关闭它直到最后一次异步调度.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回"false", 以便过滤器可以为每个错误调度提供{@code EntityManager}.
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		EntityManagerFactory emf = lookupEntityManagerFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(emf)) {
			// 不要修改EntityManager: 只需设置参与标志.
			participate = true;
		}
		else {
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applyEntityManagerBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening JPA EntityManager in OpenEntityManagerInViewFilter");
				try {
					EntityManager em = createEntityManager(emf);
					EntityManagerHolder emHolder = new EntityManagerHolder(em);
					TransactionSynchronizationManager.bindResource(emf, emHolder);

					AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(emf, emHolder);
					asyncManager.registerCallableInterceptor(key, interceptor);
					asyncManager.registerDeferredResultInterceptor(key, interceptor);
				}
				catch (PersistenceException ex) {
					throw new DataAccessResourceFailureException("Could not create JPA EntityManager", ex);
				}
			}
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				EntityManagerHolder emHolder = (EntityManagerHolder)
						TransactionSynchronizationManager.unbindResource(emf);
				if (!isAsyncStarted(request)) {
					logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewFilter");
					EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
				}
			}
		}
	}

	/**
	 * 查找此过滤器应使用的EntityManagerFactory, 将当前HTTP请求作为参数.
	 * <p>默认实现委托给没有参数的{@code lookupEntityManagerFactory}, 一旦获得就缓存EntityManagerFactory引用.
	 * 
	 * @return 要使用的EntityManagerFactory
	 */
	protected EntityManagerFactory lookupEntityManagerFactory(HttpServletRequest request) {
		if (this.entityManagerFactory == null) {
			this.entityManagerFactory = lookupEntityManagerFactory();
		}
		return this.entityManagerFactory;
	}

	/**
	 * 查找此过滤器应使用的EntityManagerFactory.
	 * <p>默认实现在Spring的根应用程序上下文中查找具有指定名称的bean.
	 * 
	 * @return 要使用的EntityManagerFactory
	 */
	protected EntityManagerFactory lookupEntityManagerFactory() {
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		String emfBeanName = getEntityManagerFactoryBeanName();
		String puName = getPersistenceUnitName();
		if (StringUtils.hasLength(emfBeanName)) {
			return wac.getBean(emfBeanName, EntityManagerFactory.class);
		}
		else if (!StringUtils.hasLength(puName) && wac.containsBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME)) {
			return wac.getBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME, EntityManagerFactory.class);
		}
		else {
			// 包括按类型回退搜索单个EntityManagerFactory bean.
			return EntityManagerFactoryUtils.findEntityManagerFactory(wac, puName);
		}
	}

	/**
	 * 创建要绑定到请求的JPA EntityManager.
	 * <p>可以在子类中重写.
	 * 
	 * @param emf 要使用的EntityManagerFactory
	 */
	protected EntityManager createEntityManager(EntityManagerFactory emf) {
		return emf.createEntityManager();
	}

	private boolean applyEntityManagerBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}
}
