package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

import org.springframework.web.servlet.support.RequestDataValueProcessor;

/**
 * HTML按钮标记. 如果应用程序依赖{@link RequestDataValueProcessor}, 则提供此标记是为了完整性.
 */
@SuppressWarnings("serial")
public class ButtonTag extends AbstractHtmlElementTag {

	/**
	 * '{@code disabled}'属性的名称.
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";


	private TagWriter tagWriter;

	private String name;

	private String value;

	private boolean disabled;


	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isDisabled() {
		return this.disabled;
	}


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("button");
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute("type", getType());
		writeValue(tagWriter);
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		tagWriter.forceBlock();
		this.tagWriter = tagWriter;
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 将 '{@code value}'属性写入提供的{@link TagWriter}.
	 * 子类可以选择覆盖此实现以精确控制何时写入值.
	 */
	protected void writeValue(TagWriter tagWriter) throws JspException {
		String valueToUse = (getValue() != null ? getValue() : getDefaultValue());
		tagWriter.writeAttribute("value", processFieldValue(getName(), valueToUse, getType()));
	}

	/**
	 * 返回默认值.
	 * 
	 * @return 默认值
	 */
	protected String getDefaultValue() {
		return "Submit";
	}

	/**
	 * 获取'{@code type}'属性的值.
	 * 子类可以覆盖它以更改呈现的 '{@code input}'元素的类型. 默认值为 '{@code submit}'.
	 */
	protected String getType() {
		return "submit";
	}

	/**
	 * 关闭 '{@code button}'块标记.
	 */
	@Override
	public int doEndTag() throws JspException {
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

}
