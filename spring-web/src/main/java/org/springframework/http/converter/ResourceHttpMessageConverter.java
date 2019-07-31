package org.springframework.http.converter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.util.StreamUtils;

/**
 * {@link HttpMessageConverter}的实现, 可以读/写{@link Resource Resources}, 并支持字节范围请求.
 *
 * <p>默认情况下, 此转换器可以读取所有媒体类型.
 * Java Activation Framework (JAF) - 如果可用 - 用于确定写入资源的{@code Content-Type}.
 * 如果JAF不可用, 则使用{@code application/octet-stream}.
 */
public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

	private static final boolean jafPresent = ClassUtils.isPresent(
			"javax.activation.FileTypeMap", ResourceHttpMessageConverter.class.getClassLoader());


	public ResourceHttpMessageConverter() {
		super(MediaType.ALL);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Resource.class.isAssignableFrom(clazz);
	}

	@Override
	protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		if (InputStreamResource.class == clazz) {
			return new InputStreamResource(inputMessage.getBody());
		}
		else if (clazz.isAssignableFrom(ByteArrayResource.class)) {
			byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
			return new ByteArrayResource(body);
		}
		else {
			throw new IllegalStateException("Unsupported resource class: " + clazz);
		}
	}

	@Override
	protected MediaType getDefaultContentType(Resource resource) {
		if (jafPresent) {
			return ActivationMediaTypeFactory.getMediaType(resource);
		}
		else {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	@Override
	protected Long getContentLength(Resource resource, MediaType contentType) throws IOException {
		// 不要尝试在InputStreamResource上确定contentLength - 之后无法读取...
		// Note: 自定义InputStreamResource子类可以提供预先计算的内容长度!
		if (InputStreamResource.class == resource.getClass()) {
			return null;
		}
		long contentLength = resource.contentLength();
		return (contentLength < 0 ? null : contentLength);
	}

	@Override
	protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeContent(resource, outputMessage);
	}

	protected void writeContent(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		try {
			InputStream in = resource.getInputStream();
			try {
				StreamUtils.copy(in, outputMessage.getBody());
			}
			catch (NullPointerException ex) {
				// ignore, see SPR-13620
			}
			finally {
				try {
					in.close();
				}
				catch (Throwable ex) {
					// ignore, see SPR-12999
				}
			}
		}
		catch (FileNotFoundException ex) {
			// ignore, see SPR-12999
		}
	}

}
