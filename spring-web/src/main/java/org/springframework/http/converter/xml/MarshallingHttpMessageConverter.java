package org.springframework.http.converter.xml;

import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}的实现,
 * 可以使用Spring的{@link Marshaller}和{@link Unmarshaller}抽象来读写XML.
 *
 * <p>此转换器需要{@code Marshaller}和{@code Unmarshaller}才能使用它.
 * 这些可以通过{@linkplain #MarshallingHttpMessageConverter(Marshaller) 构造函数}
 * 或{@linkplain #setMarshaller(Marshaller) bean属性}注入.
 *
 * <p>默认情况下, 此转换器支持{@code text/xml}和{@code application/xml}.
 * 可以通过设置{@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}属性来覆盖.
 */
public class MarshallingHttpMessageConverter extends AbstractXmlHttpMessageConverter<Object> {

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;


	/**
	 * 不设置{@link Marshaller} 或 {@link Unmarshaller}.
	 * Marshaller和Unmarshaller必须在构造后,
	 * 通过调用{@link #setMarshaller(Marshaller)}和{@link #setUnmarshaller(Unmarshaller)}来设置.
	 */
	public MarshallingHttpMessageConverter() {
	}

	/**
	 * 使用给定的{@link Marshaller}.
	 * <p>如果给定的{@link Marshaller}也实现了{@link Unmarshaller}接口, 它将用于编组和解组.
	 * <p>请注意, Spring中的所有{@code Marshaller}实现也实现了{@code Unmarshaller}接口, 因此可以安全地使用此构造函数.
	 * 
	 * @param marshaller 用作marshaller和unmarshaller的对象
	 */
	public MarshallingHttpMessageConverter(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
		if (marshaller instanceof Unmarshaller) {
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}

	/**
	 * @param marshaller 要使用的Marshaller
	 * @param unmarshaller 要使用的Unmarshaller
	 */
	public MarshallingHttpMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		Assert.notNull(unmarshaller, "Unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}


	/**
	 * 设置此消息转换器使用的{@link Marshaller}.
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 设置此消息转换器使用的{@link Unmarshaller}.
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}


	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return (canRead(mediaType) && this.unmarshaller != null && this.unmarshaller.supports(clazz));
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return (canWrite(mediaType) && this.marshaller != null && this.marshaller.supports(clazz));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead()/canWrite()
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws IOException {
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
		try {
			Object result = this.unmarshaller.unmarshal(source);
			if (!clazz.isInstance(result)) {
				throw new TypeMismatchException(result, clazz);
			}
			return result;
		}
		catch (UnmarshallingFailureException ex) {
			throw new HttpMessageNotReadableException("Could not read [" + clazz + "]", ex);
		}
	}

	@Override
	protected void writeToResult(Object o, HttpHeaders headers, Result result) throws IOException {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		try {
			this.marshaller.marshal(o, result);
		}
		catch (MarshallingFailureException ex) {
			throw new HttpMessageNotWritableException("Could not write [" + o + "]", ex);
		}
	}
}
