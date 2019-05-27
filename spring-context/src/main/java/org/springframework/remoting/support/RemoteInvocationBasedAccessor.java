package org.springframework.remoting.support;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 远程服务访问器的抽象基类, 它基于{@link RemoteInvocation}对象的序列化.
 *
 * 提供"remoteInvocationFactory"属性, 默认策略为{@link DefaultRemoteInvocationFactory}.
 */
public abstract class RemoteInvocationBasedAccessor extends UrlBasedRemoteAccessor {

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();


	/**
	 * 设置用于此访问器的RemoteInvocationFactory.
	 * 默认是{@link DefaultRemoteInvocationFactory}.
	 * <p>自定义调用工厂可以向调用添加更多上下文信息, 例如用户凭据.
	 */
	public void setRemoteInvocationFactory(RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory =
				(remoteInvocationFactory != null ? remoteInvocationFactory : new DefaultRemoteInvocationFactory());
	}

	/**
	 * 返回此访问器使用的RemoteInvocationFactory.
	 */
	public RemoteInvocationFactory getRemoteInvocationFactory() {
		return this.remoteInvocationFactory;
	}

	/**
	 * 为给定的AOP方法调用创建一个新的RemoteInvocation对象.
	 * <p>默认实现委托给配置的{@link #setRemoteInvocationFactory RemoteInvocationFactory}.
	 * 这可以在子类中重写, 以便提供自定义RemoteInvocation子类, 包含其他调用参数 (e.g. 用户凭据).
	 * <p>请注意, 最好将自定义RemoteInvocationFactory构建为可重用策略, 而不是覆盖此方法.
	 * 
	 * @param methodInvocation 当前的AOP方法调用
	 * 
	 * @return RemoteInvocation对象
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return getRemoteInvocationFactory().createRemoteInvocation(methodInvocation);
	}

	/**
	 * 重新创建给定的RemoteInvocationResult对象中包含的调用结果.
	 * <p>默认实现调用默认的 {@code recreate()}方法.
	 * 这可以在子类中重写以提供自定义重新创建, 从而可能处理返回的结果对象.
	 * 
	 * @param result 要重新创建的RemoteInvocationResult
	 * 
	 * @return 如果调用结果是成功返回, 则返回值
	 * @throws Throwable 如果调用结果是异常
	 */
	protected Object recreateRemoteInvocationResult(RemoteInvocationResult result) throws Throwable {
		return result.recreate();
	}

}
