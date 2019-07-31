package org.springframework.http.converter.xml;

import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverters}的抽象基类, 用于转换 XML.
 *
 * <p>默认情况下, 此转换器的子类支持{@code text/xml}, {@code application/xml}, 和{@code application/*-xml}.
 * 可以通过设置{@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}属性来覆盖.
 */
public abstract class AbstractXmlHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();


	/**
	 * 用于将{@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}
	 * 设置为{@code text/xml} 和 {@code application/xml}, 以及 {@code application/*-xml}.
	 */
	protected AbstractXmlHttpMessageConverter() {
		super(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml"));
	}


	@Override
	public final T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readFromSource(clazz, inputMessage.getHeaders(), new StreamSource(inputMessage.getBody()));
	}

	@Override
	protected final void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeToResult(t, outputMessage.getHeaders(), new StreamResult(outputMessage.getBody()));
	}

	/**
	 * 将给定的{@code Source}转换为{@code Result}.
	 * 
	 * @param source 源
	 * @param result 结果
	 * 
	 * @throws TransformerException 在转换错误的情况下
	 */
	protected void transform(Source source, Result result) throws TransformerException {
		this.transformerFactory.newTransformer().transform(source, result);
	}


	/**
	 * 从{@link #read(Class, HttpInputMessage)}调用的抽象模板方法.
	 * 
	 * @param clazz 要返回的对象的类型
	 * @param headers HTTP输入header
	 * @param source HTTP输入正文
	 * 
	 * @return 转换后的对象
	 * @throws IOException
	 * @throws HttpMessageNotReadableException 如果转换错误
	 */
	protected abstract T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 从{@link #writeInternal(Object, HttpOutputMessage)}调用的抽象模板方法.
	 * 
	 * @param t 要写入输出消息的对象
	 * @param headers HTTP输出header
	 * @param result HTTP输出正文
	 * 
	 * @throws IOException
	 * @throws HttpMessageNotWritableException 如果转换错误
	 */
	protected abstract void writeToResult(T t, HttpHeaders headers, Result result)
			throws IOException, HttpMessageNotWritableException;

}
