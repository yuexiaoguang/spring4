package org.springframework.http.converter.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.Assert;

/**
 * 使用JAXB2的{@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverters}的抽象基类.
 * 延迟创建{@link JAXBContext}对象.
 */
public abstract class AbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<Class<?>, JAXBContext>(64);


	/**
	 * 为给定的类创建一个新的{@link Marshaller}.
	 * 
	 * @param clazz 用于创建编组器的类
	 * 
	 * @return the {@code Marshaller}
	 * @throws HttpMessageConversionException 在JAXB错误的情况下
	 */
	protected final Marshaller createMarshaller(Class<?> clazz) {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			Marshaller marshaller = jaxbContext.createMarshaller();
			customizeMarshaller(marshaller);
			return marshaller;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException(
					"Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 在使用它将对象写入输出之前, 自定义此消息转换器创建的{@link Marshaller}.
	 * 
	 * @param marshaller 要自定义的编组器
	 */
	protected void customizeMarshaller(Marshaller marshaller) {
	}

	/**
	 * 为给定的类创建一个新的{@link Unmarshaller}.
	 * 
	 * @param clazz 用于创建解组器的类
	 * 
	 * @return the {@code Unmarshaller}
	 * @throws HttpMessageConversionException 在JAXB错误的情况下
	 */
	protected final Unmarshaller createUnmarshaller(Class<?> clazz) {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			customizeUnmarshaller(unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException(
					"Could not create Unmarshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 自定义此消息转换器创建的{@link Unmarshaller}, 然后使用它从输入中读取对象.
	 * 
	 * @param unmarshaller 要自定义的解组器
	 */
	protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
	}

	/**
	 * 返回给定的类的{@link JAXBContext}.
	 * 
	 * @param clazz 要返回上下文的类
	 * 
	 * @return the {@code JAXBContext}
	 * @throws HttpMessageConversionException 在JAXB错误的情况下
	 */
	protected final JAXBContext getJaxbContext(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		JAXBContext jaxbContext = this.jaxbContexts.get(clazz);
		if (jaxbContext == null) {
			try {
				jaxbContext = JAXBContext.newInstance(clazz);
				this.jaxbContexts.putIfAbsent(clazz, jaxbContext);
			}
			catch (JAXBException ex) {
				throw new HttpMessageConversionException(
						"Could not instantiate JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		}
		return jaxbContext;
	}

}
