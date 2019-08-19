package org.springframework.web.servlet.tags.form;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.jsp.JspException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 抽象基类, 提供实现数据绑定感知JSP标记的常用方法,
 * 用于渲染'{@code type}'为'{@code checkbox}' 或 '{@code radio}'的 <i>多个</i> HTML '{@code input}'元素.
 */
@SuppressWarnings("serial")
public abstract class AbstractMultiCheckedElementTag extends AbstractCheckedElementTag {

	/**
	 * HTML '{@code span}'标记.
	 */
	private static final String SPAN_TAG = "span";


	/**
	 * {@link java.util.Collection}, {@link java.util.Map} 或对象数组,
	 * 用于生成'{@code input type="checkbox/radio"}'标签.
	 */
	private Object items;

	/**
	 * 映射到'{@code input type="checkbox/radio"}'标记的'{@code value}'属性的属性名称.
	 */
	private String itemValue;

	/**
	 * 要作为'{@code input type="checkbox/radio"}'标记的一部分显示的值.
	 */
	private String itemLabel;

	/**
	 * 用于包含'{@code input type="checkbox/radio"}'标记的HTML元素.
	 */
	private String element = SPAN_TAG;

	/**
	 * 在每个'{@code input type="checkbox/radio"}'标签之间使用的分隔符.
	 */
	private String delimiter;


	/**
	 * 设置{@link java.util.Collection}, {@link java.util.Map}或对象数组,
	 * 用于生成'{@code input type="checkbox/radio"}'标签.
	 * <p>通常是运行时表达式.
	 * 
	 * @param items said items
	 */
	public void setItems(Object items) {
		Assert.notNull(items, "'items' must not be null");
		this.items = items;
	}

	/**
	 * 获取{@link java.util.Collection}, {@link java.util.Map}或对象数组,
	 * 用于生成'{@code input type="checkbox/radio"}'标签.
	 */
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到'{@code input type="checkbox/radio"}'标记的'{@code value}'属性的属性名称.
	 * <p>可能是运行时表达式.
	 */
	public void setItemValue(String itemValue) {
		Assert.hasText(itemValue, "'itemValue' must not be empty");
		this.itemValue = itemValue;
	}

	/**
	 * 获取映射到'{@code input type="checkbox/radio"}'标记的'{@code value}'属性的属性名称.
	 */
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置要作为'{@code input type="checkbox/radio"}'标记的一部分显示的值.
	 * <p>可能是运行时表达式.
	 */
	public void setItemLabel(String itemLabel) {
		Assert.hasText(itemLabel, "'itemLabel' must not be empty");
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取要作为'{@code input type="checkbox/radio"}'标记的一部分显示的值.
	 */
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * 设置要在每个'{@code input type="checkbox/radio"}'标记之间使用的分隔符.
	 * <p>默认情况下, 有<em>否</em>分隔符.
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * 返回在每个'{@code input type="radio"}'标记之间使用的分隔符.
	 */
	public String getDelimiter() {
		return this.delimiter;
	}

	/**
	 * 设置用于包含'{@code input type="checkbox/radio"}'标记的HTML元素.
	 * <p>默认为HTML '{@code <span/>}'标记.
	 */
	public void setElement(String element) {
		Assert.hasText(element, "'element' cannot be null or blank");
		this.element = element;
	}

	/**
	 * 获取用于包含'{@code input type="checkbox/radio"}'标记的HTML元素.
	 */
	public String getElement() {
		return this.element;
	}


	/**
	 * 由于正在处理多个HTML元素, 因此也会将计数器附加到指定的ID.
	 */
	@Override
	protected String resolveId() throws JspException {
		Object id = evaluate("id", getId());
		if (id != null) {
			String idString = id.toString();
			return (StringUtils.hasText(idString) ? TagIdGenerator.nextId(idString, this.pageContext) : null);
		}
		return autogenerateId();
	}

	/**
	 * 使用配置的{@link #setItems(Object)}值呈现'{@code input type="radio"}'元素.
	 * 如果值与绑定值匹配, 则将元素标记为已选中.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		Object items = getItems();
		Object itemsObject = (items instanceof String ? evaluate("items", items) : items);

		String itemValue = getItemValue();
		String itemLabel = getItemLabel();
		String valueProperty =
				(itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)) : null);
		String labelProperty =
				(itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)) : null);

		Class<?> boundType = getBindStatus().getValueType();
		if (itemsObject == null && boundType != null && boundType.isEnum()) {
			itemsObject = boundType.getEnumConstants();
		}

		if (itemsObject == null) {
			throw new IllegalArgumentException("Attribute 'items' is required and must be a Collection, an Array or a Map");
		}

		if (itemsObject.getClass().isArray()) {
			Object[] itemsArray = (Object[]) itemsObject;
			for (int i = 0; i < itemsArray.length; i++) {
				Object item = itemsArray[i];
				writeObjectEntry(tagWriter, valueProperty, labelProperty, item, i);
			}
		}
		else if (itemsObject instanceof Collection) {
			final Collection<?> optionCollection = (Collection<?>) itemsObject;
			int itemIndex = 0;
			for (Iterator<?> it = optionCollection.iterator(); it.hasNext(); itemIndex++) {
				Object item = it.next();
				writeObjectEntry(tagWriter, valueProperty, labelProperty, item, itemIndex);
			}
		}
		else if (itemsObject instanceof Map) {
			final Map<?, ?> optionMap = (Map<?, ?>) itemsObject;
			int itemIndex = 0;
			for (Iterator it = optionMap.entrySet().iterator(); it.hasNext(); itemIndex++) {
				Map.Entry entry = (Map.Entry) it.next();
				writeMapEntry(tagWriter, valueProperty, labelProperty, entry, itemIndex);
			}
		}
		else {
			throw new IllegalArgumentException("Attribute 'items' must be an array, a Collection or a Map");
		}

		return SKIP_BODY;
	}

	private void writeObjectEntry(TagWriter tagWriter, String valueProperty,
			String labelProperty, Object item, int itemIndex) throws JspException {

		BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
		Object renderValue;
		if (valueProperty != null) {
			renderValue = wrapper.getPropertyValue(valueProperty);
		}
		else if (item instanceof Enum) {
			renderValue = ((Enum<?>) item).name();
		}
		else {
			renderValue = item;
		}
		Object renderLabel = (labelProperty != null ? wrapper.getPropertyValue(labelProperty) : item);
		writeElementTag(tagWriter, item, renderValue, renderLabel, itemIndex);
	}

	private void writeMapEntry(TagWriter tagWriter, String valueProperty,
			String labelProperty, Map.Entry<?, ?> entry, int itemIndex) throws JspException {

		Object mapKey = entry.getKey();
		Object mapValue = entry.getValue();
		BeanWrapper mapKeyWrapper = PropertyAccessorFactory.forBeanPropertyAccess(mapKey);
		BeanWrapper mapValueWrapper = PropertyAccessorFactory.forBeanPropertyAccess(mapValue);
		Object renderValue = (valueProperty != null ?
				mapKeyWrapper.getPropertyValue(valueProperty) : mapKey.toString());
		Object renderLabel = (labelProperty != null ?
				mapValueWrapper.getPropertyValue(labelProperty) : mapValue.toString());
		writeElementTag(tagWriter, mapKey, renderValue, renderLabel, itemIndex);
	}

	private void writeElementTag(TagWriter tagWriter, Object item, Object value, Object label, int itemIndex)
			throws JspException {

		tagWriter.startTag(getElement());
		if (itemIndex > 0) {
			Object resolvedDelimiter = evaluate("delimiter", getDelimiter());
			if (resolvedDelimiter != null) {
				tagWriter.appendValue(resolvedDelimiter.toString());
			}
		}
		tagWriter.startTag("input");
		String id = resolveId();
		writeOptionalAttribute(tagWriter, "id", id);
		writeOptionalAttribute(tagWriter, "name", getName());
		writeOptionalAttributes(tagWriter);
		tagWriter.writeAttribute("type", getInputType());
		renderFromValue(item, value, tagWriter);
		tagWriter.endTag();
		tagWriter.startTag("label");
		tagWriter.writeAttribute("for", id);
		tagWriter.appendValue(convertToDisplayString(label));
		tagWriter.endTag();
		tagWriter.endTag();
	}

}
