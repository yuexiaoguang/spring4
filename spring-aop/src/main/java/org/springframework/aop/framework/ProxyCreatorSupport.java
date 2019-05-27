package org.springframework.aop.framework;

import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * 代理工厂的基类.
 * 提供对可配置的AopProxyFactory的便捷访问.
 */
@SuppressWarnings("serial")
public class ProxyCreatorSupport extends AdvisedSupport {

	private AopProxyFactory aopProxyFactory;

	private final List<AdvisedSupportListener> listeners = new LinkedList<AdvisedSupportListener>();

	/** 设置为 true, 当创建第一个AOP代理时 */
	private boolean active = false;


	public ProxyCreatorSupport() {
		this.aopProxyFactory = new DefaultAopProxyFactory();
	}

	/**
	 * @param aopProxyFactory 要使用的AopProxyFactory
	 */
	public ProxyCreatorSupport(AopProxyFactory aopProxyFactory) {
		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
		this.aopProxyFactory = aopProxyFactory;
	}


	/**
	 * 自定义AopProxyFactory, 允许在不改变核心框架的情况下放弃不同的策略.
	 * <p>默认是 {@link DefaultAopProxyFactory}, 根据需求使用动态JDK代理或CGLIB代理.
	 */
	public void setAopProxyFactory(AopProxyFactory aopProxyFactory) {
		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
		this.aopProxyFactory = aopProxyFactory;
	}

	/**
	 * 返回此ProxyConfig使用的AopProxyFactory.
	 */
	public AopProxyFactory getAopProxyFactory() {
		return this.aopProxyFactory;
	}

	/**
	 * 将给定的AdvisedSupportListener添加到此代理配置中.
	 * 
	 * @param listener 要注册的监听器
	 */
	public void addListener(AdvisedSupportListener listener) {
		Assert.notNull(listener, "AdvisedSupportListener must not be null");
		this.listeners.add(listener);
	}

	/**
	 * 从此代理配置中删除给定的AdvisedSupportListener.
	 * 
	 * @param listener 要注销的监听器
	 */
	public void removeListener(AdvisedSupportListener listener) {
		Assert.notNull(listener, "AdvisedSupportListener must not be null");
		this.listeners.remove(listener);
	}


	/**
	 * 子类应该调用它来获取新的AOP代理. 不应该创建一个以{@code this}作为参数的AOP代理.
	 */
	protected final synchronized AopProxy createAopProxy() {
		if (!this.active) {
			activate();
		}
		return getAopProxyFactory().createAopProxy(this);
	}

	/**
	 * 激活此代理配置.
	 */
	private void activate() {
		this.active = true;
		for (AdvisedSupportListener listener : this.listeners) {
			listener.activated(this);
		}
	}

	/**
	 * 将增强更改事件传播到所有AdvisedSupportListener.
	 */
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		synchronized (this) {
			if (this.active) {
				for (AdvisedSupportListener listener : this.listeners) {
					listener.adviceChanged(this);
				}
			}
		}
	}

	/**
	 * 子类可以调用它来检查是否已经创建了AOP代理.
	 */
	protected final synchronized boolean isActive() {
		return this.active;
	}

}
