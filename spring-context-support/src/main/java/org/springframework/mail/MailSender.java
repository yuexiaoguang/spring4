package org.springframework.mail;

/**
 * 此接口定义了发送简单邮件的策略.
 * 由于要求简单, 可以被各种邮件系统实现.
 * 对于MIME消息等更丰富的功能, 请考虑使用JavaMailSender.
 *
 * <p>允许轻松测试客户端, 因为它不依赖于JavaMail的基础结构类:
 * 不需要模拟JavaMail会话或Transport.
 */
public interface MailSender {

	/**
	 * 发送给定的简单邮件消息.
	 * 
	 * @param simpleMessage 要发送的消息
	 * 
	 * @throws MailParseException 解析消息失败
	 * @throws MailAuthenticationException 身份验证失败
	 * @throws MailSendException 发送消息失败
	 */
	void send(SimpleMailMessage simpleMessage) throws MailException;

	/**
	 * 批量发送给定的简单邮件数组.
	 * 
	 * @param simpleMessages 要发送的消息
	 * 
	 * @throws MailParseException 解析消息失败
	 * @throws MailAuthenticationException 身份验证失败
	 * @throws MailSendException 发送消息失败
	 */
	void send(SimpleMailMessage... simpleMessages) throws MailException;

}
