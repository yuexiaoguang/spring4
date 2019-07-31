package org.springframework.http.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link HttpMessageConverter}的实现, 可以读取和写入{@link BufferedImage BufferedImages}.
 *
 * <p>默认情况下, 此转换器可以读取{@linkplain ImageIO#getReaderMIMETypes() 注册的图像读取器}支持的所有媒体类型,
 * 并使用第一个可用的{@linkplain javax.imageio.ImageIO#getWriterMIMETypes() 注册的图像写入器}进行写入.
 * 可以通过设置{@link #setDefaultContentType defaultContentType}属性来覆盖后者.
 *
 * <p>如果设置了{@link #setCacheDir cacheDir}属性, 则此转换器将缓存图像数据.
 *
 * <p>{@link #process(ImageReadParam)}和{@link #process(ImageWriteParam)}模板方法允许子类覆盖 Image I/O参数.
 */
public class BufferedImageHttpMessageConverter implements HttpMessageConverter<BufferedImage> {

	private final List<MediaType> readableMediaTypes = new ArrayList<MediaType>();

	private MediaType defaultContentType;

	private File cacheDir;


	public BufferedImageHttpMessageConverter() {
		String[] readerMediaTypes = ImageIO.getReaderMIMETypes();
		for (String mediaType : readerMediaTypes) {
			if (StringUtils.hasText(mediaType)) {
				this.readableMediaTypes.add(MediaType.parseMediaType(mediaType));
			}
		}

		String[] writerMediaTypes = ImageIO.getWriterMIMETypes();
		for (String mediaType : writerMediaTypes) {
			if (StringUtils.hasText(mediaType)) {
				this.defaultContentType = MediaType.parseMediaType(mediaType);
				break;
			}
		}
	}


	/**
	 * 设置用于写入的默认{@code Content-Type}.
	 * 
	 * @throws IllegalArgumentException 如果 Java Image I/O API不支持给定的内容类型
	 */
	public void setDefaultContentType(MediaType defaultContentType) {
		Assert.notNull(defaultContentType, "'contentType' must not be null");
		Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(defaultContentType.toString());
		if (!imageWriters.hasNext()) {
			throw new IllegalArgumentException(
					"Content-Type [" + defaultContentType + "] is not supported by the Java Image I/O API");
		}

		this.defaultContentType = defaultContentType;
	}

	/**
	 * 返回用于写入的默认{@code Content-Type}.
	 * 在没有指定内容类型参数的情况下调用{@link #write}时调用.
	 */
	public MediaType getDefaultContentType() {
		return this.defaultContentType;
	}

	/**
	 * 设置缓存目录. 如果此属性设置为现有目录, 则此转换器将缓存图像数据.
	 */
	public void setCacheDir(File cacheDir) {
		Assert.notNull(cacheDir, "'cacheDir' must not be null");
		Assert.isTrue(cacheDir.isDirectory(), "'cacheDir' is not a directory");
		this.cacheDir = cacheDir;
	}


	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return (BufferedImage.class == clazz && isReadable(mediaType));
	}

	private boolean isReadable(MediaType mediaType) {
		if (mediaType == null) {
			return true;
		}
		Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(mediaType.toString());
		return imageReaders.hasNext();
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return (BufferedImage.class == clazz && isWritable(mediaType));
	}

	private boolean isWritable(MediaType mediaType) {
		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}
		Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(mediaType.toString());
		return imageWriters.hasNext();
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.readableMediaTypes);
	}

	@Override
	public BufferedImage read(Class<? extends BufferedImage> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		ImageInputStream imageInputStream = null;
		ImageReader imageReader = null;
		try {
			imageInputStream = createImageInputStream(inputMessage.getBody());
			MediaType contentType = inputMessage.getHeaders().getContentType();
			Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(contentType.toString());
			if (imageReaders.hasNext()) {
				imageReader = imageReaders.next();
				ImageReadParam irp = imageReader.getDefaultReadParam();
				process(irp);
				imageReader.setInput(imageInputStream, true);
				return imageReader.read(0, irp);
			}
			else {
				throw new HttpMessageNotReadableException(
						"Could not find javax.imageio.ImageReader for Content-Type [" + contentType + "]");
			}
		}
		finally {
			if (imageReader != null) {
				imageReader.dispose();
			}
			if (imageInputStream != null) {
				try {
					imageInputStream.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

	private ImageInputStream createImageInputStream(InputStream is) throws IOException {
		if (this.cacheDir != null) {
			return new FileCacheImageInputStream(is, cacheDir);
		}
		else {
			return new MemoryCacheImageInputStream(is);
		}
	}

	@Override
	public void write(final BufferedImage image, final MediaType contentType,
			final HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		final MediaType selectedContentType = getContentType(contentType);
		outputMessage.getHeaders().setContentType(selectedContentType);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(OutputStream outputStream) throws IOException {
					writeInternal(image, selectedContentType, outputStream);
				}
			});
		}
		else {
			writeInternal(image, selectedContentType, outputMessage.getBody());
		}
	}

	private MediaType getContentType(MediaType contentType) {
		if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
			contentType = getDefaultContentType();
		}
		Assert.notNull(contentType, "Could not select Content-Type. " +
				"Please specify one through the 'defaultContentType' property.");
		return contentType;
	}

	private void writeInternal(BufferedImage image, MediaType contentType, OutputStream body)
			throws IOException, HttpMessageNotWritableException {

		ImageOutputStream imageOutputStream = null;
		ImageWriter imageWriter = null;
		try {
			Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(contentType.toString());
			if (imageWriters.hasNext()) {
				imageWriter = imageWriters.next();
				ImageWriteParam iwp = imageWriter.getDefaultWriteParam();
				process(iwp);
				imageOutputStream = createImageOutputStream(body);
				imageWriter.setOutput(imageOutputStream);
				imageWriter.write(null, new IIOImage(image, null, null), iwp);
			}
			else {
				throw new HttpMessageNotWritableException(
						"Could not find javax.imageio.ImageWriter for Content-Type [" + contentType + "]");
			}
		}
		finally {
			if (imageWriter != null) {
				imageWriter.dispose();
			}
			if (imageOutputStream != null) {
				try {
					imageOutputStream.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

	private ImageOutputStream createImageOutputStream(OutputStream os) throws IOException {
		if (this.cacheDir != null) {
			return new FileCacheImageOutputStream(os, this.cacheDir);
		}
		else {
			return new MemoryCacheImageOutputStream(os);
		}
	}


	/**
	 * 模板方法, 允许在用于读取图像之前操纵{@link ImageReadParam}.
	 * <p>默认实现为空.
	 */
	protected void process(ImageReadParam irp) {
	}

	/**
	 * 模板方法, 允许在用于写入图像之前操纵{@link ImageWriteParam}.
	 * <p>默认实现为空.
	 */
	protected void process(ImageWriteParam iwp) {
	}

}
