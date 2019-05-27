package org.springframework.mail.javamail;

import javax.mail.internet.MimeMessage;

/**
 * 用于准备JavaMail MIME消息的回调接口.
 *
 * <p>{@link JavaMailSender}的相应{@code send}方法将负责实际创建{@link MimeMessage}实例以及正确的异常转换.
 *
 * <p>使用{@link MimeMessageHelper}来填充传入的MimeMessage通常很方便, 特别是在处理附件或特殊字符编码时.
 * See {@link MimeMessageHelper MimeMessageHelper's javadoc} for an example.
 */
public interface MimeMessagePreparator {

	/**
	 * 准备给定的新MimeMessage实例.
	 * 
	 * @param mimeMessage 要准备的信息
	 * 
	 * @throws javax.mail.MessagingException 传递由MimeMessage方法抛出的任何异常, 以自动转换为MailException层次结构
	 * @throws java.io.IOException 传递由MimeMessage方法抛出的任何异常, 以自动转换为MailException层次结构
	 * @throws Exception 如果邮件准备失败, 例如无法为邮件文本呈现Velocity模板
	 */
	void prepare(MimeMessage mimeMessage) throws Exception;

}
