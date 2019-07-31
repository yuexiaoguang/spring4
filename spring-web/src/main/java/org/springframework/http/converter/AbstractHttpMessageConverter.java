package org.springframework.http.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;

/**
 * 大多数{@link HttpMessageConverter}实现的抽象基类.
 *
 * <p>此基类通过{@link #setSupportedMediaTypes(List) supportedMediaTypes} bean属性
 * 添加了对设置支持的{@code MediaTypes}的支持.
 * 在写入输出消息时, 它还增加了对{@code Content-Type}和{@code Content-Length}的支持.
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private List<MediaType> supportedMediaTypes = Collections.emptyList();

	private Charset defaultCharset;


	protected AbstractHttpMessageConverter() {
	}

	/**
	 * @param supportedMediaType 支持的媒体类型
	 */
	protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	/**
	 * @param supportedMediaTypes 支持的媒体类型
	 */
	protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}

	/**
	 * @param defaultCharset 默认字符集
	 * @param supportedMediaTypes 支持的媒体类型
	 */
	protected AbstractHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
		this.defaultCharset = defaultCharset;
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}


	/**
	 * 设置此转换器支持的{@link MediaType}对象列表.
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.supportedMediaTypes = new ArrayList<MediaType>(supportedMediaTypes);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	/**
	 * 设置默认字符集.
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	/**
	 * 返回默认字符集.
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}


	/**
	 * 此实现检查是否{@linkplain #supports(Class) 支持}给定的类,
	 * 以及{@linkplain #getSupportedMediaTypes() 支持的媒体类型}是否 {@linkplain MediaType#includes(MediaType) 包括}给定的媒体类型.
	 */
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && canRead(mediaType);
	}

	/**
	 * 如果{@linkplain #setSupportedMediaTypes(List) 支持的}媒体类型
	 * {@link MediaType#includes(MediaType) 包括}给定的媒体类型, 则返回{@code true}.
	 * 
	 * @param mediaType 要读取的媒体类型, 可以是{@code null}.
	 * 通常是{@code Content-Type} header的值.
	 * 
	 * @return {@code true} 如果支持的媒体类型包括媒体类型, 或如果媒体类型是{@code null}
	 */
	protected boolean canRead(MediaType mediaType) {
		if (mediaType == null) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 此实现检查是否{@linkplain #supports(Class) 支持}给定的类,
	 * 以及{@linkplain #getSupportedMediaTypes() 支持的媒体类型}是否 {@linkplain MediaType#includes(MediaType) 包括}给定的媒体类型.
	 */
	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && canWrite(mediaType);
	}

	/**
	 * 如果给定的媒体类型包含{@linkplain #setSupportedMediaTypes(List) 支持的媒体类型}中的任何一种, 则返回{@code true}.
	 * 
	 * @param mediaType 要写入的媒体类型, 可以是{@code null}.
	 * 通常是{@code Accept} header的值.
	 * 
	 * @return {@code true} 如果支持的媒体类型与媒体类型兼容, 或者如果媒体类型是{@code null}
	 */
	protected boolean canWrite(MediaType mediaType) {
		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 这个实现简单委托给{@link #readInternal(Class, HttpInputMessage)}.
	 * 但是, 未来的实现可能会添加一些默认行为.
	 */
	@Override
	public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readInternal(clazz, inputMessage);
	}

	/**
	 * 此实现通过调用{@link #addDefaultHeaders}来设置默认header, 然后调用{@link #writeInternal}.
	 */
	@Override
	public final void write(final T t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		final HttpHeaders headers = outputMessage.getHeaders();
		addDefaultHeaders(headers, t, contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(final OutputStream outputStream) throws IOException {
					writeInternal(t, new HttpOutputMessage() {
						@Override
						public OutputStream getBody() throws IOException {
							return outputStream;
						}
						@Override
						public HttpHeaders getHeaders() {
							return headers;
						}
					});
				}
			});
		}
		else {
			writeInternal(t, outputMessage);
			outputMessage.getBody().flush();
		}
	}

	/**
	 * 将默认header添加到输出消息.
	 * <p>如果未提供内容类型, 则此实现委托给{@link #getDefaultContentType(Object)},
	 * 必要时设置默认字符集, 调用{@link #getContentLength}, 并设置相应的header.
	 */
	protected void addDefaultHeaders(HttpHeaders headers, T t, MediaType contentType) throws IOException {
		if (headers.getContentType() == null) {
			MediaType contentTypeToUse = contentType;
			if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
				contentTypeToUse = getDefaultContentType(t);
			}
			else if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
				MediaType mediaType = getDefaultContentType(t);
				contentTypeToUse = (mediaType != null ? mediaType : contentTypeToUse);
			}
			if (contentTypeToUse != null) {
				if (contentTypeToUse.getCharset() == null) {
					Charset defaultCharset = getDefaultCharset();
					if (defaultCharset != null) {
						contentTypeToUse = new MediaType(contentTypeToUse, defaultCharset);
					}
				}
				headers.setContentType(contentTypeToUse);
			}
		}
		if (headers.getContentLength() < 0 && !headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
			Long contentLength = getContentLength(t, headers.getContentType());
			if (contentLength != null) {
				headers.setContentLength(contentLength);
			}
		}
	}

	/**
	 * 返回给定类型的默认内容类型. 在没有指定内容类型参数的情况下调用{@link #write}时调用.
	 * <p>默认情况下, 这将返回{@link #setSupportedMediaTypes(List) supportedMediaTypes}属性的第一个元素.
	 * 可以在子类中重写.
	 * 
	 * @param t 要返回内容类型的类型
	 * 
	 * @return 内容类型, 或{@code null}
	 */
	protected MediaType getDefaultContentType(T t) throws IOException {
		List<MediaType> mediaTypes = getSupportedMediaTypes();
		return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
	}

	/**
	 * 返回给定类型的内容长度.
	 * <p>默认情况下, 返回{@code null}, 表示内容长度未知.
	 * 可以在子类中重写.
	 * 
	 * @param t 返回内容长度的类型
	 * 
	 * @return 内容长度, 或{@code null}
	 */
	protected Long getContentLength(T t, MediaType contentType) throws IOException {
		return null;
	}


	/**
	 * 指示此转换器是否支持给定的类.
	 * 
	 * @param clazz 要测试是否支持的类
	 * 
	 * @return {@code true}如果支持; 否则{@code false}
	 */
	protected abstract boolean supports(Class<?> clazz);

	/**
	 * 读取实际对象的抽象模板方法. 从{@link #read}调用.
	 * 
	 * @param clazz 要返回的对象的类型
	 * @param inputMessage 要读取的HTTP输入消息
	 * 
	 * @return 转换后的对象
	 * @throws IOException
	 * @throws HttpMessageNotReadableException 如果转换错误
	 */
	protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 写入实际正文的抽象模板方法. 从{@link #write}调用.
	 * 
	 * @param t 要写入输出消息的对象
	 * @param outputMessage 要写入的HTTP输出消息
	 * 
	 * @throws IOException
	 * @throws HttpMessageNotWritableException 如果转换错误
	 */
	protected abstract void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
