package org.springframework.mail;

import java.io.Serializable;
import java.util.Date;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 为简单邮件消息建模, 包括 from, to, cc, subject, 和text等字段.
 *
 * <p>考虑{@code JavaMailSender}和JavaMail {@code MimeMessages}来创建更复杂的消息,
 * 例如带有附件的消息, 特殊字符编码, 或伴随邮件地址的个人名称.
 */
@SuppressWarnings("serial")
public class SimpleMailMessage implements MailMessage, Serializable {

	private String from;

	private String replyTo;

	private String[] to;

	private String[] cc;

	private String[] bcc;

	private Date sentDate;

	private String subject;

	private String text;


	public SimpleMailMessage() {
	}

	/**
	 * 克隆.
	 */
	public SimpleMailMessage(SimpleMailMessage original) {
		Assert.notNull(original, "'original' message argument must not be null");
		this.from = original.getFrom();
		this.replyTo = original.getReplyTo();
		this.to = copyOrNull(original.getTo());
		this.cc = copyOrNull(original.getCc());
		this.bcc = copyOrNull(original.getBcc());
		this.sentDate = original.getSentDate();
		this.subject = original.getSubject();
		this.text = original.getText();
	}


	@Override
	public void setFrom(String from) {
		this.from = from;
	}

	public String getFrom() {
		return this.from;
	}

	@Override
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public String getReplyTo() {
		return this.replyTo;
	}

	@Override
	public void setTo(String to) {
		this.to = new String[] {to};
	}

	@Override
	public void setTo(String[] to) {
		this.to = to;
	}

	public String[] getTo() {
		return this.to;
	}

	@Override
	public void setCc(String cc) {
		this.cc = new String[] {cc};
	}

	@Override
	public void setCc(String[] cc) {
		this.cc = cc;
	}

	public String[] getCc() {
		return this.cc;
	}

	@Override
	public void setBcc(String bcc) {
		this.bcc = new String[] {bcc};
	}

	@Override
	public void setBcc(String[] bcc) {
		this.bcc = bcc;
	}

	public String[] getBcc() {
		return this.bcc;
	}

	@Override
	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public Date getSentDate() {
		return this.sentDate;
	}

	@Override
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return this.subject;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return this.text;
	}


	/**
	 * 将此消息的内容复制到给定的目标消息.
	 * 
	 * @param target 要复制到的{@code MailMessage}
	 */
	public void copyTo(MailMessage target) {
		Assert.notNull(target, "'target' MailMessage must not be null");
		if (getFrom() != null) {
			target.setFrom(getFrom());
		}
		if (getReplyTo() != null) {
			target.setReplyTo(getReplyTo());
		}
		if (getTo() != null) {
			target.setTo(copy(getTo()));
		}
		if (getCc() != null) {
			target.setCc(copy(getCc()));
		}
		if (getBcc() != null) {
			target.setBcc(copy(getBcc()));
		}
		if (getSentDate() != null) {
			target.setSentDate(getSentDate());
		}
		if (getSubject() != null) {
			target.setSubject(getSubject());
		}
		if (getText() != null) {
			target.setText(getText());
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SimpleMailMessage)) {
			return false;
		}
		SimpleMailMessage otherMessage = (SimpleMailMessage) other;
		return (ObjectUtils.nullSafeEquals(this.from, otherMessage.from) &&
				ObjectUtils.nullSafeEquals(this.replyTo, otherMessage.replyTo) &&
				ObjectUtils.nullSafeEquals(this.to, otherMessage.to) &&
				ObjectUtils.nullSafeEquals(this.cc, otherMessage.cc) &&
				ObjectUtils.nullSafeEquals(this.bcc, otherMessage.bcc) &&
				ObjectUtils.nullSafeEquals(this.sentDate, otherMessage.sentDate) &&
				ObjectUtils.nullSafeEquals(this.subject, otherMessage.subject) &&
				ObjectUtils.nullSafeEquals(this.text, otherMessage.text));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.from);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.replyTo);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.to);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.cc);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.bcc);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.sentDate);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.subject);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SimpleMailMessage: ");
		sb.append("from=").append(this.from).append("; ");
		sb.append("replyTo=").append(this.replyTo).append("; ");
		sb.append("to=").append(StringUtils.arrayToCommaDelimitedString(this.to)).append("; ");
		sb.append("cc=").append(StringUtils.arrayToCommaDelimitedString(this.cc)).append("; ");
		sb.append("bcc=").append(StringUtils.arrayToCommaDelimitedString(this.bcc)).append("; ");
		sb.append("sentDate=").append(this.sentDate).append("; ");
		sb.append("subject=").append(this.subject).append("; ");
		sb.append("text=").append(this.text);
		return sb.toString();
	}


	private static String[] copyOrNull(String[] state) {
		if (state == null) {
			return null;
		}
		return copy(state);
	}

	private static String[] copy(String[] state) {
		String[] copy = new String[state.length];
		System.arraycopy(state, 0, copy, 0, state.length);
		return copy;
	}

}
