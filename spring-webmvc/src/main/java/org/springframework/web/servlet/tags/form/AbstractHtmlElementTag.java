package org.springframework.web.servlet.tags.form;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于呈现HTML元素的数据绑定感知JSP标记的基类.
 * 提供一组属性, 这些属性对应于元素间通用的HTML属性集.
 *
 * <p>此外, 此基类允许将非标准属性作为标记输出的一部分进行渲染.
 * 如果需要, 可以通过{@link AbstractHtmlElementTag#getDynamicAttributes() dynamicAttributes}映射访问这些属性.
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlElementTag extends AbstractDataBoundFormElementTag implements DynamicAttributes {

	public static final String CLASS_ATTRIBUTE = "class";

	public static final String STYLE_ATTRIBUTE = "style";

	public static final String LANG_ATTRIBUTE = "lang";

	public static final String TITLE_ATTRIBUTE = "title";

	public static final String DIR_ATTRIBUTE = "dir";

	public static final String TABINDEX_ATTRIBUTE = "tabindex";

	public static final String ONCLICK_ATTRIBUTE = "onclick";

	public static final String ONDBLCLICK_ATTRIBUTE = "ondblclick";

	public static final String ONMOUSEDOWN_ATTRIBUTE = "onmousedown";

	public static final String ONMOUSEUP_ATTRIBUTE = "onmouseup";

	public static final String ONMOUSEOVER_ATTRIBUTE = "onmouseover";

	public static final String ONMOUSEMOVE_ATTRIBUTE = "onmousemove";

	public static final String ONMOUSEOUT_ATTRIBUTE = "onmouseout";

	public static final String ONKEYPRESS_ATTRIBUTE = "onkeypress";

	public static final String ONKEYUP_ATTRIBUTE = "onkeyup";

	public static final String ONKEYDOWN_ATTRIBUTE = "onkeydown";


	private String cssClass;

	private String cssErrorClass;

	private String cssStyle;

	private String lang;

	private String title;

	private String dir;

	private String tabindex;

	private String onclick;

	private String ondblclick;

	private String onmousedown;

	private String onmouseup;

	private String onmouseover;

	private String onmousemove;

	private String onmouseout;

	private String onkeypress;

	private String onkeyup;

	private String onkeydown;

	private Map<String, Object> dynamicAttributes;


	/**
	 * 设置'{@code class}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	/**
	 * 获取'{@code class}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getCssClass() {
		return this.cssClass;
	}

	/**
	 * 绑定到特定标记的字段有错误时使用的CSS类.
	 * 可能是运行时表达式.
	 */
	public void setCssErrorClass(String cssErrorClass) {
		this.cssErrorClass = cssErrorClass;
	}

	/**
	 * 绑定到特定标记的字段有错误时使用的CSS类.
	 * 可能是运行时表达式.
	 */
	protected String getCssErrorClass() {
		return this.cssErrorClass;
	}

	/**
	 * 设置'{@code style}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setCssStyle(String cssStyle) {
		this.cssStyle = cssStyle;
	}

	/**
	 * 获取'{@code style}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getCssStyle() {
		return this.cssStyle;
	}

	/**
	 * 设置'{@code lang}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * 获取'{@code lang}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getLang() {
		return this.lang;
	}

	/**
	 * 设置'{@code title}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * 获取'{@code title}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getTitle() {
		return this.title;
	}

	/**
	 * 设置'{@code dir}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setDir(String dir) {
		this.dir = dir;
	}

	/**
	 * 获取'{@code dir}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getDir() {
		return this.dir;
	}

	/**
	 * 设置'{@code tabindex}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setTabindex(String tabindex) {
		this.tabindex = tabindex;
	}

	/**
	 * 获取'{@code tabindex}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getTabindex() {
		return this.tabindex;
	}

	/**
	 * 设置'{@code onclick}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnclick(String onclick) {
		this.onclick = onclick;
	}

	/**
	 * 获取'{@code onclick}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnclick() {
		return this.onclick;
	}

	/**
	 * 设置'{@code ondblclick}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOndblclick(String ondblclick) {
		this.ondblclick = ondblclick;
	}

	/**
	 * 获取'{@code ondblclick}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOndblclick() {
		return this.ondblclick;
	}

	/**
	 * 设置'{@code onmousedown}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnmousedown(String onmousedown) {
		this.onmousedown = onmousedown;
	}

	/**
	 * 获取'{@code onmousedown}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnmousedown() {
		return this.onmousedown;
	}

	/**
	 * 设置'{@code onmouseup}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnmouseup(String onmouseup) {
		this.onmouseup = onmouseup;
	}

	/**
	 * 获取'{@code onmouseup}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnmouseup() {
		return this.onmouseup;
	}

	/**
	 * 设置'{@code onmouseover}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnmouseover(String onmouseover) {
		this.onmouseover = onmouseover;
	}

	/**
	 * 获取'{@code onmouseover}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnmouseover() {
		return this.onmouseover;
	}

	/**
	 * 设置'{@code onmousemove}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnmousemove(String onmousemove) {
		this.onmousemove = onmousemove;
	}

	/**
	 * 获取'{@code onmousemove}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnmousemove() {
		return this.onmousemove;
	}

	/**
	 * 设置'{@code onmouseout}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnmouseout(String onmouseout) {
		this.onmouseout = onmouseout;
	}
	/**
	 * 获取'{@code onmouseout}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnmouseout() {
		return this.onmouseout;
	}

	/**
	 * 设置'{@code onkeypress}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnkeypress(String onkeypress) {
		this.onkeypress = onkeypress;
	}

	/**
	 * 获取'{@code onkeypress}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnkeypress() {
		return this.onkeypress;
	}

	/**
	 * 设置'{@code onkeyup}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnkeyup(String onkeyup) {
		this.onkeyup = onkeyup;
	}

	/**
	 * 获取'{@code onkeyup}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnkeyup() {
		return this.onkeyup;
	}

	/**
	 * 设置'{@code onkeydown}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setOnkeydown(String onkeydown) {
		this.onkeydown = onkeydown;
	}

	/**
	 * 获取'{@code onkeydown}'属性的值.
	 * 可能是运行时表达式.
	 */
	protected String getOnkeydown() {
		return this.onkeydown;
	}

	/**
	 * 获取动态属性.
	 */
	protected Map<String, Object> getDynamicAttributes() {
		return this.dynamicAttributes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDynamicAttribute(String uri, String localName, Object value ) throws JspException {
		if (this.dynamicAttributes == null) {
			this.dynamicAttributes = new HashMap<String, Object>();
		}
		if (!isValidDynamicAttribute(localName, value)) {
			throw new IllegalArgumentException(
					"Attribute " + localName + "=\"" + value + "\" is not allowed");
		}
		this.dynamicAttributes.put(localName, value);
	}

	/**
	 * 给定的 name-value对是否是有效的动态属性.
	 */
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return true;
	}

	/**
	 * 将通过此基类配置的默认属性写入提供的{@link TagWriter}.
	 * 子类应该在希望将基本属性写入输出时调用它.
	 */
	@Override
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		super.writeDefaultAttributes(tagWriter);
		writeOptionalAttributes(tagWriter);
	}

	/**
	 * 将通过此基类配置的可选属性写入提供的{@link TagWriter}.
	 * 将呈现的可选属性, 包括任何非标准动态属性.
	 * 由{@link #writeDefaultAttributes(TagWriter)}调用.
	 */
	protected void writeOptionalAttributes(TagWriter tagWriter) throws JspException {
		tagWriter.writeOptionalAttributeValue(CLASS_ATTRIBUTE, resolveCssClass());
		tagWriter.writeOptionalAttributeValue(STYLE_ATTRIBUTE,
				ObjectUtils.getDisplayString(evaluate("cssStyle", getCssStyle())));
		writeOptionalAttribute(tagWriter, LANG_ATTRIBUTE, getLang());
		writeOptionalAttribute(tagWriter, TITLE_ATTRIBUTE, getTitle());
		writeOptionalAttribute(tagWriter, DIR_ATTRIBUTE, getDir());
		writeOptionalAttribute(tagWriter, TABINDEX_ATTRIBUTE, getTabindex());
		writeOptionalAttribute(tagWriter, ONCLICK_ATTRIBUTE, getOnclick());
		writeOptionalAttribute(tagWriter, ONDBLCLICK_ATTRIBUTE, getOndblclick());
		writeOptionalAttribute(tagWriter, ONMOUSEDOWN_ATTRIBUTE, getOnmousedown());
		writeOptionalAttribute(tagWriter, ONMOUSEUP_ATTRIBUTE, getOnmouseup());
		writeOptionalAttribute(tagWriter, ONMOUSEOVER_ATTRIBUTE, getOnmouseover());
		writeOptionalAttribute(tagWriter, ONMOUSEMOVE_ATTRIBUTE, getOnmousemove());
		writeOptionalAttribute(tagWriter, ONMOUSEOUT_ATTRIBUTE, getOnmouseout());
		writeOptionalAttribute(tagWriter, ONKEYPRESS_ATTRIBUTE, getOnkeypress());
		writeOptionalAttribute(tagWriter, ONKEYUP_ATTRIBUTE, getOnkeyup());
		writeOptionalAttribute(tagWriter, ONKEYDOWN_ATTRIBUTE, getOnkeydown());

		if (!CollectionUtils.isEmpty(this.dynamicAttributes)) {
			for (String attr : this.dynamicAttributes.keySet()) {
				tagWriter.writeOptionalAttributeValue(attr, getDisplayString(this.dynamicAttributes.get(attr)));
			}
		}
	}

	/**
	 * 根据当前{@link org.springframework.web.servlet.support.BindStatus}对象的状态获取要使用的相应CSS类.
	 */
	protected String resolveCssClass() throws JspException {
		if (getBindStatus().isError() && StringUtils.hasText(getCssErrorClass())) {
			return ObjectUtils.getDisplayString(evaluate("cssErrorClass", getCssErrorClass()));
		}
		else {
			return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
		}
	}

}
