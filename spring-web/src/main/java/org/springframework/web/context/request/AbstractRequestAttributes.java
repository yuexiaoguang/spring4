package org.springframework.web.context.request;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * RequestAttributes实现的抽象支持类, 为特定于请求的销毁回调和更新访问的会话属性提供请求完成机制.
 */
public abstract class AbstractRequestAttributes implements RequestAttributes {

	/** 从属性名称String到销毁回调Runnable */
	protected final Map<String, Runnable> requestDestructionCallbacks = new LinkedHashMap<String, Runnable>(8);

	private volatile boolean requestActive = true;


	/**
	 * 发出请求已完成的信号.
	 * <p>执行所有请求销毁回调, 并更新在请求处理期间访问过的会话属性.
	 */
	public void requestCompleted() {
		executeRequestDestructionCallbacks();
		updateAccessedSessionAttributes();
		this.requestActive = false;
	}

	/**
	 * 确定原始请求是否仍处于活动状态.
	 */
	protected final boolean isRequestActive() {
		return this.requestActive;
	}

	/**
	 * 注册给定的回调, 以便在请求完成后执行.
	 * 
	 * @param name 注册回调的属性的名称
	 * @param callback 要进行销毁的回调
	 */
	protected final void registerRequestDestructionCallback(String name, Runnable callback) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(callback, "Callback must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.put(name, callback);
		}
	}

	/**
	 * 删除指定属性的请求销毁回调.
	 * 
	 * @param name 要删除回调的属性的名称
	 */
	protected final void removeRequestDestructionCallback(String name) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.remove(name);
		}
	}

	/**
	 * 请求完成后, 执行注册的所有回调.
	 */
	private void executeRequestDestructionCallbacks() {
		synchronized (this.requestDestructionCallbacks) {
			for (Runnable runnable : this.requestDestructionCallbacks.values()) {
				runnable.run();
			}
			this.requestDestructionCallbacks.clear();
		}
	}

	/**
	 * 更新在请求处理期间访问过的所有会话属性, 以将其可能更新的状态公开给底层会话管理器.
	 */
	protected abstract void updateAccessedSessionAttributes();

}
