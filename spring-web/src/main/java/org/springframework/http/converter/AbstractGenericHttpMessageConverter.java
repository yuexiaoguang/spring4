package org.springframework.http.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;

/**
 * 大多数{@link GenericHttpMessageConverter}实现的抽象基类.
 */
public abstract class AbstractGenericHttpMessageConverter<T> extends AbstractHttpMessageConverter<T>
		implements GenericHttpMessageConverter<T> {

	protected AbstractGenericHttpMessageConverter() {
	}

	/**
	 * @param supportedMediaType 支持的媒体类型
	 */
	protected AbstractGenericHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}

	/**
	 * @param supportedMediaTypes 支持的媒体类型
	 */
	protected AbstractGenericHttpMessageConverter(MediaType... supportedMediaTypes) {
		super(supportedMediaTypes);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
		return (type instanceof Class ? canRead((Class<?>) type, mediaType) : canRead(mediaType));
	}

	@Override
	public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
		return canWrite(clazz, mediaType);
	}

	/**
	 * 此实现通过调用{@link #addDefaultHeaders}来设置默认header, 然后调用{@link #writeInternal}.
	 */
	public final void write(final T t, final Type type, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		final HttpHeaders headers = outputMessage.getHeaders();
		addDefaultHeaders(headers, t, contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(final OutputStream outputStream) throws IOException {
					writeInternal(t, type, new HttpOutputMessage() {
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
			writeInternal(t, type, outputMessage);
			outputMessage.getBody().flush();
		}
	}

	@Override
	protected void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeInternal(t, null, outputMessage);
	}

	/**
	 * 写入实际正文的抽象模板方法. 从{@link #write}调用.
	 * 
	 * @param t 要写入输出消息的对象
	 * @param type 要写入的对象的类型 (may be {@code null})
	 * @param outputMessage 要写入的HTTP输出消息
	 * 
	 * @throws IOException
	 * @throws HttpMessageNotWritableException 如果转换错误
	 */
	protected abstract void writeInternal(T t, Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
