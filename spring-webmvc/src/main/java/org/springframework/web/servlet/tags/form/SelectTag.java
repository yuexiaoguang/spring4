package org.springframework.web.servlet.tags.form;

import java.util.Collection;
import java.util.Map;
import javax.servlet.jsp.JspException;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.support.BindStatus;

/**
 * 渲染HTML '{@code select}'元素的数据绑定感知JSP标记.
 *
 * <p>可以使用OptionWriter类支持的方法之一渲染内部'{@code option}'标记.
 *
 * <p>还支持使用嵌套的{@link OptionTag OptionTags}或 (通常一个) 嵌套的{@link OptionsTag}.
 */
@SuppressWarnings("serial")
public class SelectTag extends AbstractHtmlInputElementTag {

	/**
	 * {@link javax.servlet.jsp.PageContext}属性, 在该属性下绑定的值将暴露给内部{@link OptionTag OptionTags}.
	 */
	public static final String LIST_VALUE_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.form.SelectTag.listValue";

	/**
	 * 已指定但已解析为null的条目的标记对象.
	 * 允许区分'set but null'和'not set at all'.
	 */
	private static final Object EMPTY = new Object();


	/**
	 * {@link Collection}, {@link Map}或对象数组, 用于生成内部'{@code option}'标记.
	 */
	private Object items;

	/**
	 * 映射到'{@code option}'标记的'{@code value}'属性的属性名称.
	 */
	private String itemValue;

	/**
	 * 映射到'{@code option}'标记的内部文本的属性名称.
	 */
	private String itemLabel;

	/**
	 * 在最终的'{@code select}'元素上呈现的HTML '{@code size}'属性的值.
	 */
	private String size;

	/**
	 * 指示'{@code select}'标记是否允许多项选择.
	 */
	private Object multiple;

	/**
	 * 正在写入输出的{@link TagWriter}实例.
	 * <p>仅与嵌套的{@link OptionTag OptionTags}一起使用.
	 */
	private TagWriter tagWriter;


	/**
	 * 设置用于生成内部'{@code option}'标签的{@link Collection}, {@link Map}或对象数组.
	 * <p>希望从数组, {@link Collection} 或 {@link Map}呈现'{@code option}'标记时需要.
	 * <p>通常是运行时表达式.
	 * 
	 * @param items 包含此选择选项的条目
	 */
	public void setItems(Object items) {
		this.items = (items != null ? items : EMPTY);
	}

	/**
	 * 获取'{@code items}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到'{@code option}'标记的'{@code value}'属性的属性名称.
	 * <p>希望从数组或{@link Collection}呈现'{@code option}'标记时需要.
	 * <p>可能是运行时表达式.
	 */
	public void setItemValue(String itemValue) {
		this.itemValue = itemValue;
	}

	/**
	 * 获取'{@code itemValue}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置映射到'{@code option}'标记的标签(内部文本)的属性的名称.
	 * <p>可能是运行时表达式.
	 */
	public void setItemLabel(String itemLabel) {
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取'{@code itemLabel}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * 设置在最终'{@code select}'元素上呈现的HTML '{@code size}'属性的值.
	 */
	public void setSize(String size) {
		this.size = size;
	}

	/**
	 * 获取'{@code size}'属性的值.
	 */
	protected String getSize() {
		return this.size;
	}

	/**
	 * 设置在最终'{@code select}'元素上呈现的HTML '{@code multiple}'属性的值.
	 */
	public void setMultiple(Object multiple) {
		this.multiple = multiple;
	}

	/**
	 * 获取在最终'{@code select}'元素上呈现的HTML '{@code multiple}'属性的值.
	 */
	protected Object getMultiple() {
		return this.multiple;
	}


	/**
	 * 将HTML '{@code select}'标记呈现给提供的{@link TagWriter}.
	 * <p>如果设置了{@link #setItems items}属性，则呈现嵌套的'{@code option}'标记,
	 * 否则呈现嵌套{@link OptionTag OptionTags}的绑定值.
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("select");
		writeDefaultAttributes(tagWriter);
		if (isMultiple()) {
			tagWriter.writeAttribute("multiple", "multiple");
		}
		tagWriter.writeOptionalAttributeValue("size", getDisplayString(evaluate("size", getSize())));

		Object items = getItems();
		if (items != null) {
			// Items specified, but might still be empty...
			if (items != EMPTY) {
				Object itemsObject = evaluate("items", items);
				if (itemsObject != null) {
					final String selectName = getName();
					String valueProperty = (getItemValue() != null ?
							ObjectUtils.getDisplayString(evaluate("itemValue", getItemValue())) : null);
					String labelProperty = (getItemLabel() != null ?
							ObjectUtils.getDisplayString(evaluate("itemLabel", getItemLabel())) : null);
					OptionWriter optionWriter =
							new OptionWriter(itemsObject, getBindStatus(), valueProperty, labelProperty, isHtmlEscape()) {
								@Override
								protected String processOptionValue(String resolvedValue) {
									return processFieldValue(selectName, resolvedValue, "option");
								}
							};
					optionWriter.writeOptions(tagWriter);
				}
			}
			tagWriter.endTag(true);
			writeHiddenTagIfNecessary(tagWriter);
			return SKIP_BODY;
		}
		else {
			// 使用嵌套的<form:option/>标记, 只需公开PageContext中的值即可...
			tagWriter.forceBlock();
			this.tagWriter = tagWriter;
			this.pageContext.setAttribute(LIST_VALUE_PAGE_ATTRIBUTE, getBindStatus());
			return EVAL_BODY_INCLUDE;
		}
	}

	/**
	 * 如果使用多选, 则需要一个隐藏的元素以确保在服务器端正确取消选择所有条目, 以响应{@code null} post.
	 */
	private void writeHiddenTagIfNecessary(TagWriter tagWriter) throws JspException {
		if (isMultiple()) {
			tagWriter.startTag("input");
			tagWriter.writeAttribute("type", "hidden");
			String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
			tagWriter.writeAttribute("name", name);
			tagWriter.writeAttribute("value", processFieldValue(name, "1", "hidden"));
			tagWriter.endTag();
		}
	}

	private boolean isMultiple() throws JspException {
		Object multiple = getMultiple();
		if (multiple != null) {
			String stringValue = multiple.toString();
			return ("multiple".equalsIgnoreCase(stringValue) || Boolean.parseBoolean(stringValue));
		}
		return forceMultiple();
	}

	/**
	 * 如果绑定值需要生成的'{@code select}' 标记为多选, 则返回'{@code true}'.
	 */
	private boolean forceMultiple() throws JspException {
		BindStatus bindStatus = getBindStatus();
		Class<?> valueType = bindStatus.getValueType();
		if (valueType != null && typeRequiresMultiple(valueType)) {
			return true;
		}
		else if (bindStatus.getEditor() != null) {
			Object editorValue = bindStatus.getEditor().getValue();
			if (editorValue != null && typeRequiresMultiple(editorValue.getClass())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回'{@code true}', 如果是数组, {@link Collection Collections} 和 {@link Map Maps}.
	 */
	private static boolean typeRequiresMultiple(Class<?> type) {
		return (type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
	}

	/**
	 * 关闭使用嵌套{@link OptionTag options}时可能已打开的任何块标记.
	 */
	@Override
	public int doEndTag() throws JspException {
		if (this.tagWriter != null) {
			this.tagWriter.endTag();
			writeHiddenTagIfNecessary(this.tagWriter);
		}
		return EVAL_PAGE;
	}

	/**
	 * 清除使用嵌套的{@link OptionTag options}时可能遗留的{@link TagWriter}.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.tagWriter = null;
		this.pageContext.removeAttribute(LIST_VALUE_PAGE_ATTRIBUTE);
	}

}
