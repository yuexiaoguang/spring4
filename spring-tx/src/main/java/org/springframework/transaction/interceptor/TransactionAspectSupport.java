package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * 事务切面的基类, 例如{@link TransactionInterceptor}或AspectJ切面.
 *
 * <p>这使得底层Spring事务基础结构可以轻松地用于实现任何切面系统的切面.
 *
 * <p>子类负责以正确的顺序调用此类中的方法.
 *
 * <p>如果{@code TransactionAttribute}中未指定任何事务名称, 则公开的名称默认为{@code 完全限定类名 + "." + 方法名称}.
 *
 * <p>使用<b>策略</b>设计模式. {@code PlatformTransactionManager}实现将执行实际的事务管理,
 * {@code TransactionAttributeSource}用于确定事务定义.
 *
 * <p>如果事务切面的{@code PlatformTransactionManager}和{@code TransactionAttributeSource}是可序列化的, 则它是可序列化的.
 */
public abstract class TransactionAspectSupport implements BeanFactoryAware, InitializingBean {

	// NOTE: 此类不能实现Serializable, 因为它作为AspectJ方面的基类 (不允许实现Serializable)!


	/**
	 * 用于存储默认的事务管理器的键.
	 */
	private static final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();

	/**
	 * 支持{@code currentTransactionStatus()}方法的保存器, 并支持不同合作增强之间的通信
	 * (e.g. 前置增强和后置增强), 如果切面涉及多个方法 (就像环绕增强一样).
	 */
	private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
			new NamedThreadLocal<TransactionInfo>("Current aspect-driven transaction");


	/**
	 * 子类可以使用它来返回当前的TransactionInfo.
	 * 只有在一个方法中无法处理所有操作的子类, 例如涉及不同前后增强的AspectJ切面,
	 * 需要使用此机制来获取当前的TransactionInfo.
	 * 诸如AOP Alliance MethodInterceptor之类的环绕增强可以在整个方面方法中保存对TransactionInfo的引用.
	 * <p>即使没有创建任何事务, 也将返回TransactionInfo.
	 * {@code TransactionInfo.hasTransaction()}方法可用于查询.
	 * <p>要了解特定的事务特征, 考虑使用TransactionSynchronizationManager的{@code isSynchronizationActive()}
	 * 和/或{@code isActualTransactionActive()}方法.
	 * 
	 * @return 绑定到此线程的TransactionInfo, 或{@code null}
	 */
	protected static TransactionInfo currentTransactionInfo() throws NoTransactionException {
		return transactionInfoHolder.get();
	}

	/**
	 * 返回当前方法调用的事务状态.
	 * 主要用于希望设置当前事务只回滚但不抛出应用程序异常的代码.
	 * 
	 * @throws NoTransactionException 如果找不到事务信息, 因为该方法是在AOP调用上下文之外调用的
	 */
	public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
		TransactionInfo info = currentTransactionInfo();
		if (info == null || info.transactionStatus == null) {
			throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
		}
		return info.transactionStatus;
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private String transactionManagerBeanName;

	private PlatformTransactionManager transactionManager;

	private TransactionAttributeSource transactionAttributeSource;

	private BeanFactory beanFactory;

	private final ConcurrentMap<Object, PlatformTransactionManager> transactionManagerCache =
			new ConcurrentReferenceHashMap<Object, PlatformTransactionManager>(4);


	/**
	 * 指定默认事务管理器bean的名称.
	 */
	public void setTransactionManagerBeanName(String transactionManagerBeanName) {
		this.transactionManagerBeanName = transactionManagerBeanName;
	}

	/**
	 * 返回默认事务管理器bean的名称.
	 */
	protected final String getTransactionManagerBeanName() {
		return this.transactionManagerBeanName;
	}

	/**
	 * 指定用于驱动事务的<em>默认</em>事务管理器.
	 * <p>如果尚未为给定事务声明<em>限定符</em>, 或者尚未指定默认事务管理器bean的显式名称, 则将使用默认事务管理器.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回默认事务管理器, 或{@code null}.
	 */
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * 设置属性, 方法名称作为键, 事务属性描述符作为值 (通过TransactionAttributeEditor解析):
	 * e.g. key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly".
	 * <p>Note: 方法名称始终应用于目标类, 无论是在接口中定义还是在类本身中定义.
	 * <p>在内部, 将从给定属性创建NameMatchTransactionAttributeSource.
	 */
	public void setTransactionAttributes(Properties transactionAttributes) {
		NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
		tas.setProperties(transactionAttributes);
		this.transactionAttributeSource = tas;
	}

	/**
	 * 设置用于查找事务属性的多个事务属性源.
	 * 将为给定的源构建CompositeTransactionAttributeSource.
	 */
	public void setTransactionAttributeSources(TransactionAttributeSource... transactionAttributeSources) {
		this.transactionAttributeSource = new CompositeTransactionAttributeSource(transactionAttributeSources);
	}

	/**
	 * 设置用于查找事务属性的事务属性源.
	 * 如果指定String属性值, PropertyEditor将从该值创建MethodMapTransactionAttributeSource.
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		this.transactionAttributeSource = transactionAttributeSource;
	}

	/**
	 * 返回事务属性源.
	 */
	public TransactionAttributeSource getTransactionAttributeSource() {
		return this.transactionAttributeSource;
	}

	/**
	 * 设置用于检索PlatformTransactionManager bean的BeanFactory.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回用于检索PlatformTransactionManager bean的BeanFactory.
	 */
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 检查是否已设置所需的属性.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getTransactionManager() == null && this.beanFactory == null) {
			throw new IllegalStateException(
					"Set the 'transactionManager' property or make sure to run within a BeanFactory " +
					"containing a PlatformTransactionManager bean!");
		}
		if (getTransactionAttributeSource() == null) {
			throw new IllegalStateException(
					"Either 'transactionAttributeSource' or 'transactionAttributes' is required: " +
					"If there are no transactional methods, then don't use a transaction aspect.");
		}
	}


	/**
	 * 基于环绕增强的子类的一般委托, 委托给这个类的其他几个模板方法.
	 * 能够处理{@link CallbackPreferringPlatformTransactionManager}, 以及常规{@link PlatformTransactionManager}实现.
	 * 
	 * @param method 正在调用的方法
	 * @param targetClass 正在调用方法的目标类
	 * @param invocation 用于继续目标调用的回调
	 * 
	 * @return 方法的返回值
	 * @throws Throwable 从目标调用传播
	 */
	protected Object invokeWithinTransaction(Method method, Class<?> targetClass, final InvocationCallback invocation)
			throws Throwable {

		// 如果事务属性为null, 则该方法是非事务性的.
		final TransactionAttribute txAttr = getTransactionAttributeSource().getTransactionAttribute(method, targetClass);
		final PlatformTransactionManager tm = determineTransactionManager(txAttr);
		final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

		if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
			// 使用getTransaction和commit/rollback调用的标准事务划分.
			TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
			Object retVal = null;
			try {
				// 这是一个环绕增强: 调用链中的下一个拦截器.
				// 这通常会导致调用目标对象.
				retVal = invocation.proceedWithInvocation();
			}
			catch (Throwable ex) {
				// 目标调用异常
				completeTransactionAfterThrowing(txInfo, ex);
				throw ex;
			}
			finally {
				cleanupTransactionInfo(txInfo);
			}
			commitTransactionAfterReturning(txInfo);
			return retVal;
		}

		else {
			final ThrowableHolder throwableHolder = new ThrowableHolder();

			// 它是一个CallbackPreferringPlatformTransactionManager: 传递一个TransactionCallback.
			try {
				Object result = ((CallbackPreferringPlatformTransactionManager) tm).execute(txAttr,
						new TransactionCallback<Object>() {
							@Override
							public Object doInTransaction(TransactionStatus status) {
								TransactionInfo txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
								try {
									return invocation.proceedWithInvocation();
								}
								catch (Throwable ex) {
									if (txAttr.rollbackOn(ex)) {
										// RuntimeException: 将导致回滚.
										if (ex instanceof RuntimeException) {
											throw (RuntimeException) ex;
										}
										else {
											throw new ThrowableHolderException(ex);
										}
									}
									else {
										// 正常的返回值: 将导致提交.
										throwableHolder.throwable = ex;
										return null;
									}
								}
								finally {
									cleanupTransactionInfo(txInfo);
								}
							}
						});

				// 检查结果状态: 它可能表示要重新抛出Throwable.
				if (throwableHolder.throwable != null) {
					throw throwableHolder.throwable;
				}
				return result;
			}
			catch (ThrowableHolderException ex) {
				throw ex.getCause();
			}
			catch (TransactionSystemException ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
					ex2.initApplicationException(throwableHolder.throwable);
				}
				throw ex2;
			}
			catch (Throwable ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
				}
				throw ex2;
			}
		}
	}

	/**
	 * 清空缓存.
	 */
	protected void clearTransactionManagerCache() {
		this.transactionManagerCache.clear();
		this.beanFactory = null;
	}

	/**
	 * 确定要用于给定事务的特定事务管理器.
	 */
	protected PlatformTransactionManager determineTransactionManager(TransactionAttribute txAttr) {
		// 如果未设置tx属性, 请勿尝试查找tx管理器
		if (txAttr == null || this.beanFactory == null) {
			return getTransactionManager();
		}
		String qualifier = txAttr.getQualifier();
		if (StringUtils.hasText(qualifier)) {
			return determineQualifiedTransactionManager(qualifier);
		}
		else if (StringUtils.hasText(this.transactionManagerBeanName)) {
			return determineQualifiedTransactionManager(this.transactionManagerBeanName);
		}
		else {
			PlatformTransactionManager defaultTransactionManager = getTransactionManager();
			if (defaultTransactionManager == null) {
				defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
				if (defaultTransactionManager == null) {
					defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
					this.transactionManagerCache.putIfAbsent(
							DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
				}
			}
			return defaultTransactionManager;
		}
	}

	private PlatformTransactionManager determineQualifiedTransactionManager(String qualifier) {
		PlatformTransactionManager txManager = this.transactionManagerCache.get(qualifier);
		if (txManager == null) {
			txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
					this.beanFactory, PlatformTransactionManager.class, qualifier);
			this.transactionManagerCache.putIfAbsent(qualifier, txManager);
		}
		return txManager;
	}

	private String methodIdentification(Method method, Class<?> targetClass, TransactionAttribute txAttr) {
		String methodIdentification = methodIdentification(method, targetClass);
		if (methodIdentification == null) {
			if (txAttr instanceof DefaultTransactionAttribute) {
				methodIdentification = ((DefaultTransactionAttribute) txAttr).getDescriptor();
			}
			if (methodIdentification == null) {
				methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
			}
		}
		return methodIdentification;
	}

	/**
	 * 返回此Method的String表示形式, 以便在日志记录中使用.
	 * 可以在子类中重写, 以便为给定方法提供不同的标识符.
	 * <p>默认实现返回{@code null}, 表示使用{@link DefaultTransactionAttribute#getDescriptor()},
	 * 最后为{@link ClassUtils#getQualifiedMethodName(Method, Class)}.
	 * 
	 * @param method 感兴趣的方法
	 * @param targetClass 正在调用该方法的类
	 * 
	 * @return 标识此方法的String表示形式
	 */
	protected String methodIdentification(Method method, Class<?> targetClass) {
		return null;
	}

	/**
	 * 必要时根据给定的TransactionAttribute创建事务.
	 * <p>允许调用者通过TransactionAttributeSource执行自定义TransactionAttribute查找.
	 * 
	 * @param txAttr TransactionAttribute (may be {@code null})
	 * @param joinpointIdentification 完全限定的方法名称 (用于监视和记录目的)
	 * 
	 * @return TransactionInfo对象, 无论是否创建了事务.
	 * TransactionInfo上的{@code hasTransaction()}方法可用于判断是否创建了事务.
	 */
	@SuppressWarnings("serial")
	protected TransactionInfo createTransactionIfNecessary(
			PlatformTransactionManager tm, TransactionAttribute txAttr, final String joinpointIdentification) {

		// 如果未指定名称, 则将方法标识应用为事务名称.
		if (txAttr != null && txAttr.getName() == null) {
			txAttr = new DelegatingTransactionAttribute(txAttr) {
				@Override
				public String getName() {
					return joinpointIdentification;
				}
			};
		}

		TransactionStatus status = null;
		if (txAttr != null) {
			if (tm != null) {
				status = tm.getTransaction(txAttr);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
							"] because no transaction manager has been configured");
				}
			}
		}
		return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
	}

	/**
	 * 为给定的属性和状态对象准备TransactionInfo.
	 * 
	 * @param txAttr TransactionAttribute (may be {@code null})
	 * @param joinpointIdentification 完全限定的方法名称 (用于监视和记录目的)
	 * @param status 当前事务的TransactionStatus
	 * 
	 * @return 准备好的TransactionInfo对象
	 */
	protected TransactionInfo prepareTransactionInfo(PlatformTransactionManager tm,
			TransactionAttribute txAttr, String joinpointIdentification, TransactionStatus status) {

		TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
		if (txAttr != null) {
			// 需要此方法的事务...
			if (logger.isTraceEnabled()) {
				logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
			}
			// 如果已存在不兼容的tx, 则事务管理器将标记错误.
			txInfo.newTransactionStatus(status);
		}
		else {
			// TransactionInfo.hasTransaction() 方法将返回 false.
			// 创建它只是为了保持此类中维护的ThreadLocal堆栈的完整性.
			if (logger.isTraceEnabled())
				logger.trace("Don't need to create transaction for [" + joinpointIdentification +
						"]: This method isn't transactional.");
		}

		// 总是将TransactionInfo绑定到线程, 即使没有在这里创建新事务.
		// 这保证了即使此切面没有创建任何事务, 也将正确管理TransactionInfo堆栈.
		txInfo.bindToThread();
		return txInfo;
	}

	/**
	 * 成功完成调用后执行, 但在处理异常后不执行.
	 * 如果没有创建事务, 则不执行任何操作.
	 * 
	 * @param txInfo 有关当前事务的信息
	 */
	protected void commitTransactionAfterReturning(TransactionInfo txInfo) {
		if (txInfo != null && txInfo.hasTransaction()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
			}
			txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
		}
	}

	/**
	 * 处理一个throwable, 完成事务.
	 * 可能会提交或回滚, 具体取决于配置.
	 * 
	 * @param txInfo 有关当前事务的信息
	 * @param ex 遇到的Throwable
	 */
	protected void completeTransactionAfterThrowing(TransactionInfo txInfo, Throwable ex) {
		if (txInfo != null && txInfo.hasTransaction()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
						"] after exception: " + ex);
			}
			if (txInfo.transactionAttribute.rollbackOn(ex)) {
				try {
					txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
				}
				catch (TransactionSystemException ex2) {
					logger.error("Application exception overridden by rollback exception", ex);
					ex2.initApplicationException(ex);
					throw ex2;
				}
				catch (RuntimeException ex2) {
					logger.error("Application exception overridden by rollback exception", ex);
					throw ex2;
				}
				catch (Error err) {
					logger.error("Application exception overridden by rollback error", ex);
					throw err;
				}
			}
			else {
				// 不会回滚此异常.
				// 如果TransactionStatus.isRollbackOnly()为 true, 仍会回滚.
				try {
					txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
				}
				catch (TransactionSystemException ex2) {
					logger.error("Application exception overridden by commit exception", ex);
					ex2.initApplicationException(ex);
					throw ex2;
				}
				catch (RuntimeException ex2) {
					logger.error("Application exception overridden by commit exception", ex);
					throw ex2;
				}
				catch (Error err) {
					logger.error("Application exception overridden by commit error", ex);
					throw err;
				}
			}
		}
	}

	/**
	 * 重置TransactionInfo ThreadLocal.
	 * <p>在所有情况下都要调用它: 异常或正常返回!
	 * 
	 * @param txInfo 有关当前事务的信息 (may be {@code null})
	 */
	protected void cleanupTransactionInfo(TransactionInfo txInfo) {
		if (txInfo != null) {
			txInfo.restoreThreadLocalStatus();
		}
	}


	/**
	 * 用于保存事务信息的不透明对象. 子类必须将它传递回此类的方法, 但不能看到它的内部.
	 */
	protected final class TransactionInfo {

		private final PlatformTransactionManager transactionManager;

		private final TransactionAttribute transactionAttribute;

		private final String joinpointIdentification;

		private TransactionStatus transactionStatus;

		private TransactionInfo oldTransactionInfo;

		public TransactionInfo(PlatformTransactionManager transactionManager,
				TransactionAttribute transactionAttribute, String joinpointIdentification) {

			this.transactionManager = transactionManager;
			this.transactionAttribute = transactionAttribute;
			this.joinpointIdentification = joinpointIdentification;
		}

		public PlatformTransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		public TransactionAttribute getTransactionAttribute() {
			return this.transactionAttribute;
		}

		/**
		 * 返回此连接点的String表示形式 (通常是Method调用) 以用于日志记录.
		 */
		public String getJoinpointIdentification() {
			return this.joinpointIdentification;
		}

		public void newTransactionStatus(TransactionStatus status) {
			this.transactionStatus = status;
		}

		public TransactionStatus getTransactionStatus() {
			return this.transactionStatus;
		}

		/**
		 * 返回事务是否由此切面创建, 或者是否只有一个占位符来保持ThreadLocal堆栈的完整性.
		 */
		public boolean hasTransaction() {
			return (this.transactionStatus != null);
		}

		private void bindToThread() {
			// 公开当前的TransactionStatus, 在此事务完成后, 保留任何现有的TransactionStatus以进行恢复.
			this.oldTransactionInfo = transactionInfoHolder.get();
			transactionInfoHolder.set(this);
		}

		private void restoreThreadLocalStatus() {
			// 使用堆栈恢复旧事务TransactionInfo.
			// 如果没有设置, 则为null.
			transactionInfoHolder.set(this.oldTransactionInfo);
		}

		@Override
		public String toString() {
			return this.transactionAttribute.toString();
		}
	}


	/**
	 * 用于继续目标调用的简单回调接口.
	 * 具体的拦截器/切面使其适配其调用机制.
	 */
	protected interface InvocationCallback {

		Object proceedWithInvocation() throws Throwable;
	}


	/**
	 * 回调事务模型中Throwable的内部持有者类.
	 */
	private static class ThrowableHolder {

		public Throwable throwable;
	}


	/**
	 * Throwable的内部保存器类, 用作从TransactionCallback抛出的RuntimeException (随后再次解包).
	 */
	@SuppressWarnings("serial")
	private static class ThrowableHolderException extends RuntimeException {

		public ThrowableHolderException(Throwable throwable) {
			super(throwable);
		}

		@Override
		public String toString() {
			return getCause().toString();
		}
	}
}
