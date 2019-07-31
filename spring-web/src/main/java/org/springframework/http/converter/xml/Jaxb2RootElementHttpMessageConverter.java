package org.springframework.http.converter.xml;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}的实现, 可以使用JAXB2读写XML.
 *
 * <p>此转换器可以读取使用{@link XmlRootElement}和{@link XmlType}注解的类,
 * 并写入使用{@link XmlRootElement}注解的类或其子类.
 *
 * <p>请注意, 如果使用来自{@code spring-oxm}模块的Spring的Marshaller/Unmarshaller抽象,
 * 应该使用{@link MarshallingHttpMessageConverter}.
 */
public class Jaxb2RootElementHttpMessageConverter extends AbstractJaxb2HttpMessageConverter<Object> {

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;


	/**
	 * 指示是否应支持DTD解析.
	 * <p>默认值为{@code false}, 表示DTD已禁用.
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
	}

	/**
	 * 是否支持DTD解析.
	 */
	public boolean isSupportDtd() {
		return this.supportDtd;
	}

	/**
	 * 指示转换为Source时是否处理外部XML实体.
	 * <p>默认{@code false}, 表示未解析外部实体.
	 * <p><strong>Note:</strong> 将此选项设置为{@code true}也会自动将{@link #setSupportDtd}设置为{@code true}.
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		this.processExternalEntities = processExternalEntities;
		if (processExternalEntities) {
			setSupportDtd(true);
		}
	}

	/**
	 * 返回是否允许XML外部实体的配置值.
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return (clazz.isAnnotationPresent(XmlRootElement.class) || clazz.isAnnotationPresent(XmlType.class)) &&
				canRead(mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return (AnnotationUtils.findAnnotation(clazz, XmlRootElement.class) != null && canWrite(mediaType));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws IOException {
		try {
			source = processSource(source);
			Unmarshaller unmarshaller = createUnmarshaller(clazz);
			if (clazz.isAnnotationPresent(XmlRootElement.class)) {
				return unmarshaller.unmarshal(source);
			}
			else {
				JAXBElement<?> jaxbElement = unmarshaller.unmarshal(source, clazz);
				return jaxbElement.getValue();
			}
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new HttpMessageNotReadableException("NPE while unmarshalling. " +
						"This can happen due to the presence of DTD declarations which are disabled.", ex);
			}
			throw ex;
		}
		catch (UnmarshalException ex) {
			throw new HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex.getMessage(), ex);
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	protected Source processSource(Source source) {
		if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			InputSource inputSource = new InputSource(streamSource.getInputStream());
			try {
				XMLReader xmlReader = XMLReaderFactory.createXMLReader();
				xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
				String featureName = "http://xml.org/sax/features/external-general-entities";
				xmlReader.setFeature(featureName, isProcessExternalEntities());
				if (!isProcessExternalEntities()) {
					xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
				}
				return new SAXSource(xmlReader, inputSource);
			}
			catch (SAXException ex) {
				logger.warn("Processing of external entities could not be disabled", ex);
				return source;
			}
		}
		else {
			return source;
		}
	}

	@Override
	protected void writeToResult(Object o, HttpHeaders headers, Result result) throws IOException {
		try {
			Class<?> clazz = ClassUtils.getUserClass(o);
			Marshaller marshaller = createMarshaller(clazz);
			setCharset(headers.getContentType(), marshaller);
			marshaller.marshal(o, result);
		}
		catch (MarshalException ex) {
			throw new HttpMessageNotWritableException("Could not marshal [" + o + "]: " + ex.getMessage(), ex);
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	private void setCharset(MediaType contentType, Marshaller marshaller) throws PropertyException {
		if (contentType != null && contentType.getCharset() != null) {
			marshaller.setProperty(Marshaller.JAXB_ENCODING, contentType.getCharset().name());
		}
	}


	private static final EntityResolver NO_OP_ENTITY_RESOLVER = new EntityResolver() {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource(new StringReader(""));
		}
	};

}
