package org.springframework.transaction.support;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.core.InfrastructureProxy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 用于在所有当前注册的同步上触发特定{@link TransactionSynchronization}回调方法的工具方法.
 */
public abstract class TransactionSynchronizationUtils {

	private static final Log logger = LogFactory.getLog(TransactionSynchronizationUtils.class);

	private static final boolean aopAvailable = ClassUtils.isPresent(
			"org.springframework.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());


	/**
	 * 检查给定的资源事务管理器是否引用给定 (底层)资源工厂.
	 */
	public static boolean sameResourceFactory(ResourceTransactionManager tm, Object resourceFactory) {
		return unwrapResourceIfNecessary(tm.getResourceFactory()).equals(unwrapResourceIfNecessary(resourceFactory));
	}

	/**
	 * 必要时展开给定的资源句柄; 否则按原样返回给定的句柄.
	 */
	static Object unwrapResourceIfNecessary(Object resource) {
		Assert.notNull(resource, "Resource must not be null");
		Object resourceRef = resource;
		// 解包基础结构代理
		if (resourceRef instanceof InfrastructureProxy) {
			resourceRef = ((InfrastructureProxy) resourceRef).getWrappedObject();
		}
		if (aopAvailable) {
			// 现在解包scoped代理
			resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
		}
		return resourceRef;
	}


	/**
	 * 在所有当前注册的同步上触发{@code flush}回调.
	 * 
	 * @throws RuntimeException 如果被{@code flush}回调抛出
	 */
	public static void triggerFlush() {
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.flush();
		}
	}

	/**
	 * 在所有当前注册的同步上触发{@code beforeCommit}回调.
	 * 
	 * @param readOnly 是否将事务定义为只读事务
	 * 
	 * @throws RuntimeException 如果被{@code beforeCommit}回调抛出
	 */
	public static void triggerBeforeCommit(boolean readOnly) {
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.beforeCommit(readOnly);
		}
	}

	/**
	 * 在所有当前注册的同步上触发{@code beforeCompletion}回调.
	 */
	public static void triggerBeforeCompletion() {
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			try {
				synchronization.beforeCompletion();
			}
			catch (Throwable tsex) {
				logger.error("TransactionSynchronization.beforeCompletion threw exception", tsex);
			}
		}
	}

	/**
	 * 在所有当前注册的同步上触发{@code afterCommit}回调.
	 * 
	 * @throws RuntimeException 如果被{@code afterCommit}回调引发
	 */
	public static void triggerAfterCommit() {
		invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());
	}

	/**
	 * 实际调用给定的Spring TransactionSynchronization对象的{@code afterCommit}方法.
	 * 
	 * @param synchronizations TransactionSynchronization对象的列表
	 */
	public static void invokeAfterCommit(List<TransactionSynchronization> synchronizations) {
		if (synchronizations != null) {
			for (TransactionSynchronization synchronization : synchronizations) {
				synchronization.afterCommit();
			}
		}
	}

	/**
	 * 在所有当前注册的同步上触发{@code afterCompletion}回调.
	 * 
	 * @param completionStatus 根据TransactionSynchronization接口中的常量的完成状态
	 */
	public static void triggerAfterCompletion(int completionStatus) {
		List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
		invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * 实际调用给定Spring TransactionSynchronization对象的{@code afterCompletion}方法.
	 * 
	 * @param synchronizations TransactionSynchronization对象集合
	 * @param completionStatus 根据TransactionSynchronization接口中的常量的完成状态
	 */
	public static void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
		if (synchronizations != null) {
			for (TransactionSynchronization synchronization : synchronizations) {
				try {
					synchronization.afterCompletion(completionStatus);
				}
				catch (Throwable tsex) {
					logger.error("TransactionSynchronization.afterCompletion threw exception", tsex);
				}
			}
		}
	}


	/**
	 * 内部类, 以避免对AOP模块的硬编码依赖.
	 */
	private static class ScopedProxyUnwrapper {

		public static Object unwrapIfNecessary(Object resource) {
			if (resource instanceof ScopedObject) {
				return ((ScopedObject) resource).getTargetObject();
			}
			else {
				return resource;
			}
		}
	}
}
