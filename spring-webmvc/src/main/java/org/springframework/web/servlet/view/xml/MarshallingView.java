package org.springframework.web.servlet.view.xml;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.stream.StreamResult;

import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Spring-MVC {@link View}, 允许响应上下文作为{@link Marshaller}的编组结果呈现.
 *
 * <p>要编组的对象作为模型中的参数提供, 然后在响应呈现期间{@linkplain #locateToBeMarshalled(Map) 检测}.
 * 用户可以通过{@link #setModelKey(String) sourceKey}属性在模型中指定特定条目, 或者让Spring找到Source对象.
 */
public class MarshallingView extends AbstractView {

	/**
	 * 默认内容类型. 作为bean属性可覆盖.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";


	private Marshaller marshaller;

	private String modelKey;


	/**
	 * 构建一个没有{@link Marshaller}的新{@code MarshallingView}.
	 * 必须在构造后通过调用{@link #setMarshaller}来设置编组器.
	 */
	public MarshallingView() {
		setContentType(DEFAULT_CONTENT_TYPE);
		setExposePathVariables(false);
	}

	/**
	 * 使用给定的{@link Marshaller}.
	 */
	public MarshallingView(Marshaller marshaller) {
		this();
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
	}


	/**
	 * 设置此视图使用的{@link Marshaller}.
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 设置表示要编组的对象的模型键的名称.
	 * 如果未指定, 将搜索模型映射以获取支持的值类型.
	 */
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	protected void initApplicationContext() {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
	}


	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Object toBeMarshalled = locateToBeMarshalled(model);
		if (toBeMarshalled == null) {
			throw new IllegalStateException("Unable to locate object to be marshalled in model: " + model);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		this.marshaller.marshal(toBeMarshalled, new StreamResult(baos));

		setResponseContentType(request, response);
		response.setContentLength(baos.size());
		baos.writeTo(response.getOutputStream());
	}

	/**
	 * 找到要编组的对象.
	 * <p>在尝试查找{@linkplain Marshaller#supports(Class) 支持的类型}的对象之前,
	 * 默认实现首先尝试查看配置的{@linkplain #setModelKey(String) 模型键}.
	 * 
	 * @param model 模型 Map
	 * 
	 * @return 要编组的Object (如果没有找到, 则为{@code null})
	 * @throws IllegalStateException 如果编组器不支持{@linkplain #setModelKey(String) 模型键}指定的模型对象
	 */
	protected Object locateToBeMarshalled(Map<String, Object> model) throws IllegalStateException {
		if (this.modelKey != null) {
			Object value = model.get(this.modelKey);
			if (value == null) {
				throw new IllegalStateException("Model contains no object with key [" + this.modelKey + "]");
			}
			if (!isEligibleForMarshalling(this.modelKey, value)) {
				throw new IllegalStateException("Model object [" + value + "] retrieved via key [" +
						this.modelKey + "] is not supported by the Marshaller");
			}
			return value;
		}
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			Object value = entry.getValue();
			if (value != null && (model.size() == 1 || !(value instanceof BindingResult)) &&
					isEligibleForMarshalling(entry.getKey(), value)) {
				return value;
			}
		}
		return null;
	}

	/**
	 * 检查当前视图模型中的给定值是否符合通过配置的{@link Marshaller}进行编组的条件.
	 * <p>默认实现调用{@link Marshaller#supports(Class)}, 首先展开给定的{@link JAXBElement}.
	 * 
	 * @param modelKey 模型中值的键 (never {@code null})
	 * @param value 要检查的值 (never {@code null})
	 * 
	 * @return 是否将该给定值视为合格
	 */
	protected boolean isEligibleForMarshalling(String modelKey, Object value) {
		Class<?> classToCheck = value.getClass();
		if (value instanceof JAXBElement) {
			classToCheck = ((JAXBElement) value).getDeclaredType();
		}
		return this.marshaller.supports(classToCheck);
	}

}
