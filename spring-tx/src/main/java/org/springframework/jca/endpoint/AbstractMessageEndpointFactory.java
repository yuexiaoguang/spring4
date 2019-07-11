package org.springframework.jca.endpoint;

import java.lang.reflect.Method;
import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.transaction.jta.SimpleTransactionFactory;
import org.springframework.transaction.jta.TransactionFactory;

/**
 * JCA 1.5/1.6/1.7 {@link javax.resource.spi.endpoint.MessageEndpointFactory}接口的抽象基础实现,
 * 提供事务管理功能以及端点调用的ClassLoader.
 */
public abstract class AbstractMessageEndpointFactory implements MessageEndpointFactory, BeanNameAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private TransactionFactory transactionFactory;

	private String transactionName;

	private int transactionTimeout = -1;

	private String beanName;


	/**
	 * 将XA事务管理器设置为用于包装端点调用, 在每个此类事务中登记端点资源.
	 * <p>传入的对象可以是实现Spring的{@link org.springframework.transaction.jta.TransactionFactory}接口的事务管理器,
	 * 也可以是普通的{@link javax.transaction.TransactionManager}.
	 * <p>如果未指定事务管理器, 则端点调用将不会包装在XA事务中.
	 * 查看资源提供商的ActivationSpec文档, 了解特定提供商的本地事务选项.
	 */
	public void setTransactionManager(Object transactionManager) {
		if (transactionManager instanceof TransactionFactory) {
			this.transactionFactory = (TransactionFactory) transactionManager;
		}
		else if (transactionManager instanceof TransactionManager) {
			this.transactionFactory = new SimpleTransactionFactory((TransactionManager) transactionManager);
		}
		else {
			throw new IllegalArgumentException("Transaction manager [" + transactionManager +
					"] is neither a [org.springframework.transaction.jta.TransactionFactory} nor a " +
					"[javax.transaction.TransactionManager]");
		}
	}

	/**
	 * 设置用于包装端点调用的Spring TransactionFactory, 在每个此类事务中登记端点资源.
	 * <p>或者, 通过{@link #setTransactionManager "transactionManager"}属性指定适当的事务管理器.
	 * <p>如果未指定事务工厂, 则端点调用将不会包装在XA事务中.
	 * 查看资源提供商的ActivationSpec文档, 了解特定提供商的本地事务选项.
	 */
	public void setTransactionFactory(TransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
	}

	/**
	 * 指定事务的名称.
	 * <p>默认无. 指定的名称将传递给事务管理器, 允许在事务监视器中标识事务.
	 */
	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}

	/**
	 * 指定事务超时.
	 * <p>默认 -1: 依赖事务管理器的默认超时.
	 * 指定具体超时以限制每个端点调用的最大持续时间.
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * 设置此消息端点的名称. 在Spring的bean工厂中定义时自动填充bean名称.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	/**
	 * JCA 1.7 {@code #getActivationName()}方法的实现, 返回此MessageEndpointFactory上设置的bean名称.
	 */
	public String getActivationName() {
		return this.beanName;
	}

	/**
	 * 如果指定了事务管理器, 则此实现返回{@code true}; 否则{@code false}.
	 */
	@Override
	public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
		return (this.transactionFactory != null);
	}

	/**
	 * {@code createEndpoint}的标准JCA 1.5版本.
	 * <p>此实现委托给{@link #createEndpointInternal()}, 在调用端点之前初始化端点的XAResource.
	 */
	@Override
	public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
		AbstractMessageEndpoint endpoint = createEndpointInternal();
		endpoint.initXAResource(xaResource);
		return endpoint;
	}

	/**
	 * {@code createEndpoint}的替代JCA 1.6版本.
	 * <p>此实现委托给{@link #createEndpointInternal()}, 忽略指定的超时. 它只适用于JCA 1.6合规性.
	 */
	public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
		AbstractMessageEndpoint endpoint = createEndpointInternal();
		endpoint.initXAResource(xaResource);
		return endpoint;
	}

	/**
	 * 创建实际的端点实例, 作为此工厂的{@link AbstractMessageEndpoint}内部类的子类.
	 * 
	 * @return 实际的端点实例 (never {@code null})
	 * @throws UnavailableException 如果目前没有可用的端点
	 */
	protected abstract AbstractMessageEndpoint createEndpointInternal() throws UnavailableException;


	/**
	 * 实际端点实现的内部类, 基于模板方法, 允许任何类型的具体端点实现.
	 */
	protected abstract class AbstractMessageEndpoint implements MessageEndpoint {

		private TransactionDelegate transactionDelegate;

		private boolean beforeDeliveryCalled = false;

		private ClassLoader previousContextClassLoader;

		/**
		 * 初始化此端点的TransactionDelegate.
		 * 
		 * @param xaResource 此端点的XAResource
		 */
		void initXAResource(XAResource xaResource) {
			this.transactionDelegate = new TransactionDelegate(xaResource);
		}

		/**
		 * 如果需要, 此{@code beforeDelivery}实现启动事务, 并将端点ClassLoader公开为当前线程上下文ClassLoader.
		 * <p>请注意, JCA 1.5规范在调用具体端点之前, 不需要ResourceAdapter来调用此方法.
		 * 如果未调用此方法 (检查{@link #hasBeforeDeliveryBeenCalled()}),
		 * 具体的端点方法应显式调用{@code beforeDelivery}及其兄弟{@link #afterDelivery()}, 作为其自身处理的一部分.
		 */
		@Override
		public void beforeDelivery(Method method) throws ResourceException {
			this.beforeDeliveryCalled = true;
			try {
				this.transactionDelegate.beginTransaction();
			}
			catch (Throwable ex) {
				throw new ApplicationServerInternalException("Failed to begin transaction", ex);
			}
			Thread currentThread = Thread.currentThread();
			this.previousContextClassLoader = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(getEndpointClassLoader());
		}

		/**
		 * 用于公开端点的ClassLoader的模板方法 (通常是已加载消息监听器类的ClassLoader).
		 * 
		 * @return 端点ClassLoader (never {@code null})
		 */
		protected abstract ClassLoader getEndpointClassLoader();

		/**
		 * 返回是否已调用此端点的{@link #beforeDelivery}方法.
		 */
		protected final boolean hasBeforeDeliveryBeenCalled() {
			return this.beforeDeliveryCalled;
		}

		/**
		 * 用于通知端点基类具体的端点调用导致异常的回调方法.
		 * <p>在具体端点抛出异常的情况下由子类调用.
		 * 
		 * @param ex 从具体的端点抛出的异常
		 */
		protected final void onEndpointException(Throwable ex) {
			this.transactionDelegate.setRollbackOnly();
			logger.debug("Transaction marked as rollback-only after endpoint exception", ex);
		}

		/**
		 * 此{@code afterDelivery}实现重置线程上下文ClassLoader, 并完成事务.
		 * <p>请注意, JCA 1.5规范在调用具体端点后, 不需要ResourceAdapter来调用此方法.
		 * 请参阅{@link #beforeDelivery}的javadoc中的说明.
		 */
		@Override
		public void afterDelivery() throws ResourceException {
			this.beforeDeliveryCalled = false;
			Thread.currentThread().setContextClassLoader(this.previousContextClassLoader);
			this.previousContextClassLoader = null;
			try {
				this.transactionDelegate.endTransaction();
			}
			catch (Throwable ex) {
				logger.warn("Failed to complete transaction after endpoint delivery", ex);
				throw new ApplicationServerInternalException("Failed to complete transaction", ex);
			}
		}

		@Override
		public void release() {
			try {
				this.transactionDelegate.setRollbackOnly();
				this.transactionDelegate.endTransaction();
			}
			catch (Throwable ex) {
				logger.warn("Could not complete unfinished transaction on endpoint release", ex);
			}
		}
	}


	/**
	 * 执行实际事务处理的私有内部类, 包括端点的XAResource的登记.
	 */
	private class TransactionDelegate {

		private final XAResource xaResource;

		private Transaction transaction;

		private boolean rollbackOnly;

		public TransactionDelegate(XAResource xaResource) {
			if (xaResource == null && transactionFactory != null &&
					!transactionFactory.supportsResourceAdapterManagedTransactions()) {
				throw new IllegalStateException("ResourceAdapter-provided XAResource is required for " +
						"transaction management. Check your ResourceAdapter's configuration.");
			}
			this.xaResource = xaResource;
		}

		public void beginTransaction() throws Exception {
			if (transactionFactory != null && this.xaResource != null) {
				this.transaction = transactionFactory.createTransaction(transactionName, transactionTimeout);
				this.transaction.enlistResource(this.xaResource);
			}
		}

		public void setRollbackOnly() {
			if (this.transaction != null) {
				this.rollbackOnly = true;
			}
		}

		public void endTransaction() throws Exception {
			if (this.transaction != null) {
				try {
					if (this.rollbackOnly) {
						this.transaction.rollback();
					}
					else {
						this.transaction.commit();
					}
				}
				finally {
					this.transaction = null;
					this.rollbackOnly = false;
				}
			}
		}
	}
}
