package org.springframework.transaction.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * 一个简单的事务支持的{@link Scope}实现, 委托给{@link TransactionSynchronizationManager}的资源绑定机制.
 *
 * <p><b>NOTE:</b> 与{@link org.springframework.context.support.SimpleThreadScope}一样,
 * 此事务范围默认情况下未在常见上下文中注册.
 * 相反, 需要通过
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory#registerScope}
 * 或通过{@link org.springframework.beans.factory.config.CustomScopeConfigurer} bean将其显式分配给设置中的范围键.
 */
public class SimpleTransactionScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		ScopedObjectsHolder scopedObjects = (ScopedObjectsHolder) TransactionSynchronizationManager.getResource(this);
		if (scopedObjects == null) {
			scopedObjects = new ScopedObjectsHolder();
			TransactionSynchronizationManager.registerSynchronization(new CleanupSynchronization(scopedObjects));
			TransactionSynchronizationManager.bindResource(this, scopedObjects);
		}
		Object scopedObject = scopedObjects.scopedInstances.get(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			scopedObjects.scopedInstances.put(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	public Object remove(String name) {
		ScopedObjectsHolder scopedObjects = (ScopedObjectsHolder) TransactionSynchronizationManager.getResource(this);
		if (scopedObjects != null) {
			scopedObjects.destructionCallbacks.remove(name);
			return scopedObjects.scopedInstances.remove(name);
		}
		else {
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		ScopedObjectsHolder scopedObjects = (ScopedObjectsHolder) TransactionSynchronizationManager.getResource(this);
		if (scopedObjects != null) {
			scopedObjects.destructionCallbacks.put(name, callback);
		}
	}

	@Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return TransactionSynchronizationManager.getCurrentTransactionName();
	}


	static class ScopedObjectsHolder {

		final Map<String, Object> scopedInstances = new HashMap<String, Object>();

		final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<String, Runnable>();
	}


	private class CleanupSynchronization extends TransactionSynchronizationAdapter {

		private final ScopedObjectsHolder scopedObjects;

		public CleanupSynchronization(ScopedObjectsHolder scopedObjects) {
			this.scopedObjects = scopedObjects;
		}

		@Override
		public void suspend() {
			TransactionSynchronizationManager.unbindResource(SimpleTransactionScope.this);
		}

		@Override
		public void resume() {
			TransactionSynchronizationManager.bindResource(SimpleTransactionScope.this, this.scopedObjects);
		}

		@Override
		public void afterCompletion(int status) {
			TransactionSynchronizationManager.unbindResourceIfPossible(SimpleTransactionScope.this);
			for (Runnable callback : this.scopedObjects.destructionCallbacks.values()) {
				callback.run();
			}
			this.scopedObjects.destructionCallbacks.clear();
			this.scopedObjects.scopedInstances.clear();
		}
	}

}
