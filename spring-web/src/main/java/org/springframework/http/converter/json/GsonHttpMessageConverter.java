package org.springframework.http.converter.json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter}的实现,
 * 可以使用<a href="https://code.google.com/p/google-gson/">Google Gson</a>库的 {@link Gson}类读写JSON.
 *
 * <p>此转换器可用于绑定到类型化的bean或无类型的{@code HashMap}.
 * 默认情况下, 它支持使用{@code UTF-8}字符集的{@code application/json} 和 {@code application/*+json}.
 *
 * <p>测试了Gson 2.8; 兼容Gson 2.0及更高版本.
 */
public class GsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


	private Gson gson = new Gson();

	private String jsonPrefix;


	public GsonHttpMessageConverter() {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		setDefaultCharset(DEFAULT_CHARSET);
	}


	/**
	 * 设置要使用的{@code Gson}实例.
	 * 如果未设置, 将使用默认的{@link Gson#Gson() Gson}实例.
	 * <p>设置自定义配置的{@code Gson}是进一步控制JSON序列化过程的一种方法.
	 */
	public void setGson(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}

	/**
	 * 返回此转换器的已配置{@code Gson}实例.
	 */
	public Gson getGson() {
		return this.gson;
	}

	/**
	 * 指定用于JSON输出的自定义前缀. 默认无.
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示此视图的JSON输出是否应以 ")]}', "为前缀.
	 * 默认{@code false}.
	 * <p>以这种方式为JSON字符串添加前缀, 用于防止JSON劫持.
	 * 前缀使字符串在脚本语法上无效, 因此无法被劫持.
	 * 在将字符串解析为JSON之前, 应该删除此前缀.
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}


	@Override
	@SuppressWarnings("deprecation")
	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		TypeToken<?> token = getTypeToken(type);
		return readTypeToken(token, inputMessage);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		TypeToken<?> token = getTypeToken(clazz);
		return readTypeToken(token, inputMessage);
	}

	/**
	 * 返回指定类型的Gson {@link TypeToken}.
	 * <p>默认实现返回{@code TypeToken.get(type)}, 但这可以在子类中重写, 以允许自定义泛型集合处理.
	 * For instance:
	 * <pre class="code">
	 * protected TypeToken<?> getTypeToken(Type type) {
	 *   if (type instanceof Class && List.class.isAssignableFrom((Class<?>) type)) {
	 *     return new TypeToken<ArrayList<MyBean>>() {};
	 *   }
	 *   else {
	 *     return super.getTypeToken(type);
	 *   }
	 * }
	 * </pre>
	 * 
	 * @param type 要返回TypeToken的类型
	 * 
	 * @return 类型token
	 * @deprecated as of Spring Framework 4.3.8, in favor of signature-based resolution
	 */
	@Deprecated
	protected TypeToken<?> getTypeToken(Type type) {
		return TypeToken.get(type);
	}

	private Object readTypeToken(TypeToken<?> token, HttpInputMessage inputMessage) throws IOException {
		Reader json = new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()));
		try {
			return this.gson.fromJson(json, token.getType());
		}
		catch (JsonParseException ex) {
			throw new HttpMessageNotReadableException("JSON parse error: " + ex.getMessage(), ex);
		}
	}

	private Charset getCharset(HttpHeaders headers) {
		if (headers == null || headers.getContentType() == null || headers.getContentType().getCharset() == null) {
			return DEFAULT_CHARSET;
		}
		return headers.getContentType().getCharset();
	}

	@Override
	protected void writeInternal(Object o, Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		Charset charset = getCharset(outputMessage.getHeaders());
		OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(), charset);
		try {
			if (this.jsonPrefix != null) {
				writer.append(this.jsonPrefix);
			}

			// 在Gson中, 带有类型参数的toJson将专门使用该给定类型, 忽略对象的实际类型... 这可能更具体,
			// e.g. 指定类型的子类, 包含其他字段.
			// 因此, 只传递参数化类型声明, 这些声明可能包含对象实例未保留的额外泛型.
			if (type instanceof ParameterizedType) {
				this.gson.toJson(o, type, writer);
			}
			else {
				this.gson.toJson(o, writer);
			}

			writer.flush();
		}
		catch (JsonIOException ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

}
