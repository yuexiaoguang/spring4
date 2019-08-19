package org.springframework.web.servlet.view.xml;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.json.AbstractJackson2View;

/**
 * Spring MVC {@link View}, 通过使用<a href="http://wiki.fasterxml.com/JacksonHome">Jackson 2's</a> {@link XmlMapper}
 * 序列化当前请求的模型来呈现XML内容.
 *
 * <p>要序列化的Object作为模型中的参数提供. 使用第一个可序列化条目.
 * 用户可以通过{@link #setModelKey(String) sourceKey}属性在模型中指定特定条目.
 *
 * <p>默认构造函数使用{@link Jackson2ObjectMapperBuilder}提供的默认配置.
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public class MappingJackson2XmlView extends AbstractJackson2View {

	public static final String DEFAULT_CONTENT_TYPE = "application/xml";


	private String modelKey;


	/**
	 * 使用{@link Jackson2ObjectMapperBuilder}提供的默认配置, 并将内容类型设置为{@code application/xml}.
	 */
	public MappingJackson2XmlView() {
		super(Jackson2ObjectMapperBuilder.xml().build(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * 使用提供的{@link XmlMapper}, 并将内容类型设置为{@code application/xml}.
	 */
	public MappingJackson2XmlView(XmlMapper xmlMapper) {
		super(xmlMapper, DEFAULT_CONTENT_TYPE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	/**
	 * 从给定模型中过滤掉不需要的属性.
	 * 返回值可以是另一个{@link Map}或单个值对象.
	 * 
	 * @param model 模型, 传递给{@link #renderMergedOutputModel}
	 * 
	 * @return 要渲染的值
	 */
	@Override
	protected Object filterModel(Map<String, Object> model) {
		Object value = null;
		if (this.modelKey != null) {
			value = model.get(this.modelKey);
			if (value == null) {
				throw new IllegalStateException(
						"Model contains no object with key [" + this.modelKey + "]");
			}
		}
		else {
			for (Map.Entry<String, Object> entry : model.entrySet()) {
				if (!(entry.getValue() instanceof BindingResult) && !entry.getKey().equals(JsonView.class.getName())) {
					if (value != null) {
						throw new IllegalStateException("Model contains more than one object to render, only one is supported");
					}
					value = entry.getValue();
				}
			}
		}
		return value;
	}
}
