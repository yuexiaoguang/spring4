package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

/**
 * 方便的标记, 允许用户提供要在'{@code select}'标记内呈现为'{@code option}'标记的对象集合.
 *
 * <p>必须在{@link SelectTag 'select'标记}中使用.
 */
@SuppressWarnings("serial")
public class OptionsTag extends AbstractHtmlElementTag {

	/**
	 * 用于生成内部'{@code option}'标记的{@link java.util.Collection}, {@link java.util.Map}或对象数组.
	 */
	private Object items;

	/**
	 * 映射到'{@code option}'标记的'{@code value}'属性的属性名称.
	 */
	private String itemValue;

	/**
	 * 映射到'{@code option}'标记内部文本的属性名称.
	 */
	private String itemLabel;

	private boolean disabled;


	/**
	 * 设置用于生成内部'{@code option}'标记的{@link java.util.Collection}, {@link java.util.Map}或对象数组.
	 * <p>希望从数组, {@link java.util.Collection} 或 {@link java.util.Map}呈现'{@code option}'标记时需要.
	 * <p>通常是运行时表达式.
	 */
	public void setItems(Object items) {
		this.items = items;
	}

	/**
	 * 获取用于生成内部'{@code option}'标记的{@link java.util.Collection}, {@link java.util.Map}或对象数组.
	 * <p>通常是运行时表达式.
	 */
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到'{@code option}'标记的'{@code value}'属性的属性名称.
	 * <p>希望从数组或{@link java.util.Collection}呈现'{@code option}'标记时需要.
	 */
	public void setItemValue(String itemValue) {
		Assert.hasText(itemValue, "'itemValue' must not be empty");
		this.itemValue = itemValue;
	}

	/**
	 * 返回映射到'{@code option}'标记的'{@code value}'属性的属性名称.
	 */
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置映射到'{@code option}'标记的标签(内部文本)的属性的名称.
	 */
	public void setItemLabel(String itemLabel) {
		Assert.hasText(itemLabel, "'itemLabel' must not be empty");
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取映射到'{@code option}'标记的标签(内部文本)的属性的名称.
	 */
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * 设置'{@code disabled}'属性的值.
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * 获取'{@code disabled}'属性的值.
	 */
	protected boolean isDisabled() {
		return this.disabled;
	}


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		SelectTag selectTag = getSelectTag();
		Object items = getItems();
		Object itemsObject = null;
		if (items != null) {
			itemsObject = (items instanceof String ? evaluate("items", items) : items);
		}
		else {
			Class<?> selectTagBoundType = selectTag.getBindStatus().getValueType();
			if (selectTagBoundType != null && selectTagBoundType.isEnum()) {
				itemsObject = selectTagBoundType.getEnumConstants();
			}
		}
		if (itemsObject != null) {
			String selectName = selectTag.getName();
			String itemValue = getItemValue();
			String itemLabel = getItemLabel();
			String valueProperty =
					(itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)) : null);
			String labelProperty =
					(itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)) : null);
			OptionsWriter optionWriter = new OptionsWriter(selectName, itemsObject, valueProperty, labelProperty);
			optionWriter.writeOptions(tagWriter);
		}
		return SKIP_BODY;
	}

	/**
	 * 将计数器附加到指定的id, 因为正在处理多个HTML元素.
	 */
	@Override
	protected String resolveId() throws JspException {
		Object id = evaluate("id", getId());
		if (id != null) {
			String idString = id.toString();
			return (StringUtils.hasText(idString) ? TagIdGenerator.nextId(idString, this.pageContext) : null);
		}
		return null;
	}

	private SelectTag getSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "options", "select");
		return (SelectTag) findAncestorWithClass(this, SelectTag.class);
	}

	@Override
	protected BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}


	/**
	 * 内部类, 用于调整OptionWriter以呈现多个选项.
	 */
	private class OptionsWriter extends OptionWriter {

		private final String selectName;

		public OptionsWriter(String selectName, Object optionSource, String valueProperty, String labelProperty) {
			super(optionSource, getBindStatus(), valueProperty, labelProperty, isHtmlEscape());
			this.selectName = selectName;
		}

		@Override
		protected boolean isOptionDisabled() throws JspException {
			return isDisabled();
		}

		@Override
		protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
			writeOptionalAttribute(tagWriter, "id", resolveId());
			writeOptionalAttributes(tagWriter);
		}

		@Override
		protected String processOptionValue(String value) {
			return processFieldValue(this.selectName, value, "option");
		}
	}
}
