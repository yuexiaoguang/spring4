package org.springframework.remoting.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * 封装远程调用结果, 保存结果值或异常.
 * 用于基于HTTP的序列化调用器.
 *
 * <p>这是一个SPI类, 通常不由应用程序直接使用.
 * 可以为其他调用参数进行子类化.
 *
 * <p>{@link RemoteInvocation}和{@link RemoteInvocationResult}都设计用于标准Java序列化以及JavaBean样式序列化.
 */
public class RemoteInvocationResult implements Serializable {

	/** Use serialVersionUID from Spring 1.1 for interoperability */
	private static final long serialVersionUID = 2138555143707773549L;


	private Object value;

	private Throwable exception;


	/**
	 * @param value 成功调用目标方法返回的结果值
	 */
	public RemoteInvocationResult(Object value) {
		this.value = value;
	}

	/**
	 * @param exception 不成功调用目标方法引发的异常
	 */
	public RemoteInvocationResult(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * 用于JavaBean样式的反序列化 (e.g. with Jackson).
	 */
	public RemoteInvocationResult() {
	}


	/**
	 * 设置成功调用目标方法返回的结果值.
	 * <p>此setter用于JavaBean样式的反序列化.
	 * 否则使用{@link #RemoteInvocationResult(Object)}.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 返回成功调用目标方法返回的结果值.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 设置由目标方法调用失败引发的异常.
	 * <p>此setter用于JavaBean样式的反序列化.
	 * 否则使用{@link #RemoteInvocationResult(Throwable)}.
	 */
	public void setException(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * 返回由目标方法调用失败引发的异常.
	 */
	public Throwable getException() {
		return this.exception;
	}

	/**
	 * 返回此调用结果是否包含异常.
	 * 如果返回{@code false}, 则应用结果值 (即使它是{@code null}).
	 */
	public boolean hasException() {
		return (this.exception != null);
	}

	/**
	 * 返回此调用结果是否包含InvocationTargetException, 由调用目标方法本身抛出.
	 */
	public boolean hasInvocationTargetException() {
		return (this.exception instanceof InvocationTargetException);
	}


	/**
	 * 重新创建调用结果, 在成功调用目标方法的情况下返回结果值, 或者重新抛出目标方法抛出的异常.
	 * 
	 * @return 结果值
	 * @throws Throwable 异常
	 */
	public Object recreate() throws Throwable {
		if (this.exception != null) {
			Throwable exToThrow = this.exception;
			if (this.exception instanceof InvocationTargetException) {
				exToThrow = ((InvocationTargetException) this.exception).getTargetException();
			}
			RemoteInvocationUtils.fillInClientStackTraceIfPossible(exToThrow);
			throw exToThrow;
		}
		else {
			return this.value;
		}
	}
}
