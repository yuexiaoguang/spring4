package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 数据绑定感知JSP标记, 用于呈现包含数据绑定值的隐藏HTML '{@code input}'字段.
 *
 * <p>示例 (绑定到表单后备对象的'name'属性):
 * <pre class="code>
 * &lt;form:hidden path=&quot;name&quot;/&gt;
 * </pre>
 */
@SuppressWarnings("serial")
public class HiddenInputTag extends AbstractHtmlElementTag {

	/**
	 * '{@code disabled}'属性的名称.
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";

	private boolean disabled;


	/**
	 * 设置'{@code disabled}'属性的名称.
	 * 可能是运行时表达式.
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * 获取'{@code disabled}'属性的名称.
	 */
	public boolean isDisabled() {
		return this.disabled;
	}


	/**
	 * 标志"type"为非法动态属性.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * 将HTML '{@code input}'标记写入提供的{@link TagWriter}, 包括数据绑定值.
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute("type", "hidden");
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		tagWriter.writeAttribute("value", processFieldValue(getName(), value, "hidden"));
		tagWriter.endTag();
		return SKIP_BODY;
	}

}
