package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.util.Assert;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

/**
 * 用于呈现HTML '{@code option}'标记的JSP标记.
 *
 * <p><b>必须嵌套在{@link SelectTag}中.</b>
 *
 * <p>如果{@link #setValue value}匹配绑定到输出{@link SelectTag}的值,
 * 则通过将'{@code option}' 标记为 'selected'来提供对数据绑定的完全支持.
 *
 * <p>{@link #setValue value}属性是必需的, 对应于呈现的'{@code option}'的'{@code value}'属性.
 *
 * <p>可以指定可选的{@link #setLabel label}属性, 其值对应于呈现的'{@code option}'标记的内部文本.
 * 如果未指定{@link #setLabel label}, 则在呈现内部文本时将使用{@link #setValue value}属性.
 */
@SuppressWarnings("serial")
public class OptionTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * 用于公开此标记值的JSP变量的名称.
	 */
	public static final String VALUE_VARIABLE_NAME = "value";

	/**
	 * 用于公开此标记的显示值的JSP变量的名称.
	 */
	public static final String DISPLAY_VALUE_VARIABLE_NAME = "displayValue";

	/**
	 * '{@code selected}'属性的名称.
	 */
	private static final String SELECTED_ATTRIBUTE = "selected";

	/**
	 * '{@code value}'属性的名称.
	 */
	private static final String VALUE_ATTRIBUTE = VALUE_VARIABLE_NAME;

	/**
	 * '{@code disabled}'属性的名称.
	 */
	private static final String DISABLED_ATTRIBUTE = "disabled";


	/**
	 * 呈现的HTML {@code <option>}标记的'value'属性.
	 */
	private Object value;

	/**
	 * 呈现的HTML {@code <option>}标记的文本正文.
	 */
	private String label;

	private Object oldValue;

	private Object oldDisplayValue;

	private boolean disabled;


	/**
	 * 设置呈现的HTML {@code <option>}标记的'value'属性.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 获取呈现的HTML {@code <option>}标记的'value'属性.
	 */
	protected Object getValue() {
		return this.value;
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
	 * 设置呈现的HTML {@code <option>}标记的文本正文.
	 * <p>可能是运行时表达式.
	 */
	public void setLabel(String label) {
		Assert.notNull(label, "'label' must not be null");
		this.label = label;
	}

	/**
	 * 获取呈现的HTML {@code <option>}标记的文本正文.
	 */
	protected String getLabel() {
		return this.label;
	}


	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		String label = getLabelValue(value);
		renderOption(value, label, tagWriter);
	}

	@Override
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		String label = bodyContent.getString();
		renderOption(value, label, tagWriter);
	}

	/**
	 * 在继续之前, 确保处于'{@code select}'标记下.
	 */
	@Override
	protected void onWriteTagContent() {
		assertUnderSelectTag();
	}

	@Override
	protected void exposeAttributes() throws JspException {
		Object value = resolveValue();
		this.oldValue = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(VALUE_VARIABLE_NAME, value);
		this.oldDisplayValue = this.pageContext.getAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, getDisplayString(value, getBindStatus().getEditor()));
	}

	@Override
	protected BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}

	@Override
	protected void removeAttributes() {
		if (this.oldValue != null) {
			this.pageContext.setAttribute(VALUE_ATTRIBUTE, this.oldValue);
			this.oldValue = null;
		}
		else {
			this.pageContext.removeAttribute(VALUE_VARIABLE_NAME);
		}

		if (this.oldDisplayValue != null) {
			this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, this.oldDisplayValue);
			this.oldDisplayValue = null;
		}
		else {
			this.pageContext.removeAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		}
	}

	private void renderOption(Object value, String label, TagWriter tagWriter) throws JspException {
		tagWriter.startTag("option");
		writeOptionalAttribute(tagWriter, "id", resolveId());
		writeOptionalAttributes(tagWriter);
		String renderedValue = getDisplayString(value, getBindStatus().getEditor());
		renderedValue = processFieldValue(getSelectTag().getName(), renderedValue, "option");
		tagWriter.writeAttribute(VALUE_ATTRIBUTE, renderedValue);
		if (isSelected(value)) {
			tagWriter.writeAttribute(SELECTED_ATTRIBUTE, SELECTED_ATTRIBUTE);
		}
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		tagWriter.appendValue(label);
		tagWriter.endTag();
	}

	@Override
	protected String autogenerateId() throws JspException {
		return null;
	}

	/**
	 * 返回此'{@code option}'元素的标签值.
	 * <p>如果设置了{@link #setLabel label}属性, 则使用该属性的已解析值, 否则使用{@code resolvedValue}参数的值.
	 */
	private String getLabelValue(Object resolvedValue) throws JspException {
		String label = getLabel();
		Object labelObj = (label == null ? resolvedValue : evaluate("label", label));
		return getDisplayString(labelObj, getBindStatus().getEditor());
	}

	private void assertUnderSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "option", "select");
	}

	private SelectTag getSelectTag() {
		return (SelectTag) findAncestorWithClass(this, SelectTag.class);
	}

	private boolean isSelected(Object resolvedValue) {
		return SelectedValueComparator.isSelected(getBindStatus(), resolvedValue);
	}

	private Object resolveValue() throws JspException {
		return evaluate(VALUE_VARIABLE_NAME, getValue());
	}

}
