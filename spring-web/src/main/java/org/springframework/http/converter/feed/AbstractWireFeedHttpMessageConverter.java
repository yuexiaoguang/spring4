package org.springframework.http.converter.feed;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;
import com.rometools.rome.io.WireFeedOutput;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StringUtils;

/**
 * Atom和RSS Feed消息转换器的抽象基类, 使用<a href="https://github.com/rometools/rome">ROME tools</a>项目.
 *
 * <p>><b>NOTE: 从Spring 4.1开始, 这是基于ROME版本1.5的{@code com.rometools}变体. 请升级构建依赖项.</b>
 */
public abstract class AbstractWireFeedHttpMessageConverter<T extends WireFeed> extends AbstractHttpMessageConverter<T> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


	protected AbstractWireFeedHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		WireFeedInput feedInput = new WireFeedInput();
		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = (contentType != null && contentType.getCharset() != null ?
				contentType.getCharset() : DEFAULT_CHARSET);
		try {
			Reader reader = new InputStreamReader(inputMessage.getBody(), charset);
			return (T) feedInput.build(reader);
		}
		catch (FeedException ex) {
			throw new HttpMessageNotReadableException("Could not read WireFeed: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void writeInternal(T wireFeed, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		Charset charset = (StringUtils.hasLength(wireFeed.getEncoding()) ?
				Charset.forName(wireFeed.getEncoding()) : DEFAULT_CHARSET);
		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType != null) {
			contentType = new MediaType(contentType.getType(), contentType.getSubtype(), charset);
			outputMessage.getHeaders().setContentType(contentType);
		}

		WireFeedOutput feedOutput = new WireFeedOutput();
		try {
			Writer writer = new OutputStreamWriter(outputMessage.getBody(), charset);
			feedOutput.output(wireFeed, writer);
		}
		catch (FeedException ex) {
			throw new HttpMessageNotWritableException("Could not write WireFeed: " + ex.getMessage(), ex);
		}
	}

}
