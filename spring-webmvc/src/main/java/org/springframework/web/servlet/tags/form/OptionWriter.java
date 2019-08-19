package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Map;
import javax.servlet.jsp.JspException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * 提供支持功能, 以基于某些源对象呈现'{@code option}'标记列表.
 * 此对象可以是数组, {@link Collection}, 或 {@link Map}.
 * <h3>使用数组或{@link Collection}:</h3>
 * <p>
 * 如果提供数组或{@link Collection}源对象以呈现内部'{@code option}'标记,
 * 可以选择在对象上指定属性的名称, 该属性对应于呈现的'{@code option}'的<em>值</em>(i.e., {@code valueProperty})
 * 和对应于<em>label</em>的属性的名称 (i.e., {@code labelProperty}).
 * 然后在将数组/{@link Collection}的每个元素呈现为'{@code option}'时使用这些属性.
 * 如果省略任一属性名称, 则使用相应数组/{@link Collection}元素的{@link Object#toString()}的值.
 * 但是, 如果该项是枚举, 则使用{@link Enum#name()}作为默认值.
 * </p>
 * <h3>使用{@link Map}:</h3>
 * <p>
 * 也可以选择通过提供{@link Map}作为源对象来呈现'{@code option}'标记.
 * </p>
 * <p>
 * 如果<strong>忽略</strong><em>value</em>和<<em>label</em>的属性名称:
 * </p>
 * <ul>
 * <li>每个{@link Map}条目的{@code key}将对应于呈现的'{@code option}'的<em>value</em></li>
 * <li>每个{@link Map}条目的{@code value}将对应于呈现的'{@code option}'的<em>label</em>.</li>
 * </ul>
 * <p>
 * 如果为<em>value</em>和<em>label</em><strong>提供</strong>属性名称:
 * </p>
 * <ul>
 * <li>将从对应于每个{@link Map}条目的{@code key}的对象上的{@code valueProperty}检索呈现的'{@code option}'的<em>value</em></li>
 * <li>将从与每个{@link Map}条目的{@code value}对应的对象上的{@code labelProperty}中检索呈现的'{@code option}'的<em>label</em>.</li>
 * </ul>
 * <h3>使用这些方法时:</h3>
 * <ul>
 * <li><em>value</em>和<em>label</em>的属性名称被指定为
 * {@link #OptionWriter(Object, BindStatus, String, String, boolean) constructor}的参数.</li>
 * <li>如果'{@code option}'的键{@link #isOptionSelected 匹配}绑定到标记实例的值, 则将其标记为'selected'.</li>
 * </ul>
 */
class OptionWriter {

	private final Object optionSource;

	private final BindStatus bindStatus;

	private final String valueProperty;

	private final String labelProperty;

	private final boolean htmlEscape;


	/**
	 * @param optionSource {@code options}的源 (never {@code null})
	 * @param bindStatus 绑定的值的{@link BindStatus} (never {@code null})
	 * @param valueProperty 用于呈现{@code option}值的属性的名称 (可选)
	 * @param labelProperty 用于呈现{@code option}标签的属性的名称 (可选)
	 */
	public OptionWriter(
			Object optionSource, BindStatus bindStatus, String valueProperty, String labelProperty, boolean htmlEscape) {

		Assert.notNull(optionSource, "'optionSource' must not be null");
		Assert.notNull(bindStatus, "'bindStatus' must not be null");
		this.optionSource = optionSource;
		this.bindStatus = bindStatus;
		this.valueProperty = valueProperty;
		this.labelProperty = labelProperty;
		this.htmlEscape = htmlEscape;
	}


	/**
	 * 将已配置的{@link #optionSource}的'{@code option}'标记写入提供的{@link TagWriter}.
	 */
	public void writeOptions(TagWriter tagWriter) throws JspException {
		if (this.optionSource.getClass().isArray()) {
			renderFromArray(tagWriter);
		}
		else if (this.optionSource instanceof Collection) {
			renderFromCollection(tagWriter);
		}
		else if (this.optionSource instanceof Map) {
			renderFromMap(tagWriter);
		}
		else if (this.optionSource instanceof Class && ((Class<?>) this.optionSource).isEnum()) {
			renderFromEnum(tagWriter);
		}
		else {
			throw new JspException(
					"Type [" + this.optionSource.getClass().getName() + "] is not valid for option items");
		}
	}

	/**
	 * 使用{@link #optionSource}渲染内部'{@code option}'标记.
	 */
	private void renderFromArray(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(this.optionSource), tagWriter);
	}

	/**
	 * 使用提供的{@link Map}作为源渲染内部'{@code option}'标记.
	 */
	private void renderFromMap(TagWriter tagWriter) throws JspException {
		Map<?, ?> optionMap = (Map<?, ?>) this.optionSource;
		for (Map.Entry<?, ?> entry : optionMap.entrySet()) {
			Object mapKey = entry.getKey();
			Object mapValue = entry.getValue();
			Object renderValue = (this.valueProperty != null ?
					PropertyAccessorFactory.forBeanPropertyAccess(mapKey).getPropertyValue(this.valueProperty) :
					mapKey);
			Object renderLabel = (this.labelProperty != null ?
					PropertyAccessorFactory.forBeanPropertyAccess(mapValue).getPropertyValue(this.labelProperty) :
					mapValue);
			renderOption(tagWriter, mapKey, renderValue, renderLabel);
		}
	}

	/**
	 * 使用{@link #optionSource}渲染内部'{@code option}'标记.
	 */
	private void renderFromCollection(TagWriter tagWriter) throws JspException {
		doRenderFromCollection((Collection<?>) this.optionSource, tagWriter);
	}

	/**
	 * 使用{@link #optionSource}渲染内部'{@code option}'标记.
	 */
	private void renderFromEnum(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(((Class<?>) this.optionSource).getEnumConstants()), tagWriter);
	}

	/**
	 * 使用提供的{@link Collection}对象作为源, 渲染内部'{@code option}'标记.
	 * 在呈现'{@code option}'的'{@code value}'时使用{@link #valueProperty}字段的值,
	 * 并在呈现标签时使用 {@link #labelProperty}属性的值.
	 */
	private void doRenderFromCollection(Collection<?> optionCollection, TagWriter tagWriter) throws JspException {
		for (Object item : optionCollection) {
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
			Object value;
			if (this.valueProperty != null) {
				value = wrapper.getPropertyValue(this.valueProperty);
			}
			else if (item instanceof Enum) {
				value = ((Enum<?>) item).name();
			}
			else {
				value = item;
			}
			Object label = (this.labelProperty != null ? wrapper.getPropertyValue(this.labelProperty) : item);
			renderOption(tagWriter, item, value, label);
		}
	}

	/**
	 * 使用提供的值和标签呈现HTML '{@code option}'.
	 * 如果条目本身或其值与绑定值匹配, 则将值标记为'selected'.
	 */
	private void renderOption(TagWriter tagWriter, Object item, Object value, Object label) throws JspException {
		tagWriter.startTag("option");
		writeCommonAttributes(tagWriter);

		String valueDisplayString = getDisplayString(value);
		String labelDisplayString = getDisplayString(label);

		valueDisplayString = processOptionValue(valueDisplayString);

		// 允许渲染值处理一些奇怪的浏览器compat问题.
		tagWriter.writeAttribute("value", valueDisplayString);

		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			tagWriter.writeAttribute("selected", "selected");
		}
		if (isOptionDisabled()) {
			tagWriter.writeAttribute("disabled", "disabled");
		}
		tagWriter.appendValue(labelDisplayString);
		tagWriter.endTag();
	}

	/**
	 * 确定提供的{@code Object}的显示值, 根据需要进行HTML转义.
	 */
	private String getDisplayString(Object value) {
		PropertyEditor editor = (value != null ? this.bindStatus.findEditor(value.getClass()) : null);
		return ValueFormatter.getDisplayString(value, editor, this.htmlEscape);
	}

	/**
	 * 在写入之前处理选项值.
	 * <p>默认实现只返回相同的值.
	 */
	protected String processOptionValue(String resolvedValue) {
		return resolvedValue;
	}

	/**
	 * 确定提供的值是否与所选值匹配.
	 * <p>委托给{@link SelectedValueComparator#isSelected}.
	 */
	private boolean isOptionSelected(Object resolvedValue) {
		return SelectedValueComparator.isSelected(this.bindStatus, resolvedValue);
	}

	/**
	 * 确定是否应禁用选项字段.
	 */
	protected boolean isOptionDisabled() throws JspException {
		return false;
	}

	/**
	 * 将默认属性写入提供的{@link TagWriter}.
	 */
	protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
	}

}
