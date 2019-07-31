package org.springframework.http.converter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

/**
 * {@link HttpMessageConverter}的实现, 可以读写字符串.
 *
 * <p>默认情况下, 此转换器支持所有媒体类型 ({@code &#42;&#47;&#42;}), 并使用{@code text/plain}的{@code Content-Type}进行写入.
 * 这可以通过设置{@link #setSupportedMediaTypes supportedMediaTypes}属性来覆盖.
 */
public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");


	private volatile List<Charset> availableCharsets;

	private boolean writeAcceptCharset = true;


	public StringHttpMessageConverter() {
		this(DEFAULT_CHARSET);
	}

	/**
	 * 接受默认字符集的构造函数, 如果请求的内容类型未指定, 则使用该字符集.
	 */
	public StringHttpMessageConverter(Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN, MediaType.ALL);
	}


	/**
	 * 指示是否应将{@code Accept-Charset}写入任何传出请求.
	 * <p>默认{@code true}.
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.writeAcceptCharset = writeAcceptCharset;
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return String.class == clazz;
	}

	@Override
	protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
		Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
		return StreamUtils.copyToString(inputMessage.getBody(), charset);
	}

	@Override
	protected Long getContentLength(String str, MediaType contentType) {
		Charset charset = getContentTypeCharset(contentType);
		try {
			return (long) str.getBytes(charset.name()).length;
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new IllegalStateException(ex);
		}
	}

	@Override
	protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
		if (this.writeAcceptCharset) {
			outputMessage.getHeaders().setAcceptCharset(getAcceptedCharsets());
		}
		Charset charset = getContentTypeCharset(outputMessage.getHeaders().getContentType());
		StreamUtils.copy(str, charset, outputMessage.getBody());
	}


	/**
	 * 返回支持的{@link Charset}列表.
	 * <p>默认{@link Charset#availableCharsets()}.
	 * 可以在子类中重写.
	 * 
	 * @return 已接受的字符集列表
	 */
	protected List<Charset> getAcceptedCharsets() {
		if (this.availableCharsets == null) {
			this.availableCharsets = new ArrayList<Charset>(
					Charset.availableCharsets().values());
		}
		return this.availableCharsets;
	}

	private Charset getContentTypeCharset(MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		else {
			return getDefaultCharset();
		}
	}

}
