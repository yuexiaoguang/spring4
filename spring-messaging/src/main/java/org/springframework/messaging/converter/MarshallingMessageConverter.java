package org.springframework.messaging.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.TypeMismatchException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * {@link MessageConverter}的实现, 可以使用Spring的{@link Marshaller}和{@link Unmarshaller}抽象来读写XML.
 *
 * <p>此转换器需要{@code Marshaller}和{@code Unmarshaller}才能使用它.
 * 这些可以通过{@linkplain #MarshallingMessageConverter(Marshaller) 构造函数}
 * 或{@linkplain #setMarshaller(Marshaller) bean属性}注入.
 */
public class MarshallingMessageConverter extends AbstractMessageConverter {

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;


	/**
	 * 允许单独调用{@link #setMarshaller(Marshaller)} 和/或 {@link #setUnmarshaller(Unmarshaller)}的默认构造.
	 */
	public MarshallingMessageConverter() {
		this(new MimeType("application", "xml"), new MimeType("text", "xml"), new MimeType("application", "*+xml"));
	}

	/**
	 * @param supportedMimeTypes 要支持的MIME类型
	 */
	public MarshallingMessageConverter(MimeType... supportedMimeTypes) {
		super(Arrays.asList(supportedMimeTypes));
	}

	/**
	 * 如果给定的{@link Marshaller}也实现{@link Unmarshaller}, 它也用于解组.
	 * <p>请注意, Spring中的所有{@code Marshaller}实现也实现了{@code Unmarshaller}, 以便可以安全地使用此构造函数.
	 * 
	 * @param marshaller 用作编组器和解组器的对象
	 */
	public MarshallingMessageConverter(Marshaller marshaller) {
		this();
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
		if (marshaller instanceof Unmarshaller) {
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}


	/**
	 * 设置此消息转换器使用的{@link Marshaller}.
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 返回配置的Marshaller.
	 */
	public Marshaller getMarshaller() {
		return this.marshaller;
	}

	/**
	 * 设置此消息转换器使用的{@link Unmarshaller}.
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/**
	 * 返回配置的解组器.
	 */
	public Unmarshaller getUnmarshaller() {
		return this.unmarshaller;
	}


	@Override
	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return (supportsMimeType(message.getHeaders()) && this.unmarshaller != null &&
				this.unmarshaller.supports(targetClass));
	}

	@Override
	protected boolean canConvertTo(Object payload, MessageHeaders headers) {
		return (supportsMimeType(headers) && this.marshaller != null &&
				this.marshaller.supports(payload.getClass()));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// 不应该调用, 因为覆盖了 canConvertFrom/canConvertTo
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
		try {
			Source source = getSource(message.getPayload());
			Object result = this.unmarshaller.unmarshal(source);
			if (!targetClass.isInstance(result)) {
				throw new TypeMismatchException(result, targetClass);
			}
			return result;
		}
		catch (Exception ex) {
			throw new MessageConversionException(message, "Could not unmarshal XML: " + ex.getMessage(), ex);
		}
	}

	private Source getSource(Object payload) {
		if (payload instanceof byte[]) {
			return new StreamSource(new ByteArrayInputStream((byte[]) payload));
		}
		else {
			return new StreamSource(new StringReader((String) payload));
		}
	}

	@Override
	protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		try {
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Result result = new StreamResult(out);
				this.marshaller.marshal(payload, result);
				payload = out.toByteArray();
			}
			else {
				Writer writer = new StringWriter();
				Result result = new StreamResult(writer);
				this.marshaller.marshal(payload, result);
				payload = writer.toString();
			}
		}
		catch (Throwable ex) {
			throw new MessageConversionException("Could not marshal XML: " + ex.getMessage(), ex);
		}
		return payload;
	}

}
