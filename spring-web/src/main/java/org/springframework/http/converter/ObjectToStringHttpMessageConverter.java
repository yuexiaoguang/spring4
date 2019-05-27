package org.springframework.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * An {@code HttpMessageConverter} that uses {@link StringHttpMessageConverter}
 * for reading and writing content and a {@link ConversionService} for converting
 * the String content to and from the target object type.
 *
 * <p>By default, this converter supports the media type {@code text/plain} only.
 * This can be overridden through the {@link #setSupportedMediaTypes supportedMediaTypes}
 * property.
 *
 * <p>A usage example:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.ObjectToStringHttpMessageConverter">
 *   &lt;constructor-arg>
 *     &lt;bean class="org.springframework.context.support.ConversionServiceFactoryBean"/>
 *   &lt;/constructor-arg>
 * &lt;/bean>
 * </pre>
 */
public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private final ConversionService conversionService;

	private final StringHttpMessageConverter stringHttpMessageConverter;


	/**
	 * A constructor accepting a {@code ConversionService} to use to convert the
	 * (String) message body to/from the target class type. This constructor uses
	 * {@link StringHttpMessageConverter#DEFAULT_CHARSET} as the default charset.
	 * @param conversionService the conversion service
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService) {
		this(conversionService, StringHttpMessageConverter.DEFAULT_CHARSET);
	}

	/**
	 * A constructor accepting a {@code ConversionService} as well as a default charset.
	 * @param conversionService the conversion service
	 * @param defaultCharset the default charset
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService, Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN);

		Assert.notNull(conversionService, "ConversionService is required");
		this.conversionService = conversionService;
		this.stringHttpMessageConverter = new StringHttpMessageConverter(defaultCharset);
	}


	/**
	 * Indicates whether the {@code Accept-Charset} should be written to any outgoing request.
	 * <p>Default is {@code true}.
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.stringHttpMessageConverter.setWriteAcceptCharset(writeAcceptCharset);
	}


	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return this.conversionService.canConvert(String.class, clazz) && canRead(mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return this.conversionService.canConvert(clazz, String.class) && canWrite(mediaType);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
		String value = this.stringHttpMessageConverter.readInternal(String.class, inputMessage);
		return this.conversionService.convert(value, clazz);
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
		String value = this.conversionService.convert(obj, String.class);
		this.stringHttpMessageConverter.writeInternal(value, outputMessage);
	}

	@Override
	protected Long getContentLength(Object obj, MediaType contentType) {
		String value = this.conversionService.convert(obj, String.class);
		return this.stringHttpMessageConverter.getContentLength(value, contentType);
	}

}