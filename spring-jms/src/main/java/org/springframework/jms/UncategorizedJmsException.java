package org.springframework.jms;

/**
 * 当找不到其他匹配的子类时抛出JmsException.
 */
@SuppressWarnings("serial")
public class UncategorizedJmsException extends JmsException {

	public UncategorizedJmsException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 异常的原因.
	 * 通常期望此参数是{@link javax.jms.JMSException}的正确子类, 但也可以是JNDI NamingException等.
	 */
	public UncategorizedJmsException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param cause 异常的原因.
	 * 通常期望此参数是{@link javax.jms.JMSException}的正确子类, 但也可以是JNDI NamingException等.
	 */
	public UncategorizedJmsException(Throwable cause) {
		super("Uncategorized exception occurred during JMS processing", cause);
	}

}
