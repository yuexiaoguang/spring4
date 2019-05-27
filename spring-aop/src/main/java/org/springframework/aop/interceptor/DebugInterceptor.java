package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP Alliance {@code MethodInterceptor}, 可以在链中引入, 以显示有关截获的调用的详细信息到记录器.
 *
 * <p>记录方法入口和方法出口的完整调用详细信息, 包括调用参数和调用计数.
 * 这仅用于调试目的; 使用{@code SimpleTraceInterceptor}或{@code CustomizableTraceInterceptor}进行纯追踪.
 */
@SuppressWarnings("serial")
public class DebugInterceptor extends SimpleTraceInterceptor {

	private volatile long count;


	public DebugInterceptor() {
	}

	/**
	 * @param useDynamicLogger 使用动态记录器, 还是静态记录器
	 */
	public DebugInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		synchronized (this) {
			this.count++;
		}
		return super.invoke(invocation);
	}

	@Override
	protected String getInvocationDescription(MethodInvocation invocation) {
		return invocation + "; count=" + this.count;
	}


	/**
	 * 返回此拦截器被调用的次数.
	 */
	public long getCount() {
		return this.count;
	}

	/**
	 * 将调用计数重置为零.
	 */
	public synchronized void resetCount() {
		this.count = 0;
	}

}
