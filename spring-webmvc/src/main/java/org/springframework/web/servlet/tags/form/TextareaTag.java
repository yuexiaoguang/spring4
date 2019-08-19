package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 用于呈现HTML '{@code textarea}'的数据绑定感知JSP标记.
 */
@SuppressWarnings("serial")
public class TextareaTag extends AbstractHtmlInputElementTag {

	public static final String ROWS_ATTRIBUTE = "rows";

	public static final String COLS_ATTRIBUTE = "cols";

	public static final String ONSELECT_ATTRIBUTE = "onselect";

	@Deprecated
	public static final String READONLY_ATTRIBUTE = "readonly";


	private String rows;

	private String cols;

	private String onselect;


	/**
	 * 设置'{@code rows}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setRows(String rows) {
		this.rows = rows;
	}

	/**
	 * 获取'{@code rows}'属性的值.
	 */
	protected String getRows() {
		return this.rows;
	}

	/**
	 * 设置'{@code cols}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setCols(String cols) {
		this.cols = cols;
	}

	/**
	 * 获取'{@code cols}'属性的值.
	 */
	protected String getCols() {
		return this.cols;
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


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("textarea");
		writeDefaultAttributes(tagWriter);
		writeOptionalAttribute(tagWriter, ROWS_ATTRIBUTE, getRows());
		writeOptionalAttribute(tagWriter, COLS_ATTRIBUTE, getCols());
		writeOptionalAttribute(tagWriter, ONSELECT_ATTRIBUTE, getOnselect());
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		tagWriter.appendValue("\r\n" + processFieldValue(getName(), value, "textarea"));
		tagWriter.endTag();
		return SKIP_BODY;
	}

}
