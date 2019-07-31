package org.springframework.http.converter.protobuf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.googlecode.protobuf.format.HtmlFormat;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.ProtobufFormatter;
import com.googlecode.protobuf.format.XmlFormat;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.FileCopyUtils;

/**
 * {@code HttpMessageConverter}, 使用<a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>
 * 读取和写入{@link com.google.protobuf.Message}.
 *
 * <p>默认情况下, 它支持{@code "application/x-protobuf"}, {@code "text/plain"},
 * {@code "application/json"}, {@code "application/xml"}, 同时还支持{@code "text/html"}.
 *
 * <p>要生成{@code Message} Java类, 需要安装{@code protoc}二进制文件.
 *
 * <p>从Spring 4.3开始, 需要Protobuf 2.6和Protobuf Java Format 1.4.
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);

	public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

	public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";


	private static final ProtobufFormatter JSON_FORMAT = new JsonFormat();

	private static final ProtobufFormatter XML_FORMAT = new XmlFormat();

	private static final ProtobufFormatter HTML_FORMAT = new HtmlFormat();


	private static final ConcurrentHashMap<Class<?>, Method> methodCache = new ConcurrentHashMap<Class<?>, Method>();

	private final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();


	public ProtobufHttpMessageConverter() {
		this(null);
	}

	/**
	 * 使用允许注册消息扩展的{@link ExtensionRegistryInitializer}构造一个新实例.
	 */
	public ProtobufHttpMessageConverter(ExtensionRegistryInitializer registryInitializer) {
		super(PROTOBUF, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
		if (registryInitializer != null) {
			registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
		}
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Message.class.isAssignableFrom(clazz);
	}

	@Override
	protected MediaType getDefaultContentType(Message message) {
		return PROTOBUF;
	}

	@Override
	protected Message readInternal(Class<? extends Message> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = PROTOBUF;
		}
		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		try {
			Message.Builder builder = getMessageBuilder(clazz);
			if (MediaType.TEXT_PLAIN.isCompatibleWith(contentType)) {
				InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
				TextFormat.merge(reader, this.extensionRegistry, builder);
			}
			else if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
				JSON_FORMAT.merge(inputMessage.getBody(), charset, this.extensionRegistry, builder);
			}
			else if (MediaType.APPLICATION_XML.isCompatibleWith(contentType)) {
				XML_FORMAT.merge(inputMessage.getBody(), charset, this.extensionRegistry, builder);
			}
			else {
				builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
			}
			return builder.build();
		}
		catch (Exception ex) {
			throw new HttpMessageNotReadableException("Could not read Protobuf message: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 此方法会覆盖父级实现, 因为此HttpMessageConverter还可以生成{@code MediaType.HTML "text/html"} ContentType.
	 */
	@Override
	protected boolean canWrite(MediaType mediaType) {
		return (super.canWrite(mediaType) || MediaType.TEXT_HTML.isCompatibleWith(mediaType));
	}

	@Override
	protected void writeInternal(Message message, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = getDefaultContentType(message);
		}
		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		if (MediaType.TEXT_PLAIN.isCompatibleWith(contentType)) {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), charset);
			TextFormat.print(message, outputStreamWriter);
			outputStreamWriter.flush();
		}
		else if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
			JSON_FORMAT.print(message, outputMessage.getBody(), charset);
		}
		else if (MediaType.APPLICATION_XML.isCompatibleWith(contentType)) {
			XML_FORMAT.print(message, outputMessage.getBody(), charset);
		}
		else if (MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
			HTML_FORMAT.print(message, outputMessage.getBody(), charset);
		}
		else if (PROTOBUF.isCompatibleWith(contentType)) {
			setProtoHeader(outputMessage, message);
			FileCopyUtils.copy(message.toByteArray(), outputMessage.getBody());
		}
	}

	/**
	 * 使用内容类型为"application/x-protobuf"的消息在响应时设置"X-Protobuf-*" HTTP header.
	 * <p><b>Note:</b> 之前不应该调用<code>outputMessage.getBody()</code>, 因为它会写入HTTP header (使它们只读).</p>
	 */
	private void setProtoHeader(HttpOutputMessage response, Message message) {
		response.getHeaders().set(X_PROTOBUF_SCHEMA_HEADER, message.getDescriptorForType().getFile().getName());
		response.getHeaders().set(X_PROTOBUF_MESSAGE_HEADER, message.getDescriptorForType().getFullName());
	}


	/**
	 * 为给定的类创建一个新的{@code Message.Builder}实例.
	 * <p>此方法使用ConcurrentHashMap缓存方法查找.
	 */
	private static Message.Builder getMessageBuilder(Class<? extends Message> clazz) throws Exception {
		Method method = methodCache.get(clazz);
		if (method == null) {
			method = clazz.getMethod("newBuilder");
			methodCache.put(clazz, method);
		}
		return (Message.Builder) method.invoke(clazz);
	}

}
