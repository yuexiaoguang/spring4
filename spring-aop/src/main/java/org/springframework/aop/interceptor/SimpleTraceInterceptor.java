package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

/**
 * AOP Alliance {@code MethodInterceptor}, 可以在链中引入, 以显示有关拦截的方法调用的详细跟踪信息, 包括方法进入和方法退出信息.
 *
 * <p>考虑使用{@code CustomizableTraceInterceptor}来满足更高级的需求.
 */
@SuppressWarnings("serial")
public class SimpleTraceInterceptor extends AbstractTraceInterceptor {

	public SimpleTraceInterceptor() {
	}

	/**
	 * @param useDynamicLogger 使用动态记录器, 还是静态记录器
	 */
	public SimpleTraceInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}


	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String invocationDescription = getInvocationDescription(invocation);
		writeToLog(logger, "Entering " + invocationDescription);
		try {
			Object rval = invocation.proceed();
			writeToLog(logger, "Exiting " + invocationDescription);
			return rval;
		}
		catch (Throwable ex) {
			writeToLog(logger, "Exception thrown in " + invocationDescription, ex);
			throw ex;
		}
	}

	/**
	 * 返回给定方法调用的描述.
	 * 
	 * @param invocation 要描述的调用
	 * 
	 * @return 描述
	 */
	protected String getInvocationDescription(MethodInvocation invocation) {
		return "method '" + invocation.getMethod().getName() + "' of class [" +
				invocation.getThis().getClass().getName() + "]";
	}

}
