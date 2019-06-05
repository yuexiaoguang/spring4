package org.springframework.orm.hibernate4;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Filter;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.criterion.DetachedCriteria;

import org.springframework.dao.DataAccessException;

/**
 * 指定基本Hibernate操作的接口, 由{@link HibernateTemplate}实现.
 * 不经常使用, 但是增强可测试性的有用选项, 因为它很容易被模拟或存根.
 *
 * <p>定义{@code HibernateTemplate}的镜像各种{@link org.hibernate.Session}方法的数据访问方法.
 * 强烈建议用户阅读Hibernate {@code Session} javadoc以获取有关这些方法语义的详细信息.
 */
public interface HibernateOperations {

	/**
	 * 在{@link org.hibernate.Session}中执行给定操作对象指定的操作.
	 * <p>操作对象抛出的应用程序异常会传播到调用者 (只能取消选中).
	 * Hibernate异常转换为适当的DAO异常. 允许返回结果对象, 即域对象或域对象的集合.
	 * <p>Note: 回调代码本身不应该处理事务!
	 * 使用适当的事务管理器, 如{@link HibernateTransactionManager}.
	 * 通常, 回调代码不得触及任何{@code Session}生命周期方法, 如关闭, 断开连接或重新连接, 以使模板完成其工作.
	 * 
	 * @param action 指定Hibernate操作的回调对象
	 * 
	 * @return 操作返回的结果对象, 或{@code null}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> T execute(HibernateCallback<T> action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience methods for loading individual objects
	//-------------------------------------------------------------------------

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 或{@code null}.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#get(Class, java.io.Serializable)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * 
	 * @return 持久化实例, 或{@code null}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> T get(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 或{@code null}.
	 * <p>如果实例存在, 则获取指定的锁定模式.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#get(Class, java.io.Serializable, LockMode)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @return 持久化实例, 或{@code null}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> T get(Class<T> entityClass, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 或{@code null}.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#get(String, java.io.Serializable)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * 
	 * @return 持久化实例, 或{@code null}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	Object get(String entityName, Serializable id) throws DataAccessException;

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 或{@code null}.
	 * 如果实例存在, 则获取指定的锁定模式.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#get(String, java.io.Serializable, LockMode)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @return 持久化实例, 或{@code null}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	Object get(String entityName, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 如果未找到则抛​​出异常.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#load(Class, java.io.Serializable)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * 
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> T load(Class<T> entityClass, Serializable id) throws DataAccessException;

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 如果未找到则抛​​出异常.
	 * 如果实例存在, 则获取指定的锁定模式.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#load(Class, java.io.Serializable, LockMode)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityClass 持久化类
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> T load(Class<T> entityClass, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 如果未找到则抛​​出异常.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#load(String, java.io.Serializable)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * 
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	Object load(String entityName, Serializable id) throws DataAccessException;

	/**
	 * 返回具有给定标识符的给定实体类的持久化实例, 如果未找到则抛​​出异常.
	 * <p>如果实例存在, 则获取指定的锁定模式.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#load(String, java.io.Serializable, LockMode)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param id 持久化实例的标识符
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @return 持久化实例
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	Object load(String entityName, Serializable id, LockMode lockMode) throws DataAccessException;

	/**
	 * 返回给定实体类的所有持久化实例.
	 * Note: 使用查询或条件检索特定子集.
	 * 
	 * @param entityClass 持久化类
	 * 
	 * @return 包含0个或更多持久化实例的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> List<T> loadAll(Class<T> entityClass) throws DataAccessException;

	/**
	 * 将具有给定标识符的持久化实例加载到给定对象中, 如果未找到则抛​​出异常.
	 * <p>为了方便起见, 这个方法是{@link org.hibernate.Session#load(Object, java.io.Serializable)}的瘦包装器.
	 * 有关此方法的确切语义的解释, 请参考第一个实例中的Hibernate API文档.
	 * 
	 * @param entity 要加载到的对象(目标类)
	 * @param id 持久化实例的标识符
	 * 
	 * @throws org.springframework.orm.ObjectRetrievalFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void load(Object entity, Serializable id) throws DataAccessException;

	/**
	 * 重新读取给定持久化实例的状态.
	 * 
	 * @param entity 要重新读取的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void refresh(Object entity) throws DataAccessException;

	/**
	 * 重新读取给定持久化实例的状态.
	 * 获取实例的指定锁定模式.
	 * 
	 * @param entity 要重新读取的持久化实例
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void refresh(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 检查给定对象是否在Session缓存中.
	 * 
	 * @param entity 要检查的持久化实例
	 * 
	 * @return 给定对象是否在Session缓存中
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	boolean contains(Object entity) throws DataAccessException;

	/**
	 * 从{@link org.hibernate.Session}缓存中删除给定对象.
	 * 
	 * @param entity 要驱逐的持久实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void evict(Object entity) throws DataAccessException;

	/**
	 * 强制初始化Hibernate代理或持久化集合.
	 * 
	 * @param proxy 持久化对象或持久化集合的代理
	 * 
	 * @throws DataAccessException 如果无法初始化代理, 例如因为它没有与活动的Session关联
	 */
	void initialize(Object proxy) throws DataAccessException;

	/**
	 * 返回给定的过滤器名称的已启用的Hibernate {@link Filter}.
	 * 返回的{@code Filter}实例可用于设置过滤器参数.
	 * 
	 * @param filterName 过滤器名称
	 * 
	 * @return 已启用的Hibernate {@code Filter} (已经启用或通过此操作即时启用)
	 * @throws IllegalStateException 如果没有在事务会话中运行 (在这种情况下此操作没有意义)
	 */
	Filter enableFilter(String filterName) throws IllegalStateException;


	//-------------------------------------------------------------------------
	// Convenience methods for storing individual objects
	//-------------------------------------------------------------------------

	/**
	 * 获取给定对象的指定锁级别, 隐式检查相应的数据库条目是否仍然存在.
	 * 
	 * @param entity 要锁定的持久化实例
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void lock(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 获取给定对象的指定锁级别, 隐式检查相应的数据库条目是否仍然存在.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要锁定的持久化实例
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void lock(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例.
	 * 
	 * @param entity 要持久化的瞬态实例
	 * 
	 * @return 生成的标识符
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	Serializable save(Object entity) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要持久化的瞬态实例
	 * 
	 * @return 生成的标识符
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	Serializable save(String entityName, Object entity) throws DataAccessException;

	/**
	 * 更新给定的持久化实例, 将其与当前的Hibernate {@link org.hibernate.Session}关联.
	 * 
	 * @param entity 要更新的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void update(Object entity) throws DataAccessException;

	/**
	 * 更新给定的持久化实例, 将其与当前的Hibernate {@link org.hibernate.Session}关联.
	 * <p>如果实例存在, 则获取指定的锁定模式, 隐式检查相应的数据库条目是否仍然存在.
	 * 
	 * @param entity 要更新的持久化实例
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void update(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 更新给定的持久化实例, 将其与当前的Hibernate {@link org.hibernate.Session}关联.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要更新的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void update(String entityName, Object entity) throws DataAccessException;

	/**
	 * 更新给定的持久化实例, 将其与当前的Hibernate {@link org.hibernate.Session}关联.
	 * <p>如果实例存在, 则获取指定的锁定模式, 隐式检查相应的数据库条目是否仍然存在.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要更新的持久化实例
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void update(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 保存或更新给定的持久化实例, 根据其id (匹配配置的"unsaved-value"?).
	 * 将实例与当前的Hibernate {@link org.hibernate.Session}关联.
	 * 
	 * @param entity 要保存或更新的持久化实例 (与Hibernate {@code Session}相关联)
	 * 
	 * @throws DataAccessException 如果Hibernate错误
	 */
	void saveOrUpdate(Object entity) throws DataAccessException;

	/**
	 * 保存或更新给定的持久化实例, 根据其id (匹配配置的"unsaved-value"?).
	 * 将实例与当前的Hibernate {@code Session}关联.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要保存或更新的持久化实例 (与Hibernate {@code Session}相关联)
	 * 
	 * @throws DataAccessException 如果Hibernate错误
	 */
	void saveOrUpdate(String entityName, Object entity) throws DataAccessException;

	/**
	 * 根据给定的复制模式持久化给定的分离实例的状态, 重用当前标识符值.
	 * 
	 * @param entity 要复制的持久化对象
	 * @param replicationMode the Hibernate ReplicationMode
	 * 
	 * @throws DataAccessException 如果Hibernate错误
	 */
	void replicate(Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * 根据给定的复制模式持久化给定的分离实例的状态, 重用当前标识符值.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要复制的持久化对象
	 * @param replicationMode the Hibernate ReplicationMode
	 * 
	 * @throws DataAccessException 如果Hibernate错误
	 */
	void replicate(String entityName, Object entity, ReplicationMode replicationMode) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例. 遵循JSR-220语义.
	 * <p>与{@code save}类似, 将给定对象与当前Hibernate {@link org.hibernate.Session}关联.
	 * 
	 * @param entity 要持久化的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void persist(Object entity) throws DataAccessException;

	/**
	 * 持久化给定的瞬态实例. 遵循JSR-220语义.
	 * <p>与{@code save}类似, 将给定对象与当前Hibernate {@link org.hibernate.Session}关联.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要持久化的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void persist(String entityName, Object entity) throws DataAccessException;

	/**
	 * 将给定对象的状态复制到具有相同标识符的持久化对象上. 遵循JSR-220语义.
	 * <p>与{@code saveOrUpdate}类似, 但从不将给定对象与当前的Hibernate会话相关联.
	 * 如果是新实体, 状态也将被复制.
	 * <p>请注意, {@code merge}将<i>不会</i>更新传入的对象图中的标识符 (与TopLink相比)!
	 * 如果希望将新分配的ID转移到原始对象图, 考虑注册Spring的{@code IdTransferringMergeEventListener}.
	 * 
	 * @param entity 要与对应的持久化实例合并的对象
	 * 
	 * @return 更新的, 已注册的持久化实例
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> T merge(T entity) throws DataAccessException;

	/**
	 * 将给定对象的状态复制到具有相同标识符的持久化对象上. 遵循JSR-220语义.
	 * <p>与{@code saveOrUpdate}类似, 但从不将给定对象与当前的Hibernate {@link org.hibernate.Session}相关联.
	 * 如果是新实体, 状态也将被复制.
	 * <p>请注意, {@code merge}将<i>不会</i>更新传入的对象图中的标识符 (与TopLink相比)!
	 * 如果希望将新分配的ID转移到原始对象图, 考虑注册Spring的{@code IdTransferringMergeEventListener}.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要与对应的持久化实例合并的对象
	 * 
	 * @return 更新的, 已注册的持久化实例
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> T merge(String entityName, T entity) throws DataAccessException;

	/**
	 * 删除给定的持久化实例.
	 * 
	 * @param entity 要删除的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void delete(Object entity) throws DataAccessException;

	/**
	 * 删除给定的持久化实例.
	 * <p>如果实例存在, 则获取指定的锁定模式, 隐式检查相应的数据库条目是否仍然存在.
	 * 
	 * @param entity 要删除的持久化实例
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void delete(Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 删除给定的持久化实例.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要删除的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void delete(String entityName, Object entity) throws DataAccessException;

	/**
	 * 删除给定的持久化实例.
	 * <p>如果实例存在, 则获取指定的锁定模式, 隐式检查相应的数据库条目是否仍然存在.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param entity 要删除的持久化实例
	 * @param lockMode 要获取的锁定模式
	 * 
	 * @throws org.springframework.orm.ObjectOptimisticLockingFailureException 如果未找到
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void delete(String entityName, Object entity, LockMode lockMode) throws DataAccessException;

	/**
	 * 删除所有给定的持久化实例.
	 * <p>这可以与任何find方法结合使用, 通过在两行代码中查询删除.
	 * 
	 * @param entities 要删除的持久化实例
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void deleteAll(Collection<?> entities) throws DataAccessException;

	/**
	 * 刷新所有挂起的保存, 更新和删除数据库.
	 * <p>仅调用此选项以进行选择性实时刷新, 例如, 当JDBC代码需要在同一事务中查看某些更改时.
	 * 否则, 最好在事务完成时依赖自动刷新.
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void flush() throws DataAccessException;

	/**
	 * 从{@link org.hibernate.Session}缓存中删除所有对象, 并取消所有挂起的保存, 更新和删除.
	 * 
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	void clear() throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for HQL strings
	//-------------------------------------------------------------------------

	/**
	 * 执行HQL查询, 将多个值绑定到查询字符串中的 "?"参数.
	 * 
	 * @param queryString 用Hibernate的查询语言表示的查询
	 * @param values 参数的值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> find(String queryString, Object... values) throws DataAccessException;

	/**
	 * 执行HQL查询, 将一个值绑定到查询字符串中的 ":"命名参数.
	 * 
	 * @param queryString 用Hibernate的查询语言表示的查询
	 * @param paramName 参数名称
	 * @param value 参数值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByNamedParam(String queryString, String paramName, Object value) throws DataAccessException;

	/**
	 * 执行HQL查询, 将多个值绑定到查询字符串中的 ":"命名参数.
	 * 
	 * @param queryString 用Hibernate的查询语言表示的查询
	 * @param paramNames 参数名称
	 * @param values 参数值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByNamedParam(String queryString, String[] paramNames, Object[] values) throws DataAccessException;

	/**
	 * 执行HQL查询, 将给定bean的属性绑定到查询字符串中的<i>命名</i>参数.
	 * 
	 * @param queryString 用Hibernate的查询语言表示的查询
	 * @param valueBean 参数的值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByValueBean(String queryString, Object valueBean) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for named queries
	//-------------------------------------------------------------------------

	/**
	 * 执行将多个值绑定到查询字符串中的"?"参数的命名查询.
	 * <p>命名查询在Hibernate映射文件中定义.
	 * 
	 * @param queryName 映射文件中Hibernate查询的名称
	 * @param values 参数的值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByNamedQuery(String queryName, Object... values) throws DataAccessException;

	/**
	 * 执行命名查询, 将一个值绑定到查询字符串中的":"命名参数.
	 * <p>命名查询在Hibernate映射文件中定义.
	 * 
	 * @param queryName 映射文件中Hibernate查询的名称
	 * @param paramName 参数名
	 * @param value 参数值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
			throws DataAccessException;

	/**
	 * 执行命名查询, 将多个值绑定到查询字符串中的":"命名参数.
	 * <p>命名查询在Hibernate映射文件中定义.
	 * 
	 * @param queryName 映射文件中Hibernate查询的名称
	 * @param paramNames 参数名
	 * @param values 参数值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByNamedQueryAndNamedParam(String queryName, String[] paramNames, Object[] values)
			throws DataAccessException;

	/**
	 * 执行命名查询, 将给定bean的属性绑定到查询字符串中的":"命名参数.
	 * <p>命名查询在Hibernate映射文件中定义.
	 * 
	 * @param queryName 映射文件中Hibernate查询的名称
	 * @param valueBean 参数值
	 * 
	 * @return 包含查询执行结果的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByNamedQueryAndValueBean(String queryName, Object valueBean) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience finder methods for detached criteria
	//-------------------------------------------------------------------------

	/**
	 * 基于给定的Hibernate条件对象执行查询.
	 * 
	 * @param criteria 分离的Hibernate条件对象.
	 * <b>Note: 不要重复使用条件对象! 由于Hibernate条件设施的次优设计, 它们需要在每次执行时重新创建.</b>
	 * 
	 * @return 包含0个或更多持久化实例的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByCriteria(DetachedCriteria criteria) throws DataAccessException;

	/**
	 * 基于给定的Hibernate条件对象执行查询.
	 * 
	 * @param criteria 分离的Hibernate条件对象.
	 * <b>Note: 不要重复使用条件对象! 由于Hibernate条件设施的次优设计, 它们需要在每次执行时重新创建.</b>
	 * @param firstResult 要检索的第一个结果对象的索引 (从0开始)
	 * @param maxResults 要检索的最大结果对象数 (或 <=0 表示无限制)
	 * 
	 * @return 包含0个或更多持久化实例的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	List<?> findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults) throws DataAccessException;

	/**
	 * 基于给定的示例实体对象执行查询.
	 * 
	 * @param exampleEntity 所需实体的实例, 作为"按示例查询"的示例
	 * 
	 * @return 包含0个或更多持久化实例的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> List<T> findByExample(T exampleEntity) throws DataAccessException;

	/**
	 * 基于给定的示例实体对象执行查询.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param exampleEntity 所需实体的实例, 作为"按示例查询"的示例
	 * 
	 * @return 包含0个或更多持久化实例的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> List<T> findByExample(String entityName, T exampleEntity) throws DataAccessException;

	/**
	 * 基于给定的示例实体对象执行查询.
	 * 
	 * @param exampleEntity 所需实体的实例, 作为"按示例查询"的示例
	 * @param firstResult 要检索的第一个结果对象的索引 (从0开始)
	 * @param maxResults 要检索的最大结果对象数 (或 <=0 表示无限制)
	 * 
	 * @return 包含0个或更多持久化实例的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> List<T> findByExample(T exampleEntity, int firstResult, int maxResults) throws DataAccessException;

	/**
	 * 基于给定的示例实体对象执行查询.
	 * 
	 * @param entityName 持久化实体的名称
	 * @param exampleEntity 所需实体的实例, 作为"按示例查询"的示例
	 * @param firstResult 要检索的第一个结果对象的索引 (从0开始)
	 * @param maxResults 要检索的最大结果对象数 (或 <=0 表示无限制)
	 * 
	 * @return 包含0个或更多持久化实例的{@link List}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	<T> List<T> findByExample(String entityName, T exampleEntity, int firstResult, int maxResults)
			throws DataAccessException;


	//-------------------------------------------------------------------------
	// Convenience query methods for iteration and bulk updates/deletes
	//-------------------------------------------------------------------------

	/**
	 * 对持久化实例执行查询, 将多个值绑定到查询字符串中的"?"参数.
	 * <p>以{@link Iterator}的形式返回结果. 返回的实体按需初始化. 有关详细信息, 请参阅Hibernate API文档.
	 * 
	 * @param queryString 用Hibernate的查询语言表示的查询
	 * @param values 参数值
	 * 
	 * @return 包含0个或更多持久化实例的{@link Iterator}
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	Iterator<?> iterate(String queryString, Object... values) throws DataAccessException;

	/**
	 * 立即关闭由任何{@code iterate(..)}操作创建的{@link Iterator}, 而不是等到会话关闭或断开连接.
	 * 
	 * @param it 要关闭的{@code Iterator}
	 * 
	 * @throws DataAccessException 如果{@code Iterator}无法关闭
	 */
	void closeIterator(Iterator<?> it) throws DataAccessException;

	/**
	 * 根据给定的查询更新/删除所有对象，将多个值绑定到查询字符串中的"?"参数.
	 * 
	 * @param queryString 以Hibernate的查询语言表示的更新/删除查询
	 * @param values 参数的值
	 * 
	 * @return 更新/删除的实例数量
	 * @throws org.springframework.dao.DataAccessException 如果Hibernate错误
	 */
	int bulkUpdate(String queryString, Object... values) throws DataAccessException;

}
