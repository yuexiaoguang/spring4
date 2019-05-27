package org.springframework.mail.javamail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.activation.FileTypeMap;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.Assert;

/**
 * {@link JavaMailSender}接口的生产实现,
 * 支持JavaMail {@link MimeMessage MimeMessages}和Spring {@link SimpleMailMessage SimpleMailMessages}.
 * 也可以用作普通的{@link org.springframework.mail.MailSender}实现.
 *
 * <p>允许在本地将所有设置定义为bean属性.
 * 或者, 可以指定预配置的JavaMail {@link javax.mail.Session}, 可能是从应用程序服务器的JNDI环境中提取的.
 *
 * <p>此对象中的非默认属性将始终覆盖JavaMail {@code Session}中的设置.
 * 请注意, 如果在本地覆盖所有值, 则在设置预配置的{@code Session}时没有添加的值.
 */
public class JavaMailSenderImpl implements JavaMailSender {

	/** 默认协议: 'smtp' */
	public static final String DEFAULT_PROTOCOL = "smtp";

	/** 默认端口: -1 */
	public static final int DEFAULT_PORT = -1;

	private static final String HEADER_MESSAGE_ID = "Message-ID";


	private Properties javaMailProperties = new Properties();

	private Session session;

	private String protocol;

	private String host;

	private int port = DEFAULT_PORT;

	private String username;

	private String password;

	private String defaultEncoding;

	private FileTypeMap defaultFileTypeMap;


	/**
	 * <p>使用默认{@link ConfigurableMimeFileTypeMap}初始化{@link #setDefaultFileTypeMap "defaultFileTypeMap"}属性.
	 */
	public JavaMailSenderImpl() {
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		this.defaultFileTypeMap = fileTypeMap;
	}


	/**
	 * 设置{@code Session}的JavaMail属性.
	 * <p>将使用这些属性创建新的{@code Session}.
	 * 使用此方法或{@link #setSession}, 但不能同时使用.
	 * <p>此实例中的非默认属性将覆盖给定的JavaMail属性.
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
		synchronized (this) {
			this.session = null;
		}
	}

	/**
	 * 允许Map访问此发件者的JavaMail属性, 并提供添加或覆盖特定条目的选项.
	 * <p>用于直接指定条目, 例如通过"javaMailProperties[mail.smtp.auth]".
	 */
	public Properties getJavaMailProperties() {
		return this.javaMailProperties;
	}

	/**
	 * 设置JavaMail {@code Session}, 可能是从JNDI中提取的.
	 * <p>默认是没有默认值的新{@code Session}, 它是通过此实例的属性完全配置的.
	 * <p>如果使用预先配置的{@code Session}, 此实例中的非默认属性将覆盖{@code Session}中的设置.
	 */
	public synchronized void setSession(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	/**
	 * 返回JavaMail {@code Session}, 如果没有明确指定, 则延迟初始化它.
	 */
	public synchronized Session getSession() {
		if (this.session == null) {
			this.session = Session.getInstance(this.javaMailProperties);
		}
		return this.session;
	}

	/**
	 * 设置邮件协议. 默认"smtp".
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * 返回邮件协议.
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * 设置邮件服务器主机, 通常是SMTP主机.
	 * <p>默认是底层JavaMail会话的默认主机.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 返回邮件服务器主机.
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 设置邮件服务器端口.
	 * <p>默认 {@link #DEFAULT_PORT}, 让JavaMail使用默认的SMTP端口 (25).
	*/
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 返回邮件服务器端口.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置邮件主机上帐户的用户名.
	 * <p>请注意，必须使用设置为{@code true}的属性{@code "mail.smtp.auth"}配置底层 JavaMail {@code Session},
	 * 否则, JavaMail运行时将不会将指定的用户名发送到邮件服务器.
	 * 如果没有明确传入{@code Session}来使用, 只需通过{@link #setJavaMailProperties}指定此设置即可.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 返回邮件主机上帐户的用户名.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设置邮件主机上帐户的密码.
	 * <p>请注意，必须使用设置为{@code true}的属性{@code "mail.smtp.auth"}配置底层 JavaMail {@code Session},
	 * 否则, JavaMail运行时将不会将指定的用户名发送到邮件服务器.
	 * 如果没有明确传入{@code Session}来使用, 只需通过{@link #setJavaMailProperties}指定此设置即可.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 返回邮件主机上帐户的密码.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * 设置用于此实例创建的{@link MimeMessage MimeMessages}的默认编码.
	 * <p>{@link MimeMessageHelper}将自动检测此类编码.
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回{@link MimeMessage MimeMessages}的默认编码, 如果没有, 则返回{@code null}.
	 */
	public String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 设置默认Java Activation {@link FileTypeMap}以用于此实例创建的{@link MimeMessage MimeMessages}.
	 * <p>此处指定的{@code FileTypeMap}将由{@link MimeMessageHelper}自动检测,
	 * 从而无需为每个{@code MimeMessageHelper}实例指定{@code FileTypeMap}.
	 * <p>例如, 可以在此处指定Spring的{@link ConfigurableMimeFileTypeMap}的自定义实例.
	 * 如果未明确指定, 将使用默认的{@code ConfigurableMimeFileTypeMap}, 其中包含一组扩展的MIME类型映射
	 * (由Spring jar中包含的{@code mime.types}文件定义).
	 */
	public void setDefaultFileTypeMap(FileTypeMap defaultFileTypeMap) {
		this.defaultFileTypeMap = defaultFileTypeMap;
	}

	/**
	 * 返回{@link MimeMessage MimeMessages}的默认Java Activation {@link FileTypeMap}; 如果没有, 则返回{@code null}.
	 */
	public FileTypeMap getDefaultFileTypeMap() {
		return this.defaultFileTypeMap;
	}


	//---------------------------------------------------------------------
	// Implementation of MailSender
	//---------------------------------------------------------------------

	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		send(new SimpleMailMessage[] {simpleMessage});
	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		List<MimeMessage> mimeMessages = new ArrayList<MimeMessage>(simpleMessages.length);
		for (SimpleMailMessage simpleMessage : simpleMessages) {
			MimeMailMessage message = new MimeMailMessage(createMimeMessage());
			simpleMessage.copyTo(message);
			mimeMessages.add(message.getMimeMessage());
		}
		doSend(mimeMessages.toArray(new MimeMessage[mimeMessages.size()]), simpleMessages);
	}


	//---------------------------------------------------------------------
	// Implementation of JavaMailSender
	//---------------------------------------------------------------------

	/**
	 * 此实现创建一个SmartMimeMessage, 包含指定的默认编码和默认的FileTypeMap.
	 * 这个特殊的默认携带消息将由{@link MimeMessageHelper}自动检测, 它将使用携带的编码和FileTypeMap, 除非明确地覆盖.
	 */
	@Override
	public MimeMessage createMimeMessage() {
		return new SmartMimeMessage(getSession(), getDefaultEncoding(), getDefaultFileTypeMap());
	}

	@Override
	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		try {
			return new MimeMessage(getSession(), contentStream);
		}
		catch (Exception ex) {
			throw new MailParseException("Could not parse raw MIME content", ex);
		}
	}

	@Override
	public void send(MimeMessage mimeMessage) throws MailException {
		send(new MimeMessage[] {mimeMessage});
	}

	@Override
	public void send(MimeMessage... mimeMessages) throws MailException {
		doSend(mimeMessages, null);
	}

	@Override
	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		send(new MimeMessagePreparator[] {mimeMessagePreparator});
	}

	@Override
	public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		try {
			List<MimeMessage> mimeMessages = new ArrayList<MimeMessage>(mimeMessagePreparators.length);
			for (MimeMessagePreparator preparator : mimeMessagePreparators) {
				MimeMessage mimeMessage = createMimeMessage();
				preparator.prepare(mimeMessage);
				mimeMessages.add(mimeMessage);
			}
			send(mimeMessages.toArray(new MimeMessage[mimeMessages.size()]));
		}
		catch (MailException ex) {
			throw ex;
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
		catch (Exception ex) {
			throw new MailPreparationException(ex);
		}
	}

	/**
	 * 验证此实例是否可以连接到为其配置的服务器.
	 * 如果连接尝试失败, 则抛出{@link MessagingException}.
	 */
	public void testConnection() throws MessagingException {
		Transport transport = null;
		try {
			transport = connectTransport();
		}
		finally {
			if (transport != null) {
				transport.close();
			}
		}
	}

	/**
	 * 实际上通过JavaMail发送给定的MimeMessages数组.
	 * 
	 * @param mimeMessages 要发送的MimeMessage对象
	 * @param originalMessages 从中创建MimeMessages的相应原始消息对象(具有与"mimeMessages"数组相同的数组长度和索引)
	 * 
	 * @throws org.springframework.mail.MailAuthenticationException 身份验证失败
	 * @throws org.springframework.mail.MailSendException 发送消息失败
	 */
	protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailException {
		Map<Object, Exception> failedMessages = new LinkedHashMap<Object, Exception>();
		Transport transport = null;

		try {
			for (int i = 0; i < mimeMessages.length; i++) {

				// Check transport connection first...
				if (transport == null || !transport.isConnected()) {
					if (transport != null) {
						try {
							transport.close();
						}
						catch (Exception ex) {
							// Ignore - we're reconnecting anyway
						}
						transport = null;
					}
					try {
						transport = connectTransport();
					}
					catch (AuthenticationFailedException ex) {
						throw new MailAuthenticationException(ex);
					}
					catch (Exception ex) {
						// 实际上, 所有剩余的消息都失败了...
						for (int j = i; j < mimeMessages.length; j++) {
							Object original = (originalMessages != null ? originalMessages[j] : mimeMessages[j]);
							failedMessages.put(original, ex);
						}
						throw new MailSendException("Mail server connection failed", ex, failedMessages);
					}
				}

				// 通过当前传输发送消息...
				MimeMessage mimeMessage = mimeMessages[i];
				try {
					if (mimeMessage.getSentDate() == null) {
						mimeMessage.setSentDate(new Date());
					}
					String messageId = mimeMessage.getMessageID();
					mimeMessage.saveChanges();
					if (messageId != null) {
						// 保留明确指定的消息ID...
						mimeMessage.setHeader(HEADER_MESSAGE_ID, messageId);
					}
					transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
				}
				catch (Exception ex) {
					Object original = (originalMessages != null ? originalMessages[i] : mimeMessage);
					failedMessages.put(original, ex);
				}
			}
		}
		finally {
			try {
				if (transport != null) {
					transport.close();
				}
			}
			catch (Exception ex) {
				if (!failedMessages.isEmpty()) {
					throw new MailSendException("Failed to close server connection after message failures", ex,
							failedMessages);
				}
				else {
					throw new MailSendException("Failed to close server connection after message sending", ex);
				}
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	/**
	 * 从底层JavaMail会话获取并连接传输, 传入指定的主机, 端口, 用户名和密码.
	 * 
	 * @return 已连接的Transport对象
	 * @throws MessagingException 连接尝试失败
	 */
	protected Transport connectTransport() throws MessagingException {
		String username = getUsername();
		String password = getPassword();
		if ("".equals(username)) {  // probably from a placeholder
			username = null;
			if ("".equals(password)) {  // in conjunction with "" username, this means no password to use
				password = null;
			}
		}

		Transport transport = getTransport(getSession());
		transport.connect(getHost(), getPort(), username, password);
		return transport;
	}

	/**
	 * 使用配置的协议从给定的JavaMail会话中获取Transport对象.
	 * <p>可以在子类中重写, e.g. 返回一个模拟Transport对象.
	 */
	protected Transport getTransport(Session session) throws NoSuchProviderException {
		String protocol	= getProtocol();
		if (protocol == null) {
			protocol = session.getProperty("mail.transport.protocol");
			if (protocol == null) {
				protocol = DEFAULT_PROTOCOL;
			}
		}
		return session.getTransport(protocol);
	}
}
