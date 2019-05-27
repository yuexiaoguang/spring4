package org.springframework.mail.javamail;

import java.io.InputStream;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;

/**
 * 用于JavaMail的扩展{@link org.springframework.mail.MailSender}接口, 支持MIME消息作为直接参数和准备回调.
 * 通常与{@link MimeMessageHelper}类一起使用, 以便于创建JavaMail {@link MimeMessage MimeMessages}, 包括附件等.
 *
 * <p>如果客户端需要{@link org.springframework.mail.SimpleMailMessage}之外的邮件功能, 客户端应通过此接口与邮件发件者沟通.
 * 生产实现是{@link JavaMailSenderImpl}; 为了测试, 可以基于此接口创建模拟.
 * 客户端通常会通过依赖注入接收JavaMailSender引用.
 *
 * <p>使用此接口的推荐方法是{@link MimeMessagePreparator}机制, 可能使用{@link MimeMessageHelper}来填充消息.
 * See {@link MimeMessageHelper MimeMessageHelper's javadoc} for an example.
 *
 * <p>整个JavaMail {@link javax.mail.Session}管理由JavaMailSender抽象.
 * 客户端代码不应以任何方式处理Session, 而是将整个JavaMail配置和资源处理留给JavaMailSender实现.
 * 这也增加了可测试性.
 *
 * <p>JavaMailSender客户端不像普通的{@link org.springframework.mail.MailSender}客户端那样容易测试,
 * 但与传统的JavaMail代码相比仍然很简单:
 * 让{@link #createMimeMessage()}返回使用{@code Session.getInstance(new Properties())}调用创建的普通{@link MimeMessage},
 * 并检查各种{@code send}方法的模拟实现中传入的消息.
 */
public interface JavaMailSender extends MailSender {

	/**
	 * 为此发件者的底层JavaMail会话创建新的JavaMail MimeMessage.
	 * 需要调用以创建MimeMessage实例, 该实例可由客户端准备并传递给send(MimeMessage).
	 * 
	 * @return 新的MimeMessage实例
	 */
	MimeMessage createMimeMessage();

	/**
	 * 使用给定的输入流作为消息源, 为此发送者的底层JavaMail会话创建新的JavaMail MimeMessage.
	 * 
	 * @param contentStream 消息的原始MIME输入流
	 * 
	 * @return 新的MimeMessage实例
	 * @throws org.springframework.mail.MailParseException 在消息创建失败的情况下
	*/
	MimeMessage createMimeMessage(InputStream contentStream) throws MailException;

	/**
	 * 发送给定的JavaMail MIME消息.
	 * 需要使用{@link #createMimeMessage()}创建消息.
	 * 
	 * @param mimeMessage 要发送的消息
	 * 
	 * @throws org.springframework.mail.MailAuthenticationException 身份验证失败
	 * @throws org.springframework.mail.MailSendException 发送消息失败
	 */
	void send(MimeMessage mimeMessage) throws MailException;

	/**
	 * 批量发送给定的JavaMail MIME消息数组.
	 * 需要使用{@link #createMimeMessage()}创建消息.
	 * 
	 * @param mimeMessages 要发送的消息
	 * 
	 * @throws org.springframework.mail.MailAuthenticationException 身份验证失败
	 * @throws org.springframework.mail.MailSendException 发送消息失败
	 */
	void send(MimeMessage... mimeMessages) throws MailException;

	/**
	 * 发送由给定MimeMessagePreparator准备的JavaMail MIME消息.
	 * <p>准备MimeMessage实例的替代方法, 而不是{@link #createMimeMessage()}和{@link #send(MimeMessage)}调用.
	 * 负责正确的异常转换.
	 * 
	 * @param mimeMessagePreparator 要使用的准备器
	 * 
	 * @throws org.springframework.mail.MailPreparationException 准备消息失败
	 * @throws org.springframework.mail.MailParseException 解析消息失败
	 * @throws org.springframework.mail.MailAuthenticationException 身份验证失败
	 * @throws org.springframework.mail.MailSendException 发送消息失败
	 */
	void send(MimeMessagePreparator mimeMessagePreparator) throws MailException;

	/**
	 * 发送由给定MimeMessagePreparators准备的JavaMail MIME消息.
	 * <p>准备MimeMessage实例的替代方法, 而不是{@link #createMimeMessage()}和{@link #send(MimeMessage[])}调用.
	 * 负责正确的异常转换.
	 * 
	 * @param mimeMessagePreparators 要使用的准备器
	 * 
	 * @throws org.springframework.mail.MailPreparationException 准备消息失败
	 * @throws org.springframework.mail.MailParseException 解析消息失败
	 * @throws org.springframework.mail.MailAuthenticationException 身份验证失败
	 * @throws org.springframework.mail.MailSendException 发送消息失败
	 */
	void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException;

}
