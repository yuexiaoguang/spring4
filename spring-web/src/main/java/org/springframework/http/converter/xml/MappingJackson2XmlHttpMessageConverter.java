package org.springframework.http.converter.xml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}的实现,
 * 可以使用<a href="https://github.com/FasterXML/jackson-dataformat-xml">用于读取和编写XML编码数据的Jackson 2.x扩展组件</a>来读写XML.
 *
 * <p>默认情况下, 此转换器支持{@code application/xml}, {@code text/xml}, 和使用{@code UTF-8}字符集的{@code application/*+xml}.
 * 这可以通过设置{@link #setSupportedMediaTypes supportedMediaTypes}属性来覆盖.
 *
 * <p>默认构造函数使用{@link Jackson2ObjectMapperBuilder}提供的默认配置.
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public class MappingJackson2XmlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	/**
	 * 使用{@code Jackson2ObjectMapperBuilder}提供的默认配置.
	 */
	public MappingJackson2XmlHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.xml().build());
	}

	/**
	 * 使用自定义的{@link ObjectMapper} (必须是{@link XmlMapper}实例).
	 * 可以使用{@link Jackson2ObjectMapperBuilder}轻松构建它.
	 */
	public MappingJackson2XmlHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, new MediaType("application", "xml"),
				new MediaType("text", "xml"),
				new MediaType("application", "*+xml"));
		Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
	}


	/**
	 * {@inheritDoc}
	 * {@code ObjectMapper}参数必须是{@link XmlMapper}实例.
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
		super.setObjectMapper(objectMapper);
	}

}
