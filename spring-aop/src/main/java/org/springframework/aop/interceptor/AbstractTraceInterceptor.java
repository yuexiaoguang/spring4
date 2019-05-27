package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;

/**
 * 用于跟踪的基础{@code MethodInterceptor}实现.
 *
 * <p>默认情况下, 日志消息被写入拦截器类的日志中, 而不是被拦截的类.
 * 将{@code useDynamicLogger} bean属性设置为{@code true}会导致所有日志消息被写入{@code Log}以拦截目标类.
 *
 * <p>子类必须实现{@code invokeUnderTrace}方法, 只有在应该跟踪特定的调用时才由此类调用.
 * 子类应写入提供的{@code Log}实例.
 */
@SuppressWarnings("serial")
public abstract class AbstractTraceInterceptor implements MethodInterceptor, Serializable {

	/**
	 * 用于写入跟踪消息的默认{@code Log}实例.
	 * 此实例映射到实现{@code Class}.
	 */
	protected transient Log defaultLogger = LogFactory.getLog(getClass());

	/**
	 * 在使用动态记录器时是否应隐藏代理类名称.
	 */
	private boolean hideProxyClassNames = false;

	/**
	 * 是否将异常传递给记录器.
	 */
	private boolean logExceptionStackTrace = true;


	/**
	 * 设置是使用动态记录器还是静态记录器.
	 * 默认是此跟踪拦截器的静态记录器.
	 * <p>用于确定应使用哪个{@code Log}实例为特定方法调用编写日志消息:
	 * 一个动态的对于被调用的{@code Class}, 或者一个静态的对于跟踪拦截器的 {@code Class}.
	 * <p><b>NOTE:</b> 指定这个属性或 "loggerName", 而不是两个都指定.
	 */
	public void setUseDynamicLogger(boolean useDynamicLogger) {
		// Release default logger if it is not being used.
		this.defaultLogger = (useDynamicLogger ? null : LogFactory.getLog(getClass()));
	}

	/**
	 * 设置要使用的记录器的名称. 该名称将通过Commons Logging传递给底层记录器实现, 根据记录器的配置将其解释为日志类别.
	 * <p>可以指定日志不使用类的类别 (这个拦截器的类或类是否被调用), 而是进入特定的命名类别.
	 * <p><b>NOTE:</b> 指定这个属性或 "useDynamicLogger", 而不是两个都指定.
	 */
	public void setLoggerName(String loggerName) {
		this.defaultLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * 设置为 "true", 让 {@link #setUseDynamicLogger dynamic loggers}尽可能隐藏代理类名.
	 * 默认是 "false".
	 */
	public void setHideProxyClassNames(boolean hideProxyClassNames) {
		this.hideProxyClassNames = hideProxyClassNames;
	}

	/**
	 * 设置是否将异常传递给记录器, 建议将其堆栈跟踪包含在日志中.
	 * 默认是 "true"; 设置为 "false"以便将日志输出减少到仅有跟踪消息 (其中可能包括异常类名和异常消息, 如果适用的话).
	 * @since 4.3.10
	 */
	public void setLogExceptionStackTrace(boolean logExceptionStackTrace) {
		this.logExceptionStackTrace = logExceptionStackTrace;
	}


	/**
	 * 确定是否为特定{@code MethodInvocation}启用了日志记录.
	 * 如果没有, 方法调用正常进行, 否则, 方法调用将传递给{@code invokeUnderTrace}方法进行处理.
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Log logger = getLoggerForInvocation(invocation);
		if (isInterceptorEnabled(invocation, logger)) {
			return invokeUnderTrace(invocation, logger);
		}
		else {
			return invocation.proceed();
		}
	}

	/**
	 * 返回适当的{@code Log}实例以用于给定的{@code MethodInvocation}.
	 * 如果设置了{@code useDynamicLogger}标志, {@code Log}实例将用于{@code MethodInvocation}的目标类,
	 * 否则{@code Log}将成为默认的静态记录器.
	 * 
	 * @param invocation 被跟踪的{@code MethodInvocation}
	 * 
	 * @return 要使用的{@code Log}实例
	 */
	protected Log getLoggerForInvocation(MethodInvocation invocation) {
		if (this.defaultLogger != null) {
			return this.defaultLogger;
		}
		else {
			Object target = invocation.getThis();
			return LogFactory.getLog(getClassForLogging(target));
		}
	}

	/**
	 * 确定用于记录目的的类.
	 * 
	 * @param target 要反射的目标对象
	 * 
	 * @return 给定对象的目标类
	 */
	protected Class<?> getClassForLogging(Object target) {
		return (this.hideProxyClassNames ? AopUtils.getTargetClass(target) : target.getClass());
	}

	/**
	 * 确定拦截器是否应该启动, 即, 是否应该调用{@code invokeUnderTrace}方法.
	 * <p>默认行为是检查是否启用了给定的{@code Log}实例. 子类可以覆盖它以在其他情况下应用拦截器.
	 * 
	 * @param invocation 被跟踪的{@code MethodInvocation}
	 * @param logger 要检查的{@code Log}实例
	 */
	protected boolean isInterceptorEnabled(MethodInvocation invocation, Log logger) {
		return isLogEnabled(logger);
	}

	/**
	 * 确定是否启用了给定的{@link Log}实例.
	 * <p>默认是 {@code true}, 当启用"trace"级别时.
	 * 子类可以覆盖它以更改“trace”发生的级别.
	 * 
	 * @param logger 要检查的{@code Log}实例
	 */
	protected boolean isLogEnabled(Log logger) {
		return logger.isTraceEnabled();
	}

	/**
	 * 将提供的跟踪消息写入提供的{@code Log}实例.
	 * <p>要被{@link #invokeUnderTrace}调用以进入/退出消息.
	 * <p>委托给 {@link #writeToLog(Log, String, Throwable)}作为控制底层记录器调用的最终委托.
	 * @since 4.3.10
	 */
	protected void writeToLog(Log logger, String message) {
		writeToLog(logger, message, null);
	}

	/**
	 * 将提供的跟踪消息和{@link Throwable}写入提供的{@code Log}实例.
	 * <p>要被{@link #invokeUnderTrace}调用以进入/退出结果, 可能包括异常.
	 * 请注意，{@link #setLogExceptionStackTrace}为 "false"时, 不会记录异常的堆栈跟踪.
	 * <p>默认情况下，消息以{@code TRACE}级别写入. 子类可以重写此方法以控制写入消息的级别,
	 * 通常也会相应地覆盖{@link #isLogEnabled}.
	 * @since 4.3.10
	 */
	protected void writeToLog(Log logger, String message, Throwable ex) {
		if (ex != null && this.logExceptionStackTrace) {
			logger.trace(message, ex);
		}
		else {
			logger.trace(message);
		}
	}


	/**
	 * 子类必须覆盖此方法以围绕提供的{@code MethodInvocation}执行任何跟踪.
	 * 子类负责通过调用{@code MethodInvocation.proceed()}来确保{@code MethodInvocation}实际执行.
	 * <p>默认情况下, 传入的{@code Log}实例将启用日志级别"trace".
	 * 子类不必再次检查, 除非他们覆盖{@code isInterceptorEnabled}方法来修改默认行为, 并且可以委托{@code writeToLog}来写入要写入的实际消息.
	 * 
	 * @param logger 用于写入跟踪消息的{@code Log}
	 * 
	 * @return 调用{@code MethodInvocation.proceed()}的结果
	 * @throws Throwable 如果调用{@code MethodInvocation.proceed()}遇到错误
	 */
	protected abstract Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable;

}
