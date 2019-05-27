package org.springframework.mail.javamail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * 用于填充{@link javax.mail.internet.MimeMessage}的助手类.
 *
 * <p>镜像{@link org.springframework.mail.SimpleMailMessage}的简单setter, 直接将值应用于底层MimeMessage.
 * 允许为整个消息定义字符编码, 由此帮助程序类的所有方法自动应用.
 *
 * <p>提供对HTML文本内容, 内嵌元素(如图像)和典型邮件附件的支持.
 * 还支持邮件地址附带的个人姓名.
 * 请注意, 高级设置仍可以直接应用于底层MimeMessage对象!
 *
 * <p>通常用于{@link MimeMessagePreparator}实现或{@link JavaMailSender}客户端代码:
 * 简单地将其实例化为MimeMessage包装器, 在包装器上调用setter, 使用底层MimeMessage进行邮件发送.
 * 也由{@link JavaMailSenderImpl}内部使用.
 *
 * <p>带有内嵌图像和PDF附件的HTML邮件的示例代码:
 *
 * <pre class="code">
 * mailSender.send(new MimeMessagePreparator() {
 *   public void prepare(MimeMessage mimeMessage) throws MessagingException {
 *     MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
 *     message.setFrom("me@mail.com");
 *     message.setTo("you@mail.com");
 *     message.setSubject("my subject");
 *     message.setText("my text &lt;img src='cid:myLogo'&gt;", true);
 *     message.addInline("myLogo", new ClassPathResource("img/mylogo.gif"));
 *     message.addAttachment("myDocument.pdf", new ClassPathResource("doc/myDocument.pdf"));
 *   }
 * });</pre>
 *
 * 考虑在这个帮助程序之上使用{@link MimeMailMessage} (它实现常见的{@link org.springframework.mail.MailMessage}接口,
 * 就像{@link org.springframework.mail.SimpleMailMessage}),
 * 为了让消息填充代码通过公共接口与简单消息或MIME消息进行交互.
 *
 * <p><b>有关多部分邮件的警告:</b>
 * 只包含HTML文本, 但没有内联元素或附件的简单MIME消息, 可以在或多或少能够进行HTML呈现的电子邮件客户端上运行.
 * 但是, 内联元素和附件仍然是电子邮件客户端之间的主要兼容性问题:
 * 几乎不可能在Microsoft Outlook, Lotus Notes和Mac Mail中使用内联元素和附件.
 * 考虑根据需要选择特定的多部分模式:
 * MULTIPART_MODE常量上的javadoc包含更详细的信息.
 */
public class MimeMessageHelper {

	/**
	 * 表示非多部分消息.
	 */
	public static final int MULTIPART_MODE_NO = 0;

	/**
	 * 指示具有"mixed"类型的单个根, 多部分元素的多部分消息.
	 * 文本, 内联元素和附件都将添加到该根元素中.
	 * <p>这是Spring 1.0的默认行为. 众所周知, 它可以在Outlook上正常工作.
	 * 但是, 其他邮件客户端往往会将内联元素误解为附件和/或内联显示附件.
	 */
	public static final int MULTIPART_MODE_MIXED = 1;

	/**
	 * 指示具有"related"类型的单个根多部分元素的多部分消息.
	 * 文本, 内联元素和附件都将添加到该根元素中.
	 * <p>这是从Spring 1.1到1.2版本的默认行为.
	 * 这是Outlook本机发送的"Microsoft multipart mode".
	 * 众所周知, 它可以在Outlook, Outlook Express, Yahoo Mail上正常工作, 并且在很大程度上也可以在Mac Mail上运行
	 * (为内联元素列出了附加附件, 尽管内联元素也显示为内联).
	 * 在Lotus Notes上无法正常工作 (附件不会在那里显示).
	 */
	public static final int MULTIPART_MODE_RELATED = 2;

	/**
	 * 指示具有根多部分元素"mixed"以及类型为"related"的嵌套多部分元素的多部分消息.
	 * 文本和内联元素将添加到嵌套的"related"元素, 而附件将添加到"mixed"根元素.
	 * <p>这是自Spring 1.2.1以来的默认值. 根据MIME规范, 这可以说是最正确的MIME结构:
	 * 众所周知, 它可以在Outlook, Outlook Express, Yahoo Mail, Lotus Notes上正常工作.
	 * 在Mac Mail上无法正常工作. 如果目标Mac Mail或在Outlook上遇到特定邮件的问题, 考虑使用MULTIPART_MODE_RELATED.
	 */
	public static final int MULTIPART_MODE_MIXED_RELATED = 3;


	private static final String MULTIPART_SUBTYPE_MIXED = "mixed";

	private static final String MULTIPART_SUBTYPE_RELATED = "related";

	private static final String MULTIPART_SUBTYPE_ALTERNATIVE = "alternative";

	private static final String CONTENT_TYPE_ALTERNATIVE = "text/alternative";

	private static final String CONTENT_TYPE_HTML = "text/html";

	private static final String CONTENT_TYPE_CHARSET_SUFFIX = ";charset=";

	private static final String HEADER_PRIORITY = "X-Priority";

	private static final String HEADER_CONTENT_ID = "Content-ID";


	private final MimeMessage mimeMessage;

	private MimeMultipart rootMimeMultipart;

	private MimeMultipart mimeMultipart;

	private final String encoding;

	private FileTypeMap fileTypeMap;

	private boolean validateAddresses = false;


	/**
	 * 假设一个简单的文本消息 (没有多部分内容, i.e. 没有替代文本, 没有内联元素或附件).
	 * <p>消息的字符编码将从传入的MimeMessage对象中获取, 如果在那里携带的话.
	 * 否则, 将使用JavaMail的默认编码.
	 * 
	 * @param mimeMessage 要使用的MimeMessage
	 */
	public MimeMessageHelper(MimeMessage mimeMessage) {
		this(mimeMessage, null);
	}

	/**
	 * 假设一个简单的文本消息 (没有多部分内容, i.e. 没有替代文本, 没有内联元素或附件).
	 * 
	 * @param mimeMessage 要使用的MimeMessage
	 * @param encoding 用于消息的字符编码
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, String encoding) {
		this.mimeMessage = mimeMessage;
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}

	/**
	 * 多部分模式 (支持替代文本，内联元素和附件).
	 * <p>考虑使用MimeMessageHelper构造函数,
	 * 该构造函数采用multipartMode参数来选择除MULTIPART_MODE_MIXED_RELATED之外的特定多部分模式.
	 * <p>消息的字符编码将从传入的MimeMessage对象中获取, 如果在那里携带的话.
	 * 否则, 将使用JavaMail的默认编码.
	 * 
	 * @param mimeMessage 要使用的MimeMessage
	 * @param multipart 是否创建支持替代文本, 内联元素和附件的多部分消息 (对应于MULTIPART_MODE_MIXED_RELATED)
	 * 
	 * @throws MessagingException 如果多部分创建失败
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart) throws MessagingException {
		this(mimeMessage, multipart, null);
	}

	/**
	 * 多部分模式 (支持替代文本，内联元素和附件).
	 * <p>考虑使用MimeMessageHelper构造函数,
	 * 该构造函数采用multipartMode参数来选择除MULTIPART_MODE_MIXED_RELATED之外的特定多部分模式.
	 * 
	 * @param mimeMessage 要使用的MimeMessage
	 * @param multipart 是否创建支持替代文本, 内联元素和附件的多部分消息 (对应于MULTIPART_MODE_MIXED_RELATED)
	 * @param encoding 用于消息的字符编码
	 * 
	 * @throws MessagingException 如果多部分创建失败
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart, String encoding)
			throws MessagingException {

		this(mimeMessage, (multipart ? MULTIPART_MODE_MIXED_RELATED : MULTIPART_MODE_NO), encoding);
	}

	/**
	 * 多部分模式 (支持替代文本，内联元素和附件).
	 * <p>消息的字符编码将从传入的MimeMessage对象中获取, 如果在那里携带的话.
	 * 否则, 将使用JavaMail的默认编码.
	 * 
	 * @param mimeMessage 要使用的MimeMessage
	 * @param multipartMode 要创建哪种多部分消息 (MIXED, RELATED, MIXED_RELATED, NO)
	 * 
	 * @throws MessagingException 如果多部分创建失败
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		this(mimeMessage, multipartMode, null);
	}

	/**
	 * 多部分模式 (支持替代文本，内联元素和附件).
	 * 
	 * @param mimeMessage 要使用的MimeMessage
	 * @param multipartMode 要创建哪种多部分消息(MIXED, RELATED, MIXED_RELATED, NO)
	 * @param encoding 用于消息的字符编码
	 * 
	 * @throws MessagingException 如果多部分创建失败
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode, String encoding)
			throws MessagingException {

		this.mimeMessage = mimeMessage;
		createMimeMultiparts(mimeMessage, multipartMode);
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}


	/**
	 * 返回底层的MimeMessage对象.
	 */
	public final MimeMessage getMimeMessage() {
		return this.mimeMessage;
	}


	/**
	 * 确定要使用的MimeMultipart对象, 一方面用于存储附件, 另一方面用于存储文本和内联元素.
	 * <p>文本和内联元素可以存储在根元素本身(MULTIPART_MODE_MIXED, MULTIPART_MODE_RELATED)中,
	 * 可以存储在嵌套元素中, 而不是直接存储在根元素中(MULTIPART_MODE_MIXED_RELATED).
	 * <p>默认情况下, 根MimeMultipart元素的类型为"mixed" (MULTIPART_MODE_MIXED) 或"related" (MULTIPART_MODE_RELATED).
	 * 主要的multipart元素将作为"related"类型的嵌套元素 (MULTIPART_MODE_MIXED_RELATED),
	 * 或者与根元素本身相同(MULTIPART_MODE_MIXED, MULTIPART_MODE_RELATED)添加.
	 * 
	 * @param mimeMessage 用于添加根MimeMultipart对象的MimeMessage对象
	 * @param multipartMode 多部分模式, 传递给构造函数(MIXED, RELATED, MIXED_RELATED, NO)
	 * 
	 * @throws MessagingException 如果多部分创建失败
	 */
	protected void createMimeMultiparts(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		switch (multipartMode) {
			case MULTIPART_MODE_NO:
				setMimeMultiparts(null, null);
				break;
			case MULTIPART_MODE_MIXED:
				MimeMultipart mixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(mixedMultipart);
				setMimeMultiparts(mixedMultipart, mixedMultipart);
				break;
			case MULTIPART_MODE_RELATED:
				MimeMultipart relatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				mimeMessage.setContent(relatedMultipart);
				setMimeMultiparts(relatedMultipart, relatedMultipart);
				break;
			case MULTIPART_MODE_MIXED_RELATED:
				MimeMultipart rootMixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(rootMixedMultipart);
				MimeMultipart nestedRelatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				MimeBodyPart relatedBodyPart = new MimeBodyPart();
				relatedBodyPart.setContent(nestedRelatedMultipart);
				rootMixedMultipart.addBodyPart(relatedBodyPart);
				setMimeMultiparts(rootMixedMultipart, nestedRelatedMultipart);
				break;
			default:
				throw new IllegalArgumentException("Only multipart modes MIXED_RELATED, RELATED and NO supported");
		}
	}

	/**
	 * 设置此MimeMessageHelper使用的给定的MimeMultipart对象.
	 * 
	 * @param root 根MimeMultipart对象, 将添加附件; 或{@code null} 表示不是multipart
	 * @param main 主要的MimeMultipart对象, 将添加文本和内联元素
	 * (可以与根多部分对象相同, 或嵌套在根多部分元素下面的元素)
	 */
	protected final void setMimeMultiparts(MimeMultipart root, MimeMultipart main) {
		this.rootMimeMultipart = root;
		this.mimeMultipart = main;
	}

	/**
	 * 返回是否处于多部分模式, i.e. 是否包含多部分消息.
	 */
	public final boolean isMultipart() {
		return (this.rootMimeMultipart != null);
	}

	/**
	 * 如果不处于多部分模式, 则抛出IllegalStateException.
	 */
	private void checkMultipart() throws IllegalStateException {
		if (!isMultipart()) {
			throw new IllegalStateException("Not in multipart mode - " +
				"create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
				"if you need to set alternative texts or add inline elements or attachments.");
		}
	}

	/**
	 * 返回根MIME "multipart/mixed"对象.
	 * 可用于手动添加附件.
	 * <p>在多部分邮件的情况下, 这将是MimeMessage的直接内容.
	 * 
	 * @throws IllegalStateException 如果此类不是多部分模式
	 */
	public final MimeMultipart getRootMimeMultipart() throws IllegalStateException {
		checkMultipart();
		return this.rootMimeMultipart;
	}

	/**
	 *返回底层MIME "multipart/related"对象.
	 * 可用于手动添加主体部分, 内联元素等.
	 * <p>如果是多部分邮件, 这将嵌套在根MimeMultipart中.
	 * 
	 * @throws IllegalStateException 如果此类不是多部分模式
	 */
	public final MimeMultipart getMimeMultipart() throws IllegalStateException {
		checkMultipart();
		return this.mimeMultipart;
	}


	/**
	 * 确定给定MimeMessage的默认编码.
	 * 
	 * @param mimeMessage 传入的MimeMessage
	 * 
	 * @return 与MimeMessage关联的默认编码, 或{@code null}
	 */
	protected String getDefaultEncoding(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			return ((SmartMimeMessage) mimeMessage).getDefaultEncoding();
		}
		return null;
	}

	/**
	 * 返回用于此消息的特定字符编码.
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * 确定给定MimeMessage的默认Java Activation FileTypeMap.
	 * 
	 * @param mimeMessage 传入的MimeMessage
	 * 
	 * @return 与MimeMessage关联的默认FileTypeMap; 如果没有找到该消息, 则为默认的ConfigurableMimeFileTypeMap
	 */
	protected FileTypeMap getDefaultFileTypeMap(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			FileTypeMap fileTypeMap = ((SmartMimeMessage) mimeMessage).getDefaultFileTypeMap();
			if (fileTypeMap != null) {
				return fileTypeMap;
			}
		}
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		return fileTypeMap;
	}

	/**
	 * 设置Java Activation Framework {@code FileTypeMap}, 以用于确定添加到消息中的内联内容和附件的内容类型.
	 * <p>默认是底层MimeMessage携带的{@code FileTypeMap}, 或者是Activation Framework的默认{@code FileTypeMap}实例.
	 */
	public void setFileTypeMap(FileTypeMap fileTypeMap) {
		this.fileTypeMap = (fileTypeMap != null ? fileTypeMap : getDefaultFileTypeMap(getMimeMessage()));
	}

	/**
	 * 返回此MimeMessageHelper使用的{@code FileTypeMap}.
	 */
	public FileTypeMap getFileTypeMap() {
		return this.fileTypeMap;
	}


	/**
	 * 设置是否验证传递给此类的所有地址.
	 * 默认"false".
	 * <p>请注意, 默认情况下, 这仅适用于JavaMail >= 1.3.
	 * 可以覆盖默认的{@code validateAddress method}以在较旧的JavaMail版本上进行验证 (或用于自定义验证).
	 */
	public void setValidateAddresses(boolean validateAddresses) {
		this.validateAddresses = validateAddresses;
	}

	/**
	 * 返回是否验证传递给此类的所有地址.
	 */
	public boolean isValidateAddresses() {
		return this.validateAddresses;
	}

	/**
	 * 验证给定的邮件地址.
	 * 由MimeMessageHelper的所有地址setter和adder调用.
	 * <p>默认实现调用{@code InternetAddress.validate()}, 前提是Helper实例激活了地址验证.
	 * <p>请注意, 默认情况下, 这仅适用于JavaMail >= 1.3.
	 * 可以覆盖它以在较旧的JavaMail版本上进行验证或进行自定义验证.
	 * 
	 * @param address 要验证的地址
	 * 
	 * @throws AddressException 如果验证失败
	 */
	protected void validateAddress(InternetAddress address) throws AddressException {
		if (isValidateAddresses()) {
			address.validate();
		}
	}

	/**
	 * 验证所有给定的邮件地址.
	 * 默认实现只是委托给每个地址的validateAddress.
	 * 
	 * @param addresses 要验证的地址
	 * 
	 * @throws AddressException 如果验证失败
	 */
	protected void validateAddresses(InternetAddress[] addresses) throws AddressException {
		for (InternetAddress address : addresses) {
			validateAddress(address);
		}
	}


	public void setFrom(InternetAddress from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		validateAddress(from);
		this.mimeMessage.setFrom(from);
	}

	public void setFrom(String from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(parseAddress(from));
	}

	public void setFrom(String from, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(getEncoding() != null ?
			new InternetAddress(from, personal, getEncoding()) : new InternetAddress(from, personal));
	}

	public void setReplyTo(InternetAddress replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		validateAddress(replyTo);
		this.mimeMessage.setReplyTo(new InternetAddress[] {replyTo});
	}

	public void setReplyTo(String replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		setReplyTo(parseAddress(replyTo));
	}

	public void setReplyTo(String replyTo, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		InternetAddress replyToAddress = (getEncoding() != null) ?
				new InternetAddress(replyTo, personal, getEncoding()) : new InternetAddress(replyTo, personal);
		setReplyTo(replyToAddress);
	}


	public void setTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.setRecipient(Message.RecipientType.TO, to);
	}

	public void setTo(InternetAddress[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		validateAddresses(to);
		this.mimeMessage.setRecipients(Message.RecipientType.TO, to);
	}

	public void setTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		setTo(parseAddress(to));
	}

	public void setTo(String[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		InternetAddress[] addresses = new InternetAddress[to.length];
		for (int i = 0; i < to.length; i++) {
			addresses[i] = parseAddress(to[i]);
		}
		setTo(addresses);
	}

	public void addTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.addRecipient(Message.RecipientType.TO, to);
	}

	public void addTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		addTo(parseAddress(to));
	}

	public void addTo(String to, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(to, "To address must not be null");
		addTo(getEncoding() != null ?
			new InternetAddress(to, personal, getEncoding()) :
			new InternetAddress(to, personal));
	}


	public void setCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.setRecipient(Message.RecipientType.CC, cc);
	}

	public void setCc(InternetAddress[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		validateAddresses(cc);
		this.mimeMessage.setRecipients(Message.RecipientType.CC, cc);
	}

	public void setCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		setCc(parseAddress(cc));
	}

	public void setCc(String[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[cc.length];
		for (int i = 0; i < cc.length; i++) {
			addresses[i] = parseAddress(cc[i]);
		}
		setCc(addresses);
	}

	public void addCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.addRecipient(Message.RecipientType.CC, cc);
	}

	public void addCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(parseAddress(cc));
	}

	public void addCc(String cc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(getEncoding() != null ?
			new InternetAddress(cc, personal, getEncoding()) :
			new InternetAddress(cc, personal));
	}


	public void setBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.setRecipient(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(InternetAddress[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		validateAddresses(bcc);
		this.mimeMessage.setRecipients(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		setBcc(parseAddress(bcc));
	}

	public void setBcc(String[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[bcc.length];
		for (int i = 0; i < bcc.length; i++) {
			addresses[i] = parseAddress(bcc[i]);
		}
		setBcc(addresses);
	}

	public void addBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.addRecipient(Message.RecipientType.BCC, bcc);
	}

	public void addBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(parseAddress(bcc));
	}

	public void addBcc(String bcc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(getEncoding() != null ?
			new InternetAddress(bcc, personal, getEncoding()) :
			new InternetAddress(bcc, personal));
	}

	private InternetAddress parseAddress(String address) throws MessagingException {
		InternetAddress[] parsed = InternetAddress.parse(address);
		if (parsed.length != 1) {
			throw new AddressException("Illegal address", address);
		}
		InternetAddress raw = parsed[0];
		try {
			return (getEncoding() != null ?
					new InternetAddress(raw.getAddress(), raw.getPersonal(), getEncoding()) : raw);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessagingException("Failed to parse embedded personal name to correct encoding", ex);
		}
	}


	/**
	 * 设置消息的优先级("X-Priority" header).
	 * 
	 * @param priority 优先级; 通常是1 (最高) 到 5 (最低)
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void setPriority(int priority) throws MessagingException {
		this.mimeMessage.setHeader(HEADER_PRIORITY, Integer.toString(priority));
	}

	/**
	 * 设置消息的发送日期.
	 * 
	 * @param sentDate 发送日期(never {@code null})
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void setSentDate(Date sentDate) throws MessagingException {
		Assert.notNull(sentDate, "Sent date must not be null");
		this.mimeMessage.setSentDate(sentDate);
	}

	/**
	 * 设置消息的主题, 使用正确的编码.
	 * 
	 * @param subject 主题文本
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void setSubject(String subject) throws MessagingException {
		Assert.notNull(subject, "Subject must not be null");
		if (getEncoding() != null) {
			this.mimeMessage.setSubject(subject, getEncoding());
		}
		else {
			this.mimeMessage.setSubject(subject);
		}
	}


	/**
	 * 将给定文本直接设置为非多部分模式中的内容, 或者作为多部分模式中的默认正文部分.
	 * 始终应用默认内容类型"text/plain".
	 * <p><b>NOTE:</b> 在{@code setText}<i>之后</i>调用{@link #addInline};
	 * 否则, 邮件阅读器可能无法正确解析内联引用.
	 * 
	 * @param text 消息的文本
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void setText(String text) throws MessagingException {
		setText(text, false);
	}

	/**
	 * 将给定文本直接设置为非多部分模式中的内容, 或者作为多部分模式中的默认正文部分.
	 * "html"标志确定要应用的内容类型.
	 * <p><b>NOTE:</b> 在{@code setText}<i>之后</i>调用{@link #addInline};
	 * 否则, 邮件阅读器可能无法正确解析内联引用.
	 * 
	 * @param text 消息的文本
	 * @param html 是否为HTML邮件应用内容类型"text/html", 使用默认内容类型("text/plain")
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void setText(String text, boolean html) throws MessagingException {
		Assert.notNull(text, "Text must not be null");
		MimePart partToUse;
		if (isMultipart()) {
			partToUse = getMainPart();
		}
		else {
			partToUse = this.mimeMessage;
		}
		if (html) {
			setHtmlTextToMimePart(partToUse, text);
		}
		else {
			setPlainTextToMimePart(partToUse, text);
		}
	}

	/**
	 * 将给定的纯文本和HTML文本设置为备选方案, 为邮件客户端提供两个选项. 需要多部分模式.
	 * <p><b>NOTE:</b> 在{@code setText}<i>之后</i>调用{@link #addInline};
	 * 否则, 邮件阅读器可能无法正确解析内联引用.
	 * 
	 * @param plainText 消息的纯文本
	 * @param htmlText 消息的HTML文本
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void setText(String plainText, String htmlText) throws MessagingException {
		Assert.notNull(plainText, "Plain text must not be null");
		Assert.notNull(htmlText, "HTML text must not be null");

		MimeMultipart messageBody = new MimeMultipart(MULTIPART_SUBTYPE_ALTERNATIVE);
		getMainPart().setContent(messageBody, CONTENT_TYPE_ALTERNATIVE);

		// Create the plain text part of the message.
		MimeBodyPart plainTextPart = new MimeBodyPart();
		setPlainTextToMimePart(plainTextPart, plainText);
		messageBody.addBodyPart(plainTextPart);

		// Create the HTML text part of the message.
		MimeBodyPart htmlTextPart = new MimeBodyPart();
		setHtmlTextToMimePart(htmlTextPart, htmlText);
		messageBody.addBodyPart(htmlTextPart);
	}

	private MimeBodyPart getMainPart() throws MessagingException {
		MimeMultipart mimeMultipart = getMimeMultipart();
		MimeBodyPart bodyPart = null;
		for (int i = 0; i < mimeMultipart.getCount(); i++) {
			BodyPart bp = mimeMultipart.getBodyPart(i);
			if (bp.getFileName() == null) {
				bodyPart = (MimeBodyPart) bp;
			}
		}
		if (bodyPart == null) {
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeMultipart.addBodyPart(mimeBodyPart);
			bodyPart = mimeBodyPart;
		}
		return bodyPart;
	}

	private void setPlainTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setText(text, getEncoding());
		}
		else {
			mimePart.setText(text);
		}
	}

	private void setHtmlTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setContent(text, CONTENT_TYPE_HTML + CONTENT_TYPE_CHARSET_SUFFIX + getEncoding());
		}
		else {
			mimePart.setContent(text, CONTENT_TYPE_HTML);
		}
	}


	/**
	 * 将内联元素添加到MimeMessage, 从{@code javax.activation.DataSource}获取内容.
	 * <p>请注意, DataSource实现返回的InputStream需要<i>在每次调用时都是新的</i>, 因为JavaMail将多次调用{@code getInputStream()}.
	 * <p><b>NOTE:</b> 在{@code setText}<i>之后</i>调用{@link #addInline};
	 * 否则, 邮件阅读器可能无法正确解析内联引用.
	 * 
	 * @param contentId 要使用的内容ID.
	 * 将最终作为"Content-ID" header放在正文部分中, 用尖括号括起来: e.g. "myId" -> "&lt;myId&gt;".
	 * 可以通过 src="cid:myId" 表达式在HTML源代码中引用.
	 * @param dataSource 从中获取内容的{@code javax.activation.DataSource}, 确定InputStream和内容类型
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addInline(String contentId, DataSource dataSource) throws MessagingException {
		Assert.notNull(contentId, "Content ID must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setDisposition(MimeBodyPart.INLINE);
		// 在这里使用setHeader来保持与JavaMail 1.2兼容, 而不是JavaMail 1.3的setContentID.
		mimeBodyPart.setHeader(HEADER_CONTENT_ID, "<" + contentId + ">");
		mimeBodyPart.setDataHandler(new DataHandler(dataSource));
		getMimeMultipart().addBodyPart(mimeBodyPart);
	}

	/**
	 * 将内联元素添加到MimeMessage, 从{@code java.io.File}获取内容.
	 * <p>内容类型将由给定内容文件的名称确定.
	 * 不要将此用于具有任意文件名的临时文件(可能以".tmp"等结尾)!
	 * <p><b>NOTE:</b> 在{@code setText}<i>之后</i>调用{@link #addInline};
	 * 否则, 邮件阅读器可能无法正确解析内联引用.
	 * 
	 * @param contentId 要使用的内容ID.
	 * 将最终作为"Content-ID" header放在正文部分中, 用尖括号括起来: e.g. "myId" -> "&lt;myId&gt;".
	 * 可以通过 src="cid:myId" 表达式在HTML源代码中引用.
	 * @param file 要从中获取内容的文件资源
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addInline(String contentId, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addInline(contentId, dataSource);
	}

	/**
	 * 将内联元素添加到MimeMessage, 从{@code org.springframework.core.io.Resource}获取内容.
	 * <p>内容类型将由给定内容文件的名称确定.
	 * 不要将此用于具有任意文件名的临时文件(可能以".tmp"等结尾)!
	 * <p>请注意, DataSource实现返回的InputStream需要<i>在每次调用时都是新的</i>, 因为JavaMail将多次调用{@code getInputStream()}.
	 * <p><b>NOTE:</b> 在{@code setText}<i>之后</i>调用{@link #addInline};
	 * 否则, 邮件阅读器可能无法正确解析内联引用.
	 * 
	 * @param contentId 要使用的内容ID.
	 * 将最终作为"Content-ID" header放在正文部分中, 用尖括号括起来: e.g. "myId" -> "&lt;myId&gt;".
	 * 可以通过 src="cid:myId" 表达式在HTML源代码中引用.
	 * @param resource 从中获取内容的资源
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addInline(String contentId, Resource resource) throws MessagingException {
		Assert.notNull(resource, "Resource must not be null");
		String contentType = getFileTypeMap().getContentType(resource.getFilename());
		addInline(contentId, resource, contentType);
	}

	/**
	 * 将内联元素添加到MimeMessage, 从{@code org.springframework.core.InputStreamResource}获取内容, 并明确指定内容类型.
	 * <p>可以通过Java Activation Framework的FileTypeMap确定任何给定文件名的内容类型, 例如此类所持有的文件类型.
	 * <p>请注意, DataSource实现返回的InputStream需要<i>在每次调用时都是新的</i>, 因为JavaMail将多次调用{@code getInputStream()}.
	 * <p><b>NOTE:</b> 在{@code setText}<i>之后</i>调用{@link #addInline};
	 * 否则, 邮件阅读器可能无法正确解析内联引用.
	 * 
	 * @param contentId 要使用的内容ID.
	 * 将最终作为"Content-ID" header放在正文部分中, 用尖括号括起来: e.g. "myId" -> "&lt;myId&gt;".
	 * 可以通过 src="cid:myId" 表达式在HTML源代码中引用.
	 * @param inputStreamSource 从中获取内容的资源
	 * @param contentType 用于元素的内容类型
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addInline(String contentId, InputStreamSource inputStreamSource, String contentType)
			throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, "inline");
		addInline(contentId, dataSource);
	}

	/**
	 * 添加附件到MimeMessage, 从{@code javax.activation.DataSource}获取内容.
	 * <p>请注意, DataSource实现返回的InputStream需要<i>在每次调用时都是新的</i>, 因为JavaMail将多次调用{@code getInputStream()}.
	 * 
	 * @param attachmentFilename 将在邮件中显示的附件名称 (内容类型将由此确定)
	 * @param dataSource 从中获取内容的{@code javax.activation.DataSource}, 确定InputStream和内容类型
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addAttachment(String attachmentFilename, DataSource dataSource) throws MessagingException {
		Assert.notNull(attachmentFilename, "Attachment filename must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		try {
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
			mimeBodyPart.setFileName(MimeUtility.encodeText(attachmentFilename));
			mimeBodyPart.setDataHandler(new DataHandler(dataSource));
			getRootMimeMultipart().addBodyPart(mimeBodyPart);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessagingException("Failed to encode attachment filename", ex);
		}
	}

	/**
	 * 添加附件到MimeMessage, 从{@code java.io.File}获取内容.
	 * <p>内容类型将由给定内容文件的名称确定.
	 * 不要将此用于具有任意文件名的临时文件(可能以".tmp"等结尾)!
	 * 
	 * @param attachmentFilename 邮件中的附件名称
	 * @param file 要从中获取内容的文件资源
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addAttachment(String attachmentFilename, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * 添加附件到MimeMessage, 从{@code org.springframework.core.io.InputStreamResource}获取内容.
	 * <p>内容类型将由附件的给定文件名确定.
	 * 因此, 任何内容源都可以, 包括具有任意文件名的临时文件.
	 * <p>请注意, DataSource实现返回的InputStream需要<i>在每次调用时都是新的</i>, 因为JavaMail将多次调用{@code getInputStream()}.
	 * 
	 * @param attachmentFilename 邮件中的附件名称
	 * @param inputStreamSource 从中获取内容的资源(Spring的所有Resource实现都可以在这里传递)
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addAttachment(String attachmentFilename, InputStreamSource inputStreamSource)
			throws MessagingException {

		String contentType = getFileTypeMap().getContentType(attachmentFilename);
		addAttachment(attachmentFilename, inputStreamSource, contentType);
	}

	/**
	 * 添加附件到MimeMessage, 从{@code org.springframework.core.io.InputStreamResource}获取内容.
	 * <p>请注意, DataSource实现返回的InputStream需要<i>在每次调用时都是新的</i>, 因为JavaMail将多次调用{@code getInputStream()}.
	 * 
	 * @param attachmentFilename 邮件中的附件名称
	 * @param inputStreamSource 从中获取内容的资源(Spring的所有Resource实现都可以在这里传递)
	 * @param contentType 用于元素的内容类型
	 * 
	 * @throws MessagingException 发生错误
	 */
	public void addAttachment(
			String attachmentFilename, InputStreamSource inputStreamSource, String contentType)
			throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, attachmentFilename);
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * 为给定的InputStreamSource创建Activation Framework DataSource.
	 * 
	 * @param inputStreamSource the InputStreamSource (通常是Spring Resource)
	 * @param contentType 内容类型
	 * @param name DataSource的名称
	 * 
	 * @return the Activation Framework DataSource
	 */
	protected DataSource createDataSource(
		final InputStreamSource inputStreamSource, final String contentType, final String name) {

		return new DataSource() {
			@Override
			public InputStream getInputStream() throws IOException {
				return inputStreamSource.getInputStream();
			}
			@Override
			public OutputStream getOutputStream() {
				throw new UnsupportedOperationException("Read-only javax.activation.DataSource");
			}
			@Override
			public String getContentType() {
				return contentType;
			}
			@Override
			public String getName() {
				return name;
			}
		};
	}
}
