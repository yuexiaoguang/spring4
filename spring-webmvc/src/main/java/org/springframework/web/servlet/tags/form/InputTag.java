package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 数据绑定感知JSP标记, 用于呈现'{@code type}'为'{@code text}'的 HTML '{@code input}'元素.
 */
@SuppressWarnings("serial")
public class InputTag extends AbstractHtmlInputElementTag {

	public static final String SIZE_ATTRIBUTE = "size";

	public static final String MAXLENGTH_ATTRIBUTE = "maxlength";

	public static final String ALT_ATTRIBUTE = "alt";

	public static final String ONSELECT_ATTRIBUTE = "onselect";

	public static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

	@Deprecated
	public static final String READONLY_ATTRIBUTE = "readonly";


	private String size;

	private String maxlength;

	private String alt;

	private String onselect;

	private String autocomplete;


	/**
	 * 设置'{@code size}'属性的值.
	 * 可能是运行时表达式.
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
	 * 设置'{@code maxlength}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setMaxlength(String maxlength) {
		this.maxlength = maxlength;
	}

	/**
	 * 获取'{@code maxlength}'属性的值.
	 */
	protected String getMaxlength() {
		return this.maxlength;
	}

	/**
	 * 设置'{@code alt}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setAlt(String alt) {
		this.alt = alt;
	}

	/**
	 * 获取'{@code alt}'属性的值.
	 */
	protected String getAlt() {
		return this.alt;
	}

	/**
	 * 设置'{@code onselect}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnselect(String onselect) {
		this.onselect = onselect;
	}

	/**
	 * 获取'{@code onselect}'属性的值.
	 */
	protected String getOnselect() {
		return this.onselect;
	}

	/**
	 * 设置'{@code autocomplete}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	/**
	 * 获取'{@code autocomplete}'属性的值.
	 */
	protected String getAutocomplete() {
		return this.autocomplete;
	}


	/**
	 * 将'{@code input}'标记写入提供的{@link TagWriter}.
	 * 使用{@link #getType()}返回的值来确定要呈现的'{@code input}'元素的类型.
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");

		writeDefaultAttributes(tagWriter);
		if (!hasDynamicTypeAttribute()) {
			tagWriter.writeAttribute("type", getType());
		}
		writeValue(tagWriter);

		// 自定义可选的属性
		writeOptionalAttribute(tagWriter, SIZE_ATTRIBUTE, getSize());
		writeOptionalAttribute(tagWriter, MAXLENGTH_ATTRIBUTE, getMaxlength());
		writeOptionalAttribute(tagWriter, ALT_ATTRIBUTE, getAlt());
		writeOptionalAttribute(tagWriter, ONSELECT_ATTRIBUTE, getOnselect());
		writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());

		tagWriter.endTag();
		return SKIP_BODY;
	}

	/**
	 * 将'{@code value}'属性写入提供的{@link TagWriter}.
	 * 子类可以选择覆盖此实现以精确控制何时写入值.
	 */
	protected void writeValue(TagWriter tagWriter) throws JspException {
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		String type = (hasDynamicTypeAttribute() ? (String) getDynamicAttributes().get("type") : getType());
		tagWriter.writeAttribute("value", processFieldValue(getName(), value, type));
	}

	private boolean hasDynamicTypeAttribute() {
		return (getDynamicAttributes() != null && getDynamicAttributes().containsKey("type"));
	}

	/**
	 * 标志{@code type="checkbox"}和{@code type="radio"} 为非法动态属性.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !("type".equals(localName) && ("checkbox".equals(value) || "radio".equals(value)));
	}

	/**
	 * 获取'{@code type}'属性的值.
	 * 子类可以覆盖它以更改呈现的'{@code input}'元素的类型.
	 * 默认为'{@code text}'.
	 */
	protected String getType() {
		return "text";
	}

}
