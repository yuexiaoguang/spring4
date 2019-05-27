package org.springframework.remoting.support;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ClassUtils;

/**
 * 用于跟踪远程调用的AOP Alliance MethodInterceptor.
 * 由RemoteExporter及其子类自动应用.
 *
 * <p>DEBUG级别记录传入的远程调用以及已完成处理的远程调用.
 * 如果远程调用的处理导致受检异常, 则异常将记录在INFO级别;
 * 如果它导致未受检的异常 (或错误), 则异常将以WARN级别记录.
 *
 * <p>异常日志对于在服务器端保存堆栈跟踪信息特别有用, 而不仅仅是将异常传播到客户端 (可能会或可能不会正确记录).
 */
public class RemoteInvocationTraceInterceptor implements MethodInterceptor {

	protected static final Log logger = LogFactory.getLog(RemoteInvocationTraceInterceptor.class);

	private final String exporterNameClause;


	public RemoteInvocationTraceInterceptor() {
		this.exporterNameClause = "";
	}

	/**
	 * @param exporterName 远程导出器的名称 (用作日志消息中的上下文信息)
	 */
	public RemoteInvocationTraceInterceptor(String exporterName) {
		this.exporterNameClause = exporterName + " ";
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (logger.isDebugEnabled()) {
			logger.debug("Incoming " + this.exporterNameClause + "remote call: " +
					ClassUtils.getQualifiedMethodName(method));
		}
		try {
			Object retVal = invocation.proceed();
			if (logger.isDebugEnabled()) {
				logger.debug("Finished processing of " + this.exporterNameClause + "remote call: " +
						ClassUtils.getQualifiedMethodName(method));
			}
			return retVal;
		}
		catch (Throwable ex) {
			if (ex instanceof RuntimeException || ex instanceof Error) {
				if (logger.isWarnEnabled()) {
					logger.warn("Processing of " + this.exporterNameClause + "remote call resulted in fatal exception: " +
							ClassUtils.getQualifiedMethodName(method), ex);
				}
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Processing of " + this.exporterNameClause + "remote call resulted in exception: " +
							ClassUtils.getQualifiedMethodName(method), ex);
				}
			}
			throw ex;
		}
	}

}
