package org.springframework.mail;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ObjectUtils;

/**
 * 遇到邮件发送错误时抛出异常.
 * 可以注册失败的消息及其异常.
 */
@SuppressWarnings("serial")
public class MailSendException extends MailException {

	private transient final Map<Object, Exception> failedMessages;

	private Exception[] messageExceptions;


	public MailSendException(String msg) {
		this(msg, null);
	}

	public MailSendException(String msg, Throwable cause) {
		super(msg, cause);
		this.failedMessages = new LinkedHashMap<Object, Exception>();
	}

	/**
	 * <p>消息应该与最初传递给调用的send方法的消息相同.
	 * 
	 * @param msg 详细消息
	 * @param cause 来自正在使用的邮件API的根本原因
	 * @param failedMessages 失败的消息为键, 抛出的异常为值
	 */
	public MailSendException(String msg, Throwable cause, Map<Object, Exception> failedMessages) {
		super(msg, cause);
		this.failedMessages = new LinkedHashMap<Object, Exception>(failedMessages);
		this.messageExceptions = failedMessages.values().toArray(new Exception[failedMessages.size()]);
	}

	/**
	 * <p>消息应该与最初传递给调用的send方法的消息相同.
	 * 
	 * @param failedMessages 失败的消息为键, 抛出的异常为值
	 */
	public MailSendException(Map<Object, Exception> failedMessages) {
		this(null, null, failedMessages);
	}


	/**
	 * 失败的消息为键, 抛出的异常为值.
	 * <p>请注意, 常规邮件服务器连接失败不会导致在此处返回失败的消息:
	 * 如果实际发送失败, 则仅在此处包含消息.
	 * <p>消息将与最初传递给调用的send方法的消息相同, 即使用通用MailSender接口时的SimpleMailMessages.
	 * <p>如果通过JavaMailSender发送MimeMessage实例, 则消息将为MimeMessage类型.
	 * <p><b>NOTE:</b> 序列化后, 此Map将不可用.
	 * 在这种情况下使用{@link #getMessageExceptions()}, 该序列也将在序列化后使用.
	 * 
	 * @return 失败的消息为键, 抛出的异常为值
	 */
	public final Map<Object, Exception> getFailedMessages() {
		return this.failedMessages;
	}

	/**
	 * 返回带有抛出消息异常的数组.
	 * <p>请注意，常规邮件服务器连接失败不会导致在此处返回失败的消息:
	 * 如果实际发送失败, 则仅在此处包含消息.
	 * 
	 * @return 抛出的消息异常数组; 如果没有失败的消息, 则返回一个空数组
	 */
	public final Exception[] getMessageExceptions() {
		return (this.messageExceptions != null ? this.messageExceptions : new Exception[0]);
	}


	@Override
	public String getMessage() {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			return super.getMessage();
		}
		else {
			StringBuilder sb = new StringBuilder();
			String baseMessage = super.getMessage();
			if (baseMessage != null) {
				sb.append(baseMessage).append(". ");
			}
			sb.append("Failed messages: ");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				sb.append(subEx.toString());
				if (i < this.messageExceptions.length - 1) {
					sb.append("; ");
				}
			}
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			return super.toString();
		}
		else {
			StringBuilder sb = new StringBuilder(super.toString());
			sb.append("; message exceptions (").append(this.messageExceptions.length).append(") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				sb.append('\n').append("Failed message ").append(i + 1).append(": ");
				sb.append(subEx);
			}
			return sb.toString();
		}
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			super.printStackTrace(ps);
		}
		else {
			ps.println(super.toString() + "; message exception details (" +
					this.messageExceptions.length + ") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				ps.println("Failed message " + (i + 1) + ":");
				subEx.printStackTrace(ps);
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			super.printStackTrace(pw);
		}
		else {
			pw.println(super.toString() + "; message exception details (" +
					this.messageExceptions.length + ") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				pw.println("Failed message " + (i + 1) + ":");
				subEx.printStackTrace(pw);
			}
		}
	}

}
