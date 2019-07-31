package org.springframework.http.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

/**
 * 可以读写字节数组的{@link HttpMessageConverter}的实现.
 *
 * <p>默认情况下, 此转换器支持所有媒体类型 ({@code &#42;&#47;&#42;}),
 * 并使用{@code application/octet-stream}的{@code Content-Type}进行写入.
 * 这可以通过设置{@link #setSupportedMediaTypes supportedMediaTypes}属性来覆盖.
 */
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

	public ByteArrayHttpMessageConverter() {
		super(new MediaType("application", "octet-stream"), MediaType.ALL);
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return byte[].class == clazz;
	}

	@Override
	public byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
		long contentLength = inputMessage.getHeaders().getContentLength();
		ByteArrayOutputStream bos =
				new ByteArrayOutputStream(contentLength >= 0 ? (int) contentLength : StreamUtils.BUFFER_SIZE);
		StreamUtils.copy(inputMessage.getBody(), bos);
		return bos.toByteArray();
	}

	@Override
	protected Long getContentLength(byte[] bytes, MediaType contentType) {
		return (long) bytes.length;
	}

	@Override
	protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
		StreamUtils.copy(bytes, outputMessage.getBody());
	}

}
