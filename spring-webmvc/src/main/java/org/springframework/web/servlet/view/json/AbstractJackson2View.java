package org.springframework.web.servlet.view.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractView;

/**
 * 基于Jackson和内容类型独立{@link AbstractView}实现的抽象基类.
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public abstract class AbstractJackson2View extends AbstractView {

	private ObjectMapper objectMapper;

	private JsonEncoding encoding = JsonEncoding.UTF8;

	private Boolean prettyPrint;

	private boolean disableCaching = true;

	protected boolean updateContentLength = false;


	protected AbstractJackson2View(ObjectMapper objectMapper, String contentType) {
		setObjectMapper(objectMapper);
		setContentType(contentType);
		setExposePathVariables(false);
	}

	/**
	 * 设置此视图的{@code ObjectMapper}.
	 * 如果未设置, 将使用默认的{@link ObjectMapper#ObjectMapper() ObjectMapper}.
	 * <p>设置自定义配置的{@code ObjectMapper}是进一步控制JSON序列化过程的一种方法.
	 * 另一种选择是使用Jackson提供的关于要序列化的类型的注解, 在这种情况下, 不需要自定义配置的ObjectMapper.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
		configurePrettyPrint();
	}

	/**
	 * 返回此视图的{@code ObjectMapper}.
	 */
	public final ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * 设置此视图的{@code JsonEncoding}.
	 * 默认使用{@linkplain JsonEncoding#UTF8 UTF-8}.
	 */
	public void setEncoding(JsonEncoding encoding) {
		Assert.notNull(encoding, "'encoding' must not be null");
		this.encoding = encoding;
	}

	/**
	 * 返回此视图的{@code JsonEncoding}.
	 */
	public final JsonEncoding getEncoding() {
		return this.encoding;
	}

	/**
	 * 在写入输出时是否使用默认的格式化打印器.
	 * 这是设置{@code ObjectMapper}的快捷方式, 如下所示:
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * </pre>
	 * <p>默认值为{@code false}.
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}

	/**
	 * 禁用生成的JSON的缓存.
	 * <p>默认为{@code true}, 这将阻止客户端缓存生成的JSON.
	 */
	public void setDisableCaching(boolean disableCaching) {
		this.disableCaching = disableCaching;
	}

	/**
	 * 是否更新响应的'Content-Length' header.
	 * 设置为{@code true}时, 响应被缓冲以确定内容长度并设置响应的'Content-Length' header.
	 * <p>默认设置为{@code false}.
	 */
	public void setUpdateContentLength(boolean updateContentLength) {
		this.updateContentLength = updateContentLength;
	}

	@Override
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		setResponseContentType(request, response);
		response.setCharacterEncoding(this.encoding.getJavaName());
		if (this.disableCaching) {
			response.addHeader("Cache-Control", "no-store");
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		OutputStream stream = (this.updateContentLength ? createTemporaryOutputStream() : response.getOutputStream());
		Object value = filterAndWrapModel(model, request);

		writeContent(stream, value);
		if (this.updateContentLength) {
			writeToResponse(response, (ByteArrayOutputStream) stream);
		}
	}

	/**
	 * 过滤并将模型包装在{@link MappingJacksonValue}容器中.
	 * 
	 * @param model 模型, 传递给{@link #renderMergedOutputModel}
	 * @param request 当前的HTTP请求
	 * 
	 * @return 要呈现的包装或未包装的值
	 */
	protected Object filterAndWrapModel(Map<String, Object> model, HttpServletRequest request) {
		Object value = filterModel(model);
		Class<?> serializationView = (Class<?>) model.get(JsonView.class.getName());
		FilterProvider filters = (FilterProvider) model.get(FilterProvider.class.getName());
		if (serializationView != null || filters != null) {
			MappingJacksonValue container = new MappingJacksonValue(value);
			container.setSerializationView(serializationView);
			container.setFilters(filters);
			value = container;
		}
		return value;
	}

	/**
	 * 将实际的JSON内容写入流.
	 * 
	 * @param stream 要使用的输出流
	 * @param object 要呈现的值, 从{@link #filterModel}返回
	 * 
	 * @throws IOException 如果写入失败
	 */
	protected void writeContent(OutputStream stream, Object object) throws IOException {
		JsonGenerator generator = this.objectMapper.getFactory().createGenerator(stream, this.encoding);

		writePrefix(generator, object);
		Class<?> serializationView = null;
		FilterProvider filters = null;
		Object value = object;

		if (value instanceof MappingJacksonValue) {
			MappingJacksonValue container = (MappingJacksonValue) value;
			value = container.getValue();
			serializationView = container.getSerializationView();
			filters = container.getFilters();
		}
		if (serializationView != null) {
			this.objectMapper.writerWithView(serializationView).writeValue(generator, value);
		}
		else if (filters != null) {
			this.objectMapper.writer(filters).writeValue(generator, value);
		}
		else {
			this.objectMapper.writeValue(generator, value);
		}
		writeSuffix(generator, object);
		generator.flush();
	}


	/**
	 * 在模型中设置应由此视图呈现的属性.
	 * 设置后, 将忽略所有其他模型属性.
	 */
	public abstract void setModelKey(String modelKey);

	/**
	 * 从给定模型中过滤掉不需要的属性.
	 * 返回值可以是另一个{@link Map}或单个值对象.
	 * 
	 * @param model 模型, 传递给{@link #renderMergedOutputModel}
	 * 
	 * @return 要呈现的值
	 */
	protected abstract Object filterModel(Map<String, Object> model);

	/**
	 * 在主要内容之前写入一个前缀.
	 * 
	 * @param generator 用于写入内容的生成器
	 * @param object 要写入输出消息的对象
	 */
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * 在主要内容后面写入一个后缀.
	 * 
	 * @param generator 用于写入内容的生成器
	 * @param object 要写入输出消息的对象
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
	}

}
