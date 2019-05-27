package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

import org.springframework.util.StopWatch;

/**
 * AOP Alliance {@code MethodInterceptor}, 用于性能监控.
 * 此拦截器对拦截的方法调用没有影响.
 *
 * <p>使用{@code StopWatch}进行实际性能测量.
 */
@SuppressWarnings("serial")
public class PerformanceMonitorInterceptor extends AbstractMonitoringInterceptor {

	/**
	 * 使用静态记录器.
	 */
	public PerformanceMonitorInterceptor() {
	}

	/**
	 * @param useDynamicLogger 使用动态记录器, 还是静态记录器
	 */
	public PerformanceMonitorInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}


	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String name = createInvocationTraceName(invocation);
		StopWatch stopWatch = new StopWatch(name);
		stopWatch.start(name);
		try {
			return invocation.proceed();
		}
		finally {
			stopWatch.stop();
			writeToLog(logger, stopWatch.shortSummary());
		}
	}

}
