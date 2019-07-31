package org.springframework.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * {@code HttpMessageConverter}, 使用{@link StringHttpMessageConverter}读取和写入内容,
 * 并使用{@link ConversionService}进行字符串内容和目标对象类型的相互转换.
 *
 * <p>默认情况下, 此转换器仅支持媒体类型{@code text/plain}.
 * 这可以通过{@link #setSupportedMediaTypes supportedMediaTypes}属性覆盖.
 *
 * <p>示例:
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
	 * 使用{@code ConversionService}进行字符串消息正文和目标对象类型的相互转换.
	 * 此构造函数使用{@link StringHttpMessageConverter#DEFAULT_CHARSET}作为默认字符集.
	 * 
	 * @param conversionService 转换服务
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService) {
		this(conversionService, StringHttpMessageConverter.DEFAULT_CHARSET);
	}

	/**
	 * @param conversionService 转换服务
	 * @param defaultCharset 默认字符集
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService, Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN);

		Assert.notNull(conversionService, "ConversionService is required");
		this.conversionService = conversionService;
		this.stringHttpMessageConverter = new StringHttpMessageConverter(defaultCharset);
	}


	/**
	 * 指示是否应将{@code Accept-Charset}写入任何传出请求.
	 * <p>默认{@code true}.
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
		// 不应该调用, 因为覆盖了canRead/Write
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
