package org.springframework.aop.interceptor;

import com.jamonapi.MonKey;
import com.jamonapi.MonKeyImp;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import com.jamonapi.utils.Misc;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

/**
 * 性能监视器拦截器, 使用<b>JAMon</b>库对拦截的方法执行性能测量并输出统计信息.
 * 此外, 它跟踪/计算拦截方法抛出的异常. 可以在JAMon Web应用程序中查看堆栈跟踪.
 *
 * <p>此代码的灵感来自Thierry Templier的博客.
 */
@SuppressWarnings("serial")
public class JamonPerformanceMonitorInterceptor extends AbstractMonitoringInterceptor {

	private boolean trackAllInvocations = false;


	/**
	 * 使用静态记录器.
	 */
	public JamonPerformanceMonitorInterceptor() {
	}

	/**
	 * @param useDynamicLogger 使用动态记录器, 还是静态记录器
	 */
	public JamonPerformanceMonitorInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}

	/**
	 * @param useDynamicLogger 使用动态记录器, 还是静态记录器
	 * @param trackAllInvocations 是否跟踪通过此拦截器的所有调用, 或者只是启用跟踪日志记录的调用
	 */
	public JamonPerformanceMonitorInterceptor(boolean useDynamicLogger, boolean trackAllInvocations) {
		setUseDynamicLogger(useDynamicLogger);
		setTrackAllInvocations(trackAllInvocations);
	}


	/**
	 * 是否跟踪通过此拦截器的所有调用, 或者只是启用跟踪日志记录的调用.
	 * <p>默认是"false": 仅监视启用了跟踪日志记录的调用.
	 * 指定为"true"让JAMon跟踪所有调用, 即使禁用跟踪日志记录, 也会收集统计信息.
	 */
	public void setTrackAllInvocations(boolean trackAllInvocations) {
		this.trackAllInvocations = trackAllInvocations;
	}


	/**
	 * 如果已设置“trackAllInvocations”标志，则始终应用拦截器; 如果启用了日志，则只需启动即可.
	 */
	@Override
	protected boolean isInterceptorEnabled(MethodInvocation invocation, Log logger) {
		return (this.trackAllInvocations || isLogEnabled(logger));
	}

	/**
	 * 使用JAMon Monitor包装调用, 并将当前性能统计信息写入日志 (如果启用了).
	 */
	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String name = createInvocationTraceName(invocation);
		MonKey key = new MonKeyImp(name, name, "ms.");

		Monitor monitor = MonitorFactory.start(key);
		try {
			return invocation.proceed();
		}
		catch (Throwable ex) {
			trackException(key, ex);
			throw ex;
		}
		finally {
			monitor.stop();
			if (!this.trackAllInvocations || isLogEnabled(logger)) {
				writeToLog(logger, "JAMon performance statistics for method [" + name + "]:\n" + monitor);
			}
		}
	}

	/**
	 * 计算抛出的异常并将堆栈跟踪放入key的详细信息部分.
	 * 这将允许在JAMon Web应用程序中查看堆栈跟踪.
	 */
	protected void trackException(MonKey key, Throwable ex) {
		String stackTrace = "stackTrace=" + Misc.getExceptionTrace(ex);
		key.setDetails(stackTrace);

		// 特殊异常计数器. Example: java.lang.RuntimeException
		MonitorFactory.add(new MonKeyImp(ex.getClass().getName(), stackTrace, "Exception"), 1);

		// 一般异常计数器，它是所有抛出异常的总计
		MonitorFactory.add(new MonKeyImp(MonitorFactory.EXCEPTIONS_LABEL, stackTrace, "Exception"), 1);
	}

}
