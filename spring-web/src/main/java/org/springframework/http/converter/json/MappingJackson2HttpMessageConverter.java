package org.springframework.http.converter.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter}的实现,
 * 使用<a href="http://wiki.fasterxml.com/JacksonHome">Jackson 2.x</a>的 {@link ObjectMapper}读写JSON.
 *
 * <p>此转换器可用于绑定到类型化的bean或无类型的{@code HashMap}实例.
 *
 * <p>默认情况下, 此转换器支持使用{@code UTF-8}字符集的{@code application/json}和{@code application/*+json}.
 * 可以通过设置{@link #setSupportedMediaTypes supportedMediaTypes}属性来覆盖.
 *
 * <p>默认构造函数使用{@link Jackson2ObjectMapperBuilder}提供的默认配置.
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public class MappingJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	private String jsonPrefix;


	/**
	 * 使用{@link Jackson2ObjectMapperBuilder}提供的默认配置
	 * 构造一个新的{@link MappingJackson2HttpMessageConverter}.
	 */
	public MappingJackson2HttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.json().build());
	}

	/**
	 * 可以使用{@link Jackson2ObjectMapperBuilder}轻松构建它.
	 */
	public MappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
	}

	/**
	 * 指定用于此视图的JSON输出的自定义前缀.
	 * 默认无.
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示此视图的JSON输出是否应以 ")]}', "为前缀. 默认 false.
	 * <p>以这种方式对JSON字符串加前缀, 用于帮助防止JSON劫持.
	 * 前缀使字符串在脚本语法上无效, 因此无法被劫持.
	 * 在将字符串解析为JSON之前, 应该删除此前缀.
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}


	@Override
	@SuppressWarnings("deprecation")
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
		if (this.jsonPrefix != null) {
			generator.writeRaw(this.jsonPrefix);
		}
		String jsonpFunction =
				(object instanceof MappingJacksonValue ? ((MappingJacksonValue) object).getJsonpFunction() : null);
		if (jsonpFunction != null) {
			generator.writeRaw("/**/");
			generator.writeRaw(jsonpFunction + "(");
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
		String jsonpFunction =
				(object instanceof MappingJacksonValue ? ((MappingJacksonValue) object).getJsonpFunction() : null);
		if (jsonpFunction != null) {
			generator.writeRaw(");");
		}
	}

}
