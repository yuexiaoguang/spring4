package org.springframework.remoting.support;

import java.lang.reflect.InvocationTargetException;

/**
 * 远程服务导出器的抽象基类, 它基于{@link RemoteInvocation}对象的反序列化.
 *
 * <p>提供"remoteInvocationExecutor"属性, 默认策略为{@link DefaultRemoteInvocationExecutor}.
 */
public abstract class RemoteInvocationBasedExporter extends RemoteExporter {

	private RemoteInvocationExecutor remoteInvocationExecutor = new DefaultRemoteInvocationExecutor();


	/**
	 * 设置用于此导出器的RemoteInvocationExecutor.
	 * 默认是 DefaultRemoteInvocationExecutor.
	 * <p>自定义调用执行器可以从调用中提取更多上下文信息，例如用户凭证.
	 */
	public void setRemoteInvocationExecutor(RemoteInvocationExecutor remoteInvocationExecutor) {
		this.remoteInvocationExecutor = remoteInvocationExecutor;
	}

	/**
	 * 返回此导出器使用的RemoteInvocationExecutor.
	 */
	public RemoteInvocationExecutor getRemoteInvocationExecutor() {
		return this.remoteInvocationExecutor;
	}


	/**
	 * 将给定的远程调用应用于给定的目标对象.
	 * 默认实现委托给RemoteInvocationExecutor.
	 * <p>可以在子类中重写自定义调用行为, 可能用于从自定义RemoteInvocation子类应用其他调用参数.
	 * 请注意, 最好使用自定义RemoteInvocationExecutor, 这是一种可重用的策略.
	 * 
	 * @param invocation 远程调用
	 * @param targetObject 要调用的目标对象
	 * 
	 * @return 调用结果
	 * @throws NoSuchMethodException 如果方法名称无法解析
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 */
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (logger.isTraceEnabled()) {
			logger.trace("Executing " + invocation);
		}
		try {
			return getRemoteInvocationExecutor().invoke(invocation, targetObject);
		}
		catch (NoSuchMethodException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not find target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (IllegalAccessException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (InvocationTargetException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Target method failed for " + invocation, ex.getTargetException());
			}
			throw ex;
		}
	}

	/**
	 * 将给定的远程调用应用于给定的目标对象, 将调用结果包装在可序列化的RemoteInvocationResult对象中.
	 * 默认实现创建一个普通的RemoteInvocationResult.
	 * <p>可以在子类中重写自定义调用行为, 例如返回其他上下文信息.
	 * 请注意, RemoteInvocationExecutor策略不包含此内容!
	 * 
	 * @param invocation 远程调用
	 * @param targetObject 要调用的目标对象
	 * 
	 * @return 调用结果
	 */
	protected RemoteInvocationResult invokeAndCreateResult(RemoteInvocation invocation, Object targetObject) {
		try {
			Object value = invoke(invocation, targetObject);
			return new RemoteInvocationResult(value);
		}
		catch (Throwable ex) {
			return new RemoteInvocationResult(ex);
		}
	}

}
