package org.springframework.transaction.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * 管理每个线程的资源和事务同步的中央委托.
 * 由资源管理代码使用, 而不是由典型的应用程序代码使用.
 *
 * <p>每个键支持一个资源而不覆盖, 也就是说, 在为同一个键设置新资源之前需要删除资源.
 * 如果同步处于活动状态, 则支持事务同步列表.
 *
 * <p>资源管理代码应该检查线程绑定的资源, e.g. JDBC Connections 或Hibernate Sessions, 通过{@code getResource}.
 * 这样的代码通常不应该将资源绑定到线程, 因为这是事务管理器的责任.
 * 另一个选择是, 如果事务同步处于活动状态, 则在首次使用时延迟绑定, 以执行跨越任意数量资源的事务.
 *
 * <p>事务管理器必须通过{@link #initSynchronization()} 和 {@link #clearSynchronization()}激活和取消激活事务同步.
 * 这由{@link AbstractPlatformTransactionManager}自动支持, 因此由所有标准的Spring事务管理器支持, 例如
 * {@link org.springframework.transaction.jta.JtaTransactionManager}
 * 和{@link org.springframework.jdbc.datasource.DataSourceTransactionManager}.
 *
 * <p>资源管理代码只应在此管理器处于活动状态时注册同步, 可以通过{@link #isSynchronizationActive}进行检查;
 * 它应该立即执行资源清理.
 * 如果事务同步未处于活动状态, 则表示没有当前事务, 或者事务管理器不支持事务同步.
 *
 * <p>例如, 同步用于始终在JTA事务中返回相同的资源, e.g. 分别为任何给定的DataSource或SessionFactory提供JDBC连接或Hibernate会话.
 */
public abstract class TransactionSynchronizationManager {

	private static final Log logger = LogFactory.getLog(TransactionSynchronizationManager.class);

	private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<Map<Object, Object>>("Transactional resources");

	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<Set<TransactionSynchronization>>("Transaction synchronizations");

	private static final ThreadLocal<String> currentTransactionName =
			new NamedThreadLocal<String>("Current transaction name");

	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
			new NamedThreadLocal<Boolean>("Current transaction read-only status");

	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
			new NamedThreadLocal<Integer>("Current transaction isolation level");

	private static final ThreadLocal<Boolean> actualTransactionActive =
			new NamedThreadLocal<Boolean>("Actual transaction active");


	//-------------------------------------------------------------------------
	// Management of transaction-associated resource handles
	//-------------------------------------------------------------------------

	/**
	 * 返回绑定到当前线程的所有资源.
	 * <p>主要用于调试. 资源管理器应始终调用{@code hasResource}, 用于他们感兴趣的特定资源键.
	 * 
	 * @return 带资源键的Map (通常是资源工厂)和资源值 (通常是活动资源对象), 或空Map 如果当前没有资源绑定
	 */
	public static Map<Object, Object> getResourceMap() {
		Map<Object, Object> map = resources.get();
		return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
	}

	/**
	 * 检查绑定到当前线程的给定键是否有资源.
	 * 
	 * @param key 要检查的键 (通常是资源工厂)
	 * 
	 * @return 如果有一个绑定到当前线程的值
	 */
	public static boolean hasResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doGetResource(actualKey);
		return (value != null);
	}

	/**
	 * 检索绑定到当前线程的给定键的资源.
	 * 
	 * @param key 要检查的键 (通常是资源工厂)
	 * 
	 * @return 绑定到当前线程的值 (通常是活动资源对象), 或{@code null}
	 */
	public static Object getResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doGetResource(actualKey);
		if (value != null && logger.isTraceEnabled()) {
			logger.trace("Retrieved value [" + value + "] for key [" + actualKey + "] bound to thread [" +
					Thread.currentThread().getName() + "]");
		}
		return value;
	}

	/**
	 * 实际检查绑定给定键的资源的值.
	 */
	private static Object doGetResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		Object value = map.get(actualKey);
		// 透明地删除标记为void的ResourceHolder...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			map.remove(actualKey);
			// 如果为空, 则删除整个ThreadLocal...
			if (map.isEmpty()) {
				resources.remove();
			}
			value = null;
		}
		return value;
	}

	/**
	 * 将给定键的给定资源绑定到当前线程.
	 * 
	 * @param key 绑定值的键 (通常是资源工厂)
	 * @param value 要绑定的值 (通常是活动的资源对象)
	 * 
	 * @throws IllegalStateException 如果已经有一个绑定到该线程的值
	 */
	public static void bindResource(Object key, Object value) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Assert.notNull(value, "Value must not be null");
		Map<Object, Object> map = resources.get();
		// set ThreadLocal Map if none found
		if (map == null) {
			map = new HashMap<Object, Object>();
			resources.set(map);
		}
		Object oldValue = map.put(actualKey, value);
		// 透明地禁止标记为void的ResourceHolder...
		if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
			oldValue = null;
		}
		if (oldValue != null) {
			throw new IllegalStateException("Already value [" + oldValue + "] for key [" +
					actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Bound value [" + value + "] for key [" + actualKey + "] to thread [" +
					Thread.currentThread().getName() + "]");
		}
	}

	/**
	 * 解除当前线程中给定键的资源绑定.
	 * 
	 * @param key 要解绑的键 (通常是资源工厂)
	 * 
	 * @return 先前绑定的值 (通常是活动资源对象)
	 * @throws IllegalStateException 如果没有绑定到该线程的值
	 */
	public static Object unbindResource(Object key) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doUnbindResource(actualKey);
		if (value == null) {
			throw new IllegalStateException(
					"No value for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
		}
		return value;
	}

	/**
	 * 解除当前线程中给定键的资源绑定.
	 * 
	 * @param key 要解绑的键 (通常是资源工厂)
	 * 
	 * @return 先前绑定的值, 或{@code null}
	 */
	public static Object unbindResourceIfPossible(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doUnbindResource(actualKey);
	}

	/**
	 * 实际删除绑定给定键的资源的值.
	 */
	private static Object doUnbindResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		Object value = map.remove(actualKey);
		// Remove entire ThreadLocal if empty...
		if (map.isEmpty()) {
			resources.remove();
		}
		// 透明地禁止标记为void的ResourceHolder...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			value = null;
		}
		if (value != null && logger.isTraceEnabled()) {
			logger.trace("Removed value [" + value + "] for key [" + actualKey + "] from thread [" +
					Thread.currentThread().getName() + "]");
		}
		return value;
	}


	//-------------------------------------------------------------------------
	// Management of transaction synchronizations
	//-------------------------------------------------------------------------

	/**
	 * 返回当前线程的事务同步是否处于活动状态.
	 * 可以在注册前调用, 以避免不必要的实例创建.
	 */
	public static boolean isSynchronizationActive() {
		return (synchronizations.get() != null);
	}

	/**
	 * 激活当前线程的事务同步.
	 * 在事务开始时由事务管理器调用.
	 * 
	 * @throws IllegalStateException 如果同步已经激活
	 */
	public static void initSynchronization() throws IllegalStateException {
		if (isSynchronizationActive()) {
			throw new IllegalStateException("Cannot activate transaction synchronization - already active");
		}
		logger.trace("Initializing transaction synchronization");
		synchronizations.set(new LinkedHashSet<TransactionSynchronization>());
	}

	/**
	 * 为当前线程注册新的事务同步.
	 * 通常由资源管理代码调用.
	 * <p>请注意, 同步可以实现{@link org.springframework.core.Ordered}接口.
	 * 它们将根据顺序值按顺序执行.
	 * 
	 * @param synchronization 要注册的同步对象
	 * 
	 * @throws IllegalStateException 如果事务同步未激活
	 */
	public static void registerSynchronization(TransactionSynchronization synchronization)
			throws IllegalStateException {

		Assert.notNull(synchronization, "TransactionSynchronization must not be null");
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		synchronizations.get().add(synchronization);
	}

	/**
	 * 返回当前线程的所有已注册同步的不可修改的快照列表.
	 * 
	 * @return 不可修改的TransactionSynchronization实例列表
	 * @throws IllegalStateException 如果同步未激活
	 */
	public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
		Set<TransactionSynchronization> synchs = synchronizations.get();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		// 返回不可修改的快照, 以避免在迭代和调用同步回调时发生 ConcurrentModificationException,
		// 可能导致注册进一步的同步.
		if (synchs.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			// 这里延迟排序, 而不是在registerSynchronization中排序.
			List<TransactionSynchronization> sortedSynchs = new ArrayList<TransactionSynchronization>(synchs);
			AnnotationAwareOrderComparator.sort(sortedSynchs);
			return Collections.unmodifiableList(sortedSynchs);
		}
	}

	/**
	 * 取消激活当前线程的事务同步.
	 * 事务管理器在事务清理时调用.
	 * 
	 * @throws IllegalStateException 如果同步未激活
	 */
	public static void clearSynchronization() throws IllegalStateException {
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
		}
		logger.trace("Clearing transaction synchronization");
		synchronizations.remove();
	}


	//-------------------------------------------------------------------------
	// Exposure of transaction characteristics
	//-------------------------------------------------------------------------

	/**
	 * 公开当前事务的名称.
	 * 事务管理器在事务开始和清理时调用.
	 * 
	 * @param name 事务的名称, 或{@code null}
	 */
	public static void setCurrentTransactionName(String name) {
		currentTransactionName.set(name);
	}

	/**
	 * 返回当前事务的名称, 或{@code null}.
	 * 由资源管理代码调用以针对每个用例进行优化, 例如, 优化特定命名事务的获取策略.
	 */
	public static String getCurrentTransactionName() {
		return currentTransactionName.get();
	}

	/**
	 * 公开当前事务的只读标志.
	 * 事务管理器在事务开始和清理时调用.
	 * 
	 * @param readOnly {@code true}将当前事务标记为只读; {@code false}重置这样的只读标记
	 */
	public static void setCurrentTransactionReadOnly(boolean readOnly) {
		currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
	}

	/**
	 * 返回当前事务是否标记为只读.
	 * 在准备新创建的资源时由资源管理代码调用 (例如, Hibernate Session).
	 * <p>请注意, 事务同步接收只读标志作为{@code beforeCommit}回调的参数, 以便能够禁止提交时的更改检测.
	 * 本方法旨在用于早期的只读检查, 例如将Hibernate会话的刷新模式设置为"FlushMode.NEVER".
	 */
	public static boolean isCurrentTransactionReadOnly() {
		return (currentTransactionReadOnly.get() != null);
	}

	/**
	 * 公开当前事务的隔离级别.
	 * 事务管理器在事务开始和清理时调用.
	 * 
	 * @param isolationLevel 要公开的隔离级别, 根据JDBC连接常量 (相当于相应的Spring TransactionDefinition常量), 或{@code null}来重置它
	 */
	public static void setCurrentTransactionIsolationLevel(Integer isolationLevel) {
		currentTransactionIsolationLevel.set(isolationLevel);
	}

	/**
	 * 返回当前事务的隔离级别.
	 * 在准备新创建的资源时由资源管理代码调用 (例如, JDBC Connection).
	 * 
	 * @return 当前公开的隔离级别, 根据JDBC连接常量 (相当于相应的Spring TransactionDefinition常量), 或{@code null}
	 */
	public static Integer getCurrentTransactionIsolationLevel() {
		return currentTransactionIsolationLevel.get();
	}

	/**
	 * 公开当前是否有活动的实际事务.
	 * 事务管理器在事务开始和清理时调用.
	 * 
	 * @param active {@code true}将当前线程标记为与实际事务关联; {@code false}重置该标记
	 */
	public static void setActualTransactionActive(boolean active) {
		actualTransactionActive.set(active ? Boolean.TRUE : null);
	}

	/**
	 * 返回当前是否存在活动的实际事务.
	 * 这指示当前线程是否与实际事务相关联, 而不是仅与活动事务同步相关联.
	 * <p>由资源管理代码调用, 区分活动事务同步 (有或没有支持资源事务; 也在PROPAGATION_SUPPORTS上)
	 * 和实际事务处于活动状态 (支持资源事务; 在PROPAGATION_REQUIRED, PROPAGATION_REQUIRES_NEW, etc上).
	 */
	public static boolean isActualTransactionActive() {
		return (actualTransactionActive.get() != null);
	}


	/**
	 * 清除当前线程的整个事务同步状态:
	 * 注册的同步以及各种事务特征.
	 */
	public static void clear() {
		synchronizations.remove();
		currentTransactionName.remove();
		currentTransactionReadOnly.remove();
		currentTransactionIsolationLevel.remove();
		actualTransactionActive.remove();
	}
}
