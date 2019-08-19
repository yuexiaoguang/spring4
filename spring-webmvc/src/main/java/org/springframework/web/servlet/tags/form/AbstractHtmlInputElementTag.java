package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 用于呈现HTML表单输入元素的数据绑定感知JSP标记的基类.
 *
 * <p>提供一组属性, 这些属性对应于表单输入元素中通用的HTML属性集.
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlInputElementTag extends AbstractHtmlElementTag {

	/**
	 * '{@code onfocus}'属性的名称.
	 */
	public static final String ONFOCUS_ATTRIBUTE = "onfocus";

	/**
	 * '{@code onblur}'属性的名称.
	 */
	public static final String ONBLUR_ATTRIBUTE = "onblur";

	/**
	 * '{@code onchange}'属性的名称.
	 */
	public static final String ONCHANGE_ATTRIBUTE = "onchange";

	/**
	 * '{@code accesskey}'属性的名称.
	 */
	public static final String ACCESSKEY_ATTRIBUTE = "accesskey";

	/**
	 * '{@code disabled}'属性的名称.
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";

	/**
	 * '{@code readonly}'属性的名称.
	 */
	public static final String READONLY_ATTRIBUTE = "readonly";


	private String onfocus;

	private String onblur;

	private String onchange;

	private String accesskey;

	private boolean disabled;

	private boolean readonly;


	/**
	 * 设置'{@code onfocus}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnfocus(String onfocus) {
		this.onfocus = onfocus;
	}

	/**
	 * 获取'{@code onfocus}'属性的值.
	 */
	protected String getOnfocus() {
		return this.onfocus;
	}

	/**
	 * 设置'{@code onblur}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnblur(String onblur) {
		this.onblur = onblur;
	}

	/**
	 * 获取'{@code onblur}'属性的值.
	 */
	protected String getOnblur() {
		return this.onblur;
	}

	/**
	 * 设置'{@code onchange}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnchange(String onchange) {
		this.onchange = onchange;
	}

	/**
	 * 获取'{@code onchange}'属性的值.
	 */
	protected String getOnchange() {
		return this.onchange;
	}

	/**
	 * 设置'{@code accesskey}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setAccesskey(String accesskey) {
		this.accesskey = accesskey;
	}

	/**
	 * 获取'{@code accesskey}'属性的值.
	 */
	protected String getAccesskey() {
		return this.accesskey;
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

	/**
	 * 设置'{@code readonly}'属性的值.
	 */
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	/**
	 * 获取'{@code readonly}'属性的值.
	 */
	protected boolean isReadonly() {
		return this.readonly;
	}


	/**
	 * 添加此基类定义的特定于输入的可选属性.
	 */
	@Override
	protected void writeOptionalAttributes(TagWriter tagWriter) throws JspException {
		super.writeOptionalAttributes(tagWriter);

		writeOptionalAttribute(tagWriter, ONFOCUS_ATTRIBUTE, getOnfocus());
		writeOptionalAttribute(tagWriter, ONBLUR_ATTRIBUTE, getOnblur());
		writeOptionalAttribute(tagWriter, ONCHANGE_ATTRIBUTE, getOnchange());
		writeOptionalAttribute(tagWriter, ACCESSKEY_ATTRIBUTE, getAccesskey());
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		if (isReadonly()) {
			writeOptionalAttribute(tagWriter, READONLY_ATTRIBUTE, "readonly");
		}
	}

}
