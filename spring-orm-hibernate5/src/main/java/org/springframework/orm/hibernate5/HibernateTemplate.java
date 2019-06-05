package org.springframework.orm.hibernate5;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Helper类, 简化了Hibernate数据访问代码.
 * 按照{@code org.springframework.dao}异常层次结构自动将HibernateExceptions转换为DataAccessExceptions.
 *
 * <p>中心方法是{@code execute}, 支持实现{@link HibernateCallback}接口的Hibernate访问代码.
 * 它提供了Hibernate Session处理, 使得HibernateCallback实现和调用代码都不需要明确关心检索/关闭Hibernate Session,
 * 或处理Session生命周期异常.
 * 对于典型的单步操作, 有各种便捷方法 (find, load, saveOrUpdate, delete).
 *
 * <p>可以通过直接实例化SessionFactory引用在服务实现中使用, 或者在应用程序上下文中准备并作为bean引用提供给服务.
 * Note: SessionFactory应始终在应用程序上下文中配置为bean, 在第一种情况下直接提供给服务, 在第二种情况下配置为准备好的模板.
 *
 * <p><b>NOTE: Hibernate访问代码也可以针对本机Hibernate {@link Session}进行编码.
 * 因此, 对于新启动的项目, 请考虑对{@link SessionFactory#getCurrentSession()}采用标准的Hibernate编码方式.</b>
 * 或者, 使用{@link #execute(HibernateCallback)}与Java 8 lambda代码块,
 * 用于回调提供的{@code Session}, 这也会产生优雅的代码, 与Hibernate会话生命周期分离.
 * 此HibernateTemplate上的其余操作主要作为现有应用程序中较旧的Hibernate 3.x/4.x 数据访问代码的迁移帮助方法存在.</b>
 */
public class HibernateTemplate implements HibernateOperations, InitializingBean {

	private static final Method createQueryMethod;

	private static final Method getNamedQueryMethod;

	static {
		// Hibernate 5.2的createQuery方法将一个新的子类型声明为返回类型, 因此需要使用反射来实现与5.0/5.1的二进制兼容.
		try {
			createQueryMethod = Session.class.getMethod("createQuery", String.class);
			getNamedQueryMethod = Session.class.getMethod("getNamedQuery", String.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Incompatible Hibernate Session API", ex);
		}
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	private String[] filterNames;

	private boolean exposeNativeSession = false;

	private boolean checkWriteOperations = true;

	private boolean cacheQueries = false;

	private String queryCacheRegion;

	private int fetchSize = 0;

	private int maxResults = 0;


	public HibernateTemplate() {
	}

	/**
	 * @param sessionFactory 用于创建Session的SessionFactory
	 */
	public HibernateTemplate(SessionFactory sessionFactory) {
		setSessionFactory(sessionFactory);
		afterPropertiesSet();
	}


	/**
	 * 设置应该用于创建Hibernate会话的Hibernate SessionFactory.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 设置应该用于创建Hibernate会话的Hibernate SessionFactory.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * 设置要激活的一个或多个Hibernate过滤器名称, 用于此访问者使用的所有会话.
	 * <p>这些过滤器中的每一个都将在每个操作开始时启用, 并在操作结束时相应地禁用.
	 * 这适用于新打开的会话以及现有会话 (例如, 在事务中).
	 */
	public void setFilterNames(String... filterNames) {
		this.filterNames = filterNames;
	}

	/**
	 * 返回要激活的Hibernate过滤器的名称.
	 */
	public String[] getFilterNames() {
		return this.filterNames;
	}

	/**
	 * 设置是否将本机Hibernate Session暴露给HibernateCallback代码.
	 * <p>默认 "false": 将返回会话代理, 禁用{@code close}调用, 并自动应用查询缓存设置和事务超时.
	 */
	public void setExposeNativeSession(boolean exposeNativeSession) {
		this.exposeNativeSession = exposeNativeSession;
	}

	/**
	 * 返回是否将本机Hibernate Session暴露给HibernateCallback代码, 或者是Session代理.
	 */
	public boolean isExposeNativeSession() {
		return this.exposeNativeSession;
	}

	/**
	 * 设置是否在写入操作(save/update/delete)时检查Hibernate会话是否处于只读模式.
	 * <p>默认"true", 在只读事务中尝试写入操作时的快速失败行为.
	 * 关闭它以允许在具有刷新模式MANUAL的会话上保存/更新/删除.
	 */
	public void setCheckWriteOperations(boolean checkWriteOperations) {
		this.checkWriteOperations = checkWriteOperations;
	}

	/**
	 * 返回是否在写入操作(save/update/delete)时检查Hibernate会话是否处于只读模式.
	 */
	public boolean isCheckWriteOperations() {
		return this.checkWriteOperations;
	}

	/**
	 * 设置是否缓存此模板执行的所有查询.
	 * <p>如果这是"true", 则此模板创建的所有Query和Criteria对象将被标记为可缓存 (包括通过find方法的所有查询).
	 * <p>要指定要用于此模板缓存的查询的查询区域, 设置"queryCacheRegion"属性.
	 */
	public void setCacheQueries(boolean cacheQueries) {
		this.cacheQueries = cacheQueries;
	}

	/**
	 * 返回是否缓存此模板执行的所有查询.
	 */
	public boolean isCacheQueries() {
		return this.cacheQueries;
	}

	/**
	 * 设置此模板执行的查询的缓存区域的名称.
	 * <p>如果指定了此项, 则它将应用于此模板创建的所有Query和Criteria对象 (包括通过find方法的所有查询).
	 * <p>除非将此模板创建的查询配置为通过"cacheQueries"属性进行缓存, 否则缓存区域不会生效.
	 */
	public void setQueryCacheRegion(String queryCacheRegion) {
		this.queryCacheRegion = queryCacheRegion;
	}

	/**
	 * 返回此模板执行的查询的缓存区域的名称.
	 */
	public String getQueryCacheRegion() {
		return this.queryCacheRegion;
	}

	/**
	 * 设置此HibernateTemplate的获取大小.
	 * 这对于处理大型结果集很重要: 将其设置为高于默认值, 将以内存消耗为代价提高处理速度;
	 * 将此值设置得较低可以避免传输应用程序永远不会读取的行数据.
	 * <p>默认值为0, 表示使用JDBC驱动程序的默认值.
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * 返回为此HibernateTemplate指定的获取大小.
	 */
	public int getFetchSize() {
		return this.fetchSize;
	}

	/**
	 * 设置此HibernateTemplate的最大行数.
	 * 这对于处理大型结果集的子集非常重要, 如果从不对整个结果感兴趣, 则避免在数据库或JDBC驱动程序中读取和保存整个结果集
	 * (例如, 执行可能返回大量匹配的搜索时).
	 * <p>默认值为0, 表示使用JDBC驱动程序的默认值.
	 */
	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * 返回为此HibernateTemplate指定的最大行数.
	 */
	public int getMaxResults() {
		return this.maxResults;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}


	@Override
	public <T> T execute(HibernateCallback<T> action) throws DataAccessException {
		return doExecute(action, false);
	}

	/**
	 * 在本机{@link Session}中执行给定操作对象指定的操作.
	 * <p>此执行变量覆盖模板范围的{@link #isExposeNativeSession() "exposeNativeSession"}设置.
	 * 
	 * @param action 指定Hibernate操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果Hibernate错误
	 */
	public <T> T executeWithNativeSession(HibernateCallback<T> action) {
		return doExecute(action, true);
	}

	/**
	 * 在会话中执行给定操作对象指定的操作.
	 * 
	 * @param action 指定Hibernate操作的回调对象
	 * @param enforceNativeSession 是否强制将本机Hibernate Session暴露给回调代码
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws DataAccessException 如果Hibernate错误
	 */
	@SuppressWarnings("deprecation")
	protected <T> T doExecute(HibernateCallback<T> action, boolean enforceNativeSession) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Session session = null;
		boolean isNew = false;
		try {
			session = getSessionFactory().getCurrentSession();
		}
		catch (HibernateException ex) {
			logger.debug("Could not retrieve pre-bound Hibernate session", ex);
		}
		if (session == null) {
			session = getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);
			isNew = true;
		}

		try {
			enableFilters(session);
			Session sessionToExpose =
					(enforceNativeSession || isExposeNativeSession() ? session : createSessionProxy(session));
			return action.doInHibernate(sessionToExpose);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw SessionFactoryUtils.convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}
		catch (RuntimeException ex) {
			// Callback code threw application exception...
			throw ex;
		}
		finally {
			if (isNew) {
				SessionFactoryUtils.closeSession(session);
			}
			else {
				disableFilters(session);
			}
		}
	}

	/**
	 * 为给定的Hibernate会话创建一个禁用关闭的代理.
	 * 代理还准备返回的Query和Criteria对象.
	 * 
	 * @param session 为其创建代理的Hibernate Session
	 * 
	 * @return Session代理
	 */
	protected Session createSessionProxy(Session session) {
		return (Session) Proxy.newProxyInstance(
				session.getClass().getClassLoader(), new Class<?>[] {Session.class},
				new CloseSuppressingInvocationHandler(session));
	}

	/**
	 * 在给定的Session上启用指定的过滤器.
	 * 
	 * @param session 当前Hibernate Session
	 */
	protected void enableFilters(Session session) {
		String[] filterNames = getFilterNames();
		if (filterNames != null) {
			for (String filterName : filterNames) {
				session.enableFilter(filterName);
			}
		}
	}

	/**
	 * 禁用给定Session上的指定过滤器.
	 * 
	 * @param session 当前Hibernate Session
	 */
	protected void disableFilters(Session session) {
		String[] filterNames = getFilterNames();
		if (filterNames != null) {
			for (String filterName : filterNames) {
				session.disableFilter(filterName);
			}
		}
	}


	//-------------------------------------------------------------------------
	// Convenience methods for loading individual objects
	//-------------------------------------------------------------------------

	@Override
	public <T> T get(Class<T> entityClass, Serializable id) throws DataAccessException {
		return get(entityClass, id, null);
	}

	@Override
	public <T> T get(final Class<T> entityClass, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			public T doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return session.get(entityClass, id, new LockOptions(lockMode));
				}
				else {
					return session.get(entityClass, id);
				}
			}
		});
	}

	@Override
	public Object get(String entityName, Serializable id) throws DataAccessException {
		return get(entityName, id, null);
	}

	@Override
	public Object get(final String entityName, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return session.get(entityName, id, new LockOptions(lockMode));
				}
				else {
					return session.get(entityName, id);
				}
			}
		});
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable id) throws DataAccessException {
		return load(entityClass, id, null);
	}

	@Override
	public <T> T load(final Class<T> entityClass, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			public T doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return session.load(entityClass, id, new LockOptions(lockMode));
				}
				else {
					return session.load(entityClass, id);
				}
			}
		});
	}

	@Override
	public Object load(String entityName, Serializable id) throws DataAccessException {
		return load(entityName, id, null);
	}

	@Override
	public Object load(final String entityName, final Serializable id, final LockMode lockMode)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					return session.load(entityName, id, new LockOptions(lockMode));
				}
				else {
					return session.load(entityName, id);
				}
			}
		});
	}

	@Override
	public <T> List<T> loadAll(final Class<T> entityClass) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<List<T>>() {
			@Override
			@SuppressWarnings({"unchecked", "deprecation"})
			public List<T> doInHibernate(Session session) throws HibernateException {
				Criteria criteria = session.createCriteria(entityClass);
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				prepareCriteria(criteria);
				return criteria.list();
			}
		});
	}

	@Override
	public void load(final Object entity, final Serializable id) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.load(entity, id);
				return null;
			}
		});
	}

	@Override
	public void refresh(final Object entity) throws DataAccessException {
		refresh(entity, null);
	}

	@Override
	public void refresh(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				if (lockMode != null) {
					session.refresh(entity, new LockOptions(lockMode));
				}
				else {
					session.refresh(entity);
				}
				return null;
			}
		});
	}

	@Override
	public boolean contains(final Object entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Boolean>() {
			@Override
			public Boolean doInHibernate(Session session) {
				return session.contains(entity);
			}
		});
	}

	@Override
	public void evict(final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.evict(entity);
				return null;
			}
		});
	}

	@Override
	public void initialize(Object proxy) throws DataAccessException {
		try {
			Hibernate.initialize(proxy);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}

	@Override
	public Filter enableFilter(String filterName) throws IllegalStateException {
		Session session = getSessionFactory().getCurrentSession();
		Filter filter = session.getEnabledFilter(filterName);
		if (filter == null) {
			filter = session.enableFilter(filterName);
		}
		return filter;
	}


	//-------------------------------------------------------------------------
	// Convenience methods for storing individual objects
	//-------------------------------------------------------------------------

	@Override
	public void lock(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
				return null;
			}
		});
	}

	@Override
	public void lock(final String entityName, final Object entity, final LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
				return null;
			}
		});
	}

	@Override
	public Serializable save(final Object entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Serializable>() {
			@Override
			public Serializable doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return session.save(entity);
			}
		});
	}

	@Override
	public Serializable save(final String entityName, final Object entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Serializable>() {
			@Override
			public Serializable doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return session.save(entityName, entity);
			}
		});
	}

	@Override
	public void update(Object entity) throws DataAccessException {
		update(entity, null);
	}

	@Override
	public void update(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.update(entity);
				if (lockMode != null) {
					session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
				}
				return null;
			}
		});
	}

	@Override
	public void update(String entityName, Object entity) throws DataAccessException {
		update(entityName, entity, null);
	}

	@Override
	public void update(final String entityName, final Object entity, final LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.update(entityName, entity);
				if (lockMode != null) {
					session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
				}
				return null;
			}
		});
	}

	@Override
	public void saveOrUpdate(final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.saveOrUpdate(entity);
				return null;
			}
		});
	}

	@Override
	public void saveOrUpdate(final String entityName, final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.saveOrUpdate(entityName, entity);
				return null;
			}
		});
	}

	@Override
	public void replicate(final Object entity, final ReplicationMode replicationMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.replicate(entity, replicationMode);
				return null;
			}
		});
	}

	@Override
	public void replicate(final String entityName, final Object entity, final ReplicationMode replicationMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.replicate(entityName, entity, replicationMode);
				return null;
			}
		});
	}

	@Override
	public void persist(final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.persist(entity);
				return null;
			}
		});
	}

	@Override
	public void persist(final String entityName, final Object entity) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				session.persist(entityName, entity);
				return null;
			}
		});
	}

	@Override
	public <T> T merge(final T entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return (T) session.merge(entity);
			}
		});
	}

	@Override
	public <T> T merge(final String entityName, final T entity) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				return (T) session.merge(entityName, entity);
			}
		});
	}

	@Override
	public void delete(Object entity) throws DataAccessException {
		delete(entity, null);
	}

	@Override
	public void delete(final Object entity, final LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				if (lockMode != null) {
					session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
				}
				session.delete(entity);
				return null;
			}
		});
	}

	@Override
	public void delete(String entityName, Object entity) throws DataAccessException {
		delete(entityName, entity, null);
	}

	@Override
	public void delete(final String entityName, final Object entity, final LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				if (lockMode != null) {
					session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
				}
				session.delete(entityName, entity);
				return null;
			}
		});
	}

	@Override
	public void deleteAll(final Collection<?> entities) throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				checkWriteOperationAllowed(session);
				for (Object entity : entities) {
					session.delete(entity);
				}
				return null;
			}
		});
	}

	@Override
	public void flush() throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				session.flush();
				return null;
			}
		});
	}

	@Override
	public void clear() throws DataAccessException {
		executeWithNativeSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) {
				session.clear();
				return null;
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for HQL strings
	//-------------------------------------------------------------------------

	@Override
	public List<?> find(final String queryString, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<List<?>>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public List<?> doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(createQueryMethod, session, queryString);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List<?> findByNamedParam(String queryString, String paramName, Object value)
			throws DataAccessException {

		return findByNamedParam(queryString, new String[] {paramName}, new Object[] {value});
	}

	@Override
	public List<?> findByNamedParam(final String queryString, final String[] paramNames, final Object[] values)
			throws DataAccessException {

		if (paramNames.length != values.length) {
			throw new IllegalArgumentException("Length of paramNames array must match length of values array");
		}
		return executeWithNativeSession(new HibernateCallback<List<?>>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public List<?> doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(createQueryMethod, session, queryString);
				prepareQuery(queryObject);
				for (int i = 0; i < values.length; i++) {
					applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List<?> findByValueBean(final String queryString, final Object valueBean)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<List<?>>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public List<?> doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(createQueryMethod, session, queryString);
				prepareQuery(queryObject);
				queryObject.setProperties(valueBean);
				return queryObject.list();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for named queries
	//-------------------------------------------------------------------------

	@Override
	public List<?> findByNamedQuery(final String queryName, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<List<?>>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public List<?> doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(getNamedQueryMethod, session, queryName);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List<?> findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
			throws DataAccessException {

		return findByNamedQueryAndNamedParam(queryName, new String[] {paramName}, new Object[] {value});
	}

	@Override
	public List<?> findByNamedQueryAndNamedParam(
			final String queryName, final String[] paramNames, final Object[] values)
			throws DataAccessException {

		if (values != null && (paramNames == null || paramNames.length != values.length)) {
			throw new IllegalArgumentException("Length of paramNames array must match length of values array");
		}
		return executeWithNativeSession(new HibernateCallback<List<?>>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public List<?> doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(getNamedQueryMethod, session, queryName);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
					}
				}
				return queryObject.list();
			}
		});
	}

	@Override
	public List<?> findByNamedQueryAndValueBean(final String queryName, final Object valueBean)
			throws DataAccessException {

		return executeWithNativeSession(new HibernateCallback<List<?>>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public List<?> doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(getNamedQueryMethod, session, queryName);
				prepareQuery(queryObject);
				queryObject.setProperties(valueBean);
				return queryObject.list();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for detached criteria
	//-------------------------------------------------------------------------

	@Override
	public List<?> findByCriteria(DetachedCriteria criteria) throws DataAccessException {
		return findByCriteria(criteria, -1, -1);
	}

	@Override
	public List<?> findByCriteria(final DetachedCriteria criteria, final int firstResult, final int maxResults)
			throws DataAccessException {

		Assert.notNull(criteria, "DetachedCriteria must not be null");
		return executeWithNativeSession(new HibernateCallback<List<?>>() {
			@Override
			public List<?> doInHibernate(Session session) throws HibernateException {
				Criteria executableCriteria = criteria.getExecutableCriteria(session);
				prepareCriteria(executableCriteria);
				if (firstResult >= 0) {
					executableCriteria.setFirstResult(firstResult);
				}
				if (maxResults > 0) {
					executableCriteria.setMaxResults(maxResults);
				}
				return executableCriteria.list();
			}
		});
	}

	@Override
	public <T> List<T> findByExample(T exampleEntity) throws DataAccessException {
		return findByExample(null, exampleEntity, -1, -1);
	}

	@Override
	public <T> List<T> findByExample(String entityName, T exampleEntity) throws DataAccessException {
		return findByExample(entityName, exampleEntity, -1, -1);
	}

	@Override
	public <T> List<T> findByExample(T exampleEntity, int firstResult, int maxResults) throws DataAccessException {
		return findByExample(null, exampleEntity, firstResult, maxResults);
	}

	@Override
	@SuppressWarnings("deprecation")
	public <T> List<T> findByExample(
			final String entityName, final T exampleEntity, final int firstResult, final int maxResults)
			throws DataAccessException {

		Assert.notNull(exampleEntity, "Example entity must not be null");
		return executeWithNativeSession(new HibernateCallback<List<T>>() {
			@Override
			@SuppressWarnings("unchecked")
			public List<T> doInHibernate(Session session) throws HibernateException {
				Criteria executableCriteria = (entityName != null ?
						session.createCriteria(entityName) : session.createCriteria(exampleEntity.getClass()));
				executableCriteria.add(Example.create(exampleEntity));
				prepareCriteria(executableCriteria);
				if (firstResult >= 0) {
					executableCriteria.setFirstResult(firstResult);
				}
				if (maxResults > 0) {
					executableCriteria.setMaxResults(maxResults);
				}
				return executableCriteria.list();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience query methods for iteration and bulk updates/deletes
	//-------------------------------------------------------------------------

	@Override
	public Iterator<?> iterate(final String queryString, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Iterator<?>>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public Iterator<?> doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(createQueryMethod, session, queryString);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.iterate();
			}
		});
	}

	@Override
	public void closeIterator(Iterator<?> it) throws DataAccessException {
		try {
			Hibernate.close(it);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}

	@Override
	public int bulkUpdate(final String queryString, final Object... values) throws DataAccessException {
		return executeWithNativeSession(new HibernateCallback<Integer>() {
			@Override
			@SuppressWarnings({"rawtypes", "deprecation"})
			public Integer doInHibernate(Session session) throws HibernateException {
				org.hibernate.Query queryObject = (org.hibernate.Query)
						ReflectionUtils.invokeMethod(createQueryMethod, session, queryString);
				prepareQuery(queryObject);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				return queryObject.executeUpdate();
			}
		});
	}


	//-------------------------------------------------------------------------
	// Helper methods used by the operations above
	//-------------------------------------------------------------------------

	/**
	 * 检查给定Session是否允许写操作.
	 * <p>对于{@code FlushMode.MANUAL}, 默认实现会抛出InvalidDataAccessApiUsageException.
	 * 可以在子类中重写.
	 * 
	 * @param session 当前Hibernate Session
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果不允许写操作
	 */
	protected void checkWriteOperationAllowed(Session session) throws InvalidDataAccessApiUsageException {
		if (isCheckWriteOperations() && SessionFactoryUtils.getFlushMode(session).lessThan(FlushMode.COMMIT)) {
			throw new InvalidDataAccessApiUsageException(
					"Write operations are not allowed in read-only mode (FlushMode.MANUAL): "+
					"Turn your Session into FlushMode.COMMIT/AUTO or remove 'readOnly' marker from transaction definition.");
		}
	}

	/**
	 * 准备给定的Criteria对象, 应用缓存设置和/或事务超时.
	 * 
	 * @param criteria 要准备的 Criteria对象
	 */
	protected void prepareCriteria(Criteria criteria) {
		if (isCacheQueries()) {
			criteria.setCacheable(true);
			if (getQueryCacheRegion() != null) {
				criteria.setCacheRegion(getQueryCacheRegion());
			}
		}
		if (getFetchSize() > 0) {
			criteria.setFetchSize(getFetchSize());
		}
		if (getMaxResults() > 0) {
			criteria.setMaxResults(getMaxResults());
		}

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
		if (sessionHolder != null && sessionHolder.hasTimeout()) {
			criteria.setTimeout(sessionHolder.getTimeToLiveInSeconds());
		}
	}

	/**
	 * 准备给定的Query对象, 应用缓存设置和/或事务超时.
	 * 
	 * @param queryObject 要准备的Query对象
	 */
	@SuppressWarnings({"rawtypes", "deprecation"})
	protected void prepareQuery(org.hibernate.Query queryObject) {
		if (isCacheQueries()) {
			queryObject.setCacheable(true);
			if (getQueryCacheRegion() != null) {
				queryObject.setCacheRegion(getQueryCacheRegion());
			}
		}
		if (getFetchSize() > 0) {
			queryObject.setFetchSize(getFetchSize());
		}
		if (getMaxResults() > 0) {
			queryObject.setMaxResults(getMaxResults());
		}

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
		if (sessionHolder != null && sessionHolder.hasTimeout()) {
			queryObject.setTimeout(sessionHolder.getTimeToLiveInSeconds());
		}
	}

	/**
	 * 将给定的name参数应用于给定的Query对象.
	 * 
	 * @param queryObject Query对象
	 * @param paramName 参数名
	 * @param value 参数值
	 * 
	 * @throws HibernateException 如果由Query对象抛出
	 */
	@SuppressWarnings({"rawtypes", "deprecation"})
	protected void applyNamedParameterToQuery(org.hibernate.Query queryObject, String paramName, Object value)
			throws HibernateException {

		if (value instanceof Collection) {
			queryObject.setParameterList(paramName, (Collection<?>) value);
		}
		else if (value instanceof Object[]) {
			queryObject.setParameterList(paramName, (Object[]) value);
		}
		else {
			queryObject.setParameter(paramName, value);
		}
	}


	/**
	 * 调用Hibernate Session的调用处理器.
	 * 还准备返回的Query和Criteria对象.
	 */
	private class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Session target;

		public CloseSuppressingInvocationHandler(Session target) {
			this.target = target;
		}

		@Override
		@SuppressWarnings({"rawtypes", "deprecation"})
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 传入的Session接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用Session代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("close")) {
				// 处理close方法: 禁用, 无效.
				return null;
			}

			// 在目标Session上调用方法.
			try {
				Object retVal = method.invoke(this.target, args);

				// 如果返回值是Query或Criteria, 则应用事务超时.
				// 应用于createQuery, getNamedQuery, createCriteria.
				if (retVal instanceof Criteria) {
					prepareCriteria(((Criteria) retVal));
				}
				else if (retVal instanceof org.hibernate.Query) {
					prepareQuery(((org.hibernate.Query) retVal));
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}
}
