package org.springframework.mail.javamail;

import javax.activation.FileTypeMap;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * 标准JavaMail {@link MimeMessage}的特殊子类, 带有在填充消息时使用的默认编码,
 * 和用于解析附件类型的默认Java Activation {@link FileTypeMap}.
 *
 * <p>如果是指定的默认编码和/或默认的FileTypeMap, 则由{@link JavaMailSenderImpl}创建.
 * 由{@link MimeMessageHelper}自动检测, 除非明确覆盖, 否则将使用携带的编码和FileTypeMap.
 */
class SmartMimeMessage extends MimeMessage {

	private final String defaultEncoding;

	private final FileTypeMap defaultFileTypeMap;


	/**
	 * @param session 用于创建消息的JavaMail会话
	 * @param defaultEncoding 默认编码, 或{@code null}
	 * @param defaultFileTypeMap 默认的FileTypeMap, 或{@code null}
	 */
	public SmartMimeMessage(Session session, String defaultEncoding, FileTypeMap defaultFileTypeMap) {
		super(session);
		this.defaultEncoding = defaultEncoding;
		this.defaultFileTypeMap = defaultFileTypeMap;
	}


	/**
	 * 返回此消息的默认编码, 或{@code null}.
	 */
	public final String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 返回此消息的默认FileTypeMap, 或{@code null}.
	 */
	public final FileTypeMap getDefaultFileTypeMap() {
		return this.defaultFileTypeMap;
	}

}
