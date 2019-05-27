package org.springframework.aop.framework.adapter;

/**
 * 尝试使用不受支持的Advisor或Advice类型时抛出异常.
 */
@SuppressWarnings("serial")
public class UnknownAdviceTypeException extends IllegalArgumentException {

	/**
	 * 将创建一个消息文本，指出该对象既不是Advice的子接口也不是Advisor的子接口.
	 * 
	 * @param advice 未知类型的增强对象
	 */
	public UnknownAdviceTypeException(Object advice) {
		super("Advice object [" + advice + "] is neither a supported subinterface of " +
				"[org.aopalliance.aop.Advice] nor an [org.springframework.aop.Advisor]");
	}

	public UnknownAdviceTypeException(String message) {
		super(message);
	}
}
