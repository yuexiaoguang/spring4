package org.springframework.mail;

import java.util.Date;

/**
 * 这是邮件消息的通用接口, 允许用户设置组装邮件消息所需的Key值, 而无需知道底层消息是简单的文本消息还是更复杂的MIME消息.
 *
 * <p>由SimpleMailMessage和MimeMessageHelper实现, 让消息填充代码通过公共接口与简单消息或MIME消息交互.
 */
public interface MailMessage {

	void setFrom(String from) throws MailParseException;

	void setReplyTo(String replyTo) throws MailParseException;

	void setTo(String to) throws MailParseException;

	void setTo(String[] to) throws MailParseException;

	void setCc(String cc) throws MailParseException;

	void setCc(String[] cc) throws MailParseException;

	void setBcc(String bcc) throws MailParseException;

	void setBcc(String[] bcc) throws MailParseException;

	void setSentDate(Date sentDate) throws MailParseException;

	void setSubject(String subject) throws MailParseException;

	void setText(String text) throws MailParseException;

}
