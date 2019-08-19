package org.springframework.web.servlet.view.json;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;

/**
 * Spring MVC {@link View}, 通过使用<a href="http://wiki.fasterxml.com/JacksonHome">Jackson 2's</a> {@link ObjectMapper}
 * 序列化当前请求的模型来呈现JSON内容.
 *
 * <p>默认情况下, 模型Map的全部内容 (特定于框架的类除外) 将编码为JSON.
 * 如果模型只包含一个键, 则可以通过{@link #setExtractValueFromSingleKeyModel}将其解压缩为单独的JSON编码.
 *
 * <p>默认构造函数使用{@link Jackson2ObjectMapperBuilder}提供的默认配置.
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
@SuppressWarnings("deprecation")
public class MappingJackson2JsonView extends AbstractJackson2View {

	/**
	 * 默认内容类型: "application/json".
	 * 通过{@link #setContentType}重写.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/json";

	/**
	 * JSONP的默认内容类型: "application/javascript".
	 * 
	 * @deprecated Will be removed as of Spring Framework 5.1, use
	 * <a href="https://docs.spring.io/spring/docs/4.3.x/spring-framework-reference/html/cors.html">CORS</a> instead.
	 */
	@Deprecated
	public static final String DEFAULT_JSONP_CONTENT_TYPE = "application/javascript";

	/**
	 * 用于验证jsonp回调参数值的模式.
	 */
	private static final Pattern CALLBACK_PARAM_PATTERN = Pattern.compile("[0-9A-Za-z_\\.]*");


	private String jsonPrefix;

	private Set<String> modelKeys;

	private boolean extractValueFromSingleKeyModel = false;

	private Set<String> jsonpParameterNames = new LinkedHashSet<String>();


	/**
	 * 使用{@link Jackson2ObjectMapperBuilder}提供的默认配置, 并将内容类型设置为{@code application/json}.
	 */
	public MappingJackson2JsonView() {
		super(Jackson2ObjectMapperBuilder.json().build(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * 使用提供的{@link ObjectMapper}, 并将内容类型设置为{@code application/json}.
	 */
	public MappingJackson2JsonView(ObjectMapper objectMapper) {
		super(objectMapper, DEFAULT_CONTENT_TYPE);
	}


	/**
	 * 指定用于此视图的JSON输出的自定义前缀.
	 * 默认无.
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示此视图的JSON输出是否应以<tt>")]}', "</tt>为前缀.
	 * 默认为 {@code false}.
	 * <p>以这种方式对JSON字符串添加前缀用于防止JSON劫持.
	 * 前缀使字符串在语法上为无效脚本, 因此无法被劫持.
	 * 在将字符串解析为JSON之前, 应该删除此前缀.
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModelKey(String modelKey) {
		this.modelKeys = Collections.singleton(modelKey);
	}

	/**
	 * 设置应由此视图呈现的模型中的属性.
	 * 设置后, 将忽略所有其他模型属性.
	 */
	public void setModelKeys(Set<String> modelKeys) {
		this.modelKeys = modelKeys;
	}

	/**
	 * 返回应由此视图呈现的模型中的属性.
	 */
	public final Set<String> getModelKeys() {
		return this.modelKeys;
	}

	/**
	 * 设置是否将包含单个属性的模型序列化为Map, 或者是否从模型中提取单个值并直接对其进行序列化.
	 * <p>设置此标志的效果类似于使用{@code MappingJackson2HttpMessageConverter}和{@code @ResponseBody}请求处理方法.
	 * <p>默认为{@code false}.
	 */
	public void setExtractValueFromSingleKeyModel(boolean extractValueFromSingleKeyModel) {
		this.extractValueFromSingleKeyModel = extractValueFromSingleKeyModel;
	}

	/**
	 * 设置JSONP请求参数名称.
	 * 每次请求具有其中一个参数时, 生成的JSON将被包装到由JSONP请求参数值指定名称的函数中.
	 * <p>默认配置的参数名称是"jsonp" 和 "callback".
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/JSONP">JSONP Wikipedia article</a>
	 * @deprecated Will be removed as of Spring Framework 5.1, use
	 * <a href="https://docs.spring.io/spring/docs/4.3.x/spring-framework-reference/html/cors.html">CORS</a> instead.
	 */
	@Deprecated
	public void setJsonpParameterNames(Set<String> jsonpParameterNames) {
		this.jsonpParameterNames = jsonpParameterNames;
	}

	private String getJsonpParameterValue(HttpServletRequest request) {
		if (this.jsonpParameterNames != null) {
			for (String name : this.jsonpParameterNames) {
				String value = request.getParameter(name);
				if (StringUtils.isEmpty(value)) {
					continue;
				}
				if (!isValidJsonpQueryParam(value)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring invalid jsonp parameter value: " + value);
					}
					continue;
				}
				return value;
			}
		}
		return null;
	}

	/**
	 * 验证jsonp查询参数值. 如果默认实现由数字, 字母, 或"_" 和 "."组成, 则返回true.
	 * 无效的参数值将被忽略.
	 * 
	 * @param value 查询参数值, never {@code null}
	 * 
	 * @deprecated Will be removed as of Spring Framework 5.1, use
	 * <a href="https://docs.spring.io/spring/docs/4.3.x/spring-framework-reference/html/cors.html">CORS</a> instead.
	 */
	@Deprecated
	protected boolean isValidJsonpQueryParam(String value) {
		return CALLBACK_PARAM_PATTERN.matcher(value).matches();
	}

	/**
	 * 从给定模型中过滤掉不需要的属性.
	 * 返回值可以是另一个{@link Map}或单个值对象.
	 * <p>默认实现删除{@link #setModelKeys renderedAttributes}属性中未包含的{@link BindingResult}实例和条目.
	 * 
	 * @param model 模型, 传递给{@link #renderMergedOutputModel}
	 * 
	 * @return 要呈现的值
	 */
	@Override
	protected Object filterModel(Map<String, Object> model) {
		Map<String, Object> result = new HashMap<String, Object>(model.size());
		Set<String> modelKeys = (!CollectionUtils.isEmpty(this.modelKeys) ? this.modelKeys : model.keySet());
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			if (!(entry.getValue() instanceof BindingResult) && modelKeys.contains(entry.getKey()) &&
					!entry.getKey().equals(JsonView.class.getName()) &&
					!entry.getKey().equals(FilterProvider.class.getName())) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return (this.extractValueFromSingleKeyModel && result.size() == 1 ? result.values().iterator().next() : result);
	}

	@Override
	protected Object filterAndWrapModel(Map<String, Object> model, HttpServletRequest request) {
		Object value = super.filterAndWrapModel(model, request);
		String jsonpParameterValue = getJsonpParameterValue(request);
		if (jsonpParameterValue != null) {
			if (value instanceof MappingJacksonValue) {
				((MappingJacksonValue) value).setJsonpFunction(jsonpParameterValue);
			}
			else {
				MappingJacksonValue container = new MappingJacksonValue(value);
				container.setJsonpFunction(jsonpParameterValue);
				value = container;
			}
		}
		return value;
	}

	@Override
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
		if (this.jsonPrefix != null) {
			generator.writeRaw(this.jsonPrefix);
		}

		String jsonpFunction = null;
		if (object instanceof MappingJacksonValue) {
			jsonpFunction = ((MappingJacksonValue) object).getJsonpFunction();
		}
		if (jsonpFunction != null) {
			generator.writeRaw("/**/");
			generator.writeRaw(jsonpFunction + "(" );
		}
	}

	@Override
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
		String jsonpFunction = null;
		if (object instanceof MappingJacksonValue) {
			jsonpFunction = ((MappingJacksonValue) object).getJsonpFunction();
		}
		if (jsonpFunction != null) {
			generator.writeRaw(");");
		}
	}

	@Override
	protected void setResponseContentType(HttpServletRequest request, HttpServletResponse response) {
		if (getJsonpParameterValue(request) != null) {
			response.setContentType(DEFAULT_JSONP_CONTENT_TYPE);
		}
		else {
			super.setResponseContentType(request, response);
		}
	}
}
