package org.springframework.scheduling.quartz;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.MethodInvoker;

/**
 * 未受检的异常, 它包装从目标方法抛出的异常.
 * 从Job传播到Quartz调度器, 反射调用任意目标方法.
 */
@SuppressWarnings("serial")
public class JobMethodInvocationFailedException extends NestedRuntimeException {

	/**
	 * @param methodInvoker 用于反射调用的MethodInvoker
	 * @param cause the root cause (从目标方法抛出)
	 */
	public JobMethodInvocationFailedException(MethodInvoker methodInvoker, Throwable cause) {
		super("Invocation of method '" + methodInvoker.getTargetMethod() +
				"' on target class [" + methodInvoker.getTargetClass() + "] failed", cause);
	}

}
