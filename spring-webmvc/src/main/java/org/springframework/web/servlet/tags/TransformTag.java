package org.springframework.web.servlet.tags;

import java.beans.PropertyEditor;
import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.web.util.TagUtils;

/**
 * 用于转换来自表单控制器和{@code spring:bind}标记内的其他对象 (或Spring的表单标记库中的data-bound表单元素标记)的引用数据值的标记.
 *
 * <p>BindTag有一个PropertyEditor, 用于将bean的属性转换为String, 可在HTML表单中使用.
 * 此标记使用PropertyEditor来转换传递给此标记的对象.
 */
@SuppressWarnings("serial")
public class TransformTag extends HtmlEscapingAwareTag {

	/** 要使用适当的属性编辑器进行转换的值 */
	private Object value;

	/** 将结果放入的变量 */
	private String var;

	/** 要放入结果的变量的范围 */
	private String scope = TagUtils.SCOPE_PAGE;


	/**
	 * 使用封闭BindTag中相应的PropertyEditor设置要转换的值.
	 * <p>该值可以是要转换的普通值 (JSP或JSP表达式中的硬编码字符串值), 也可以是要计算的JSP EL表达式 (转换表达式的结果).
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 设置PageContext属性名称, 在该名称下公开包含转换结果的变量.
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置将变量导出到的范围.
	 * 默认为 SCOPE_PAGE ("page").
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}


	@Override
	protected final int doStartTagInternal() throws JspException {
		if (this.value != null) {
			// 如果适用, 查找包含 EditorAwareTag (e.g. BindTag).
			EditorAwareTag tag = (EditorAwareTag) TagSupport.findAncestorWithClass(this, EditorAwareTag.class);
			if (tag == null) {
				throw new JspException("TransformTag can only be used within EditorAwareTag (e.g. BindTag)");
			}

			// OK, let's obtain the editor...
			String result = null;
			PropertyEditor editor = tag.getEditor();
			if (editor != null) {
				// If an editor was found, edit the value.
				editor.setValue(this.value);
				result = editor.getAsText();
			}
			else {
				// Else, just do a toString.
				result = this.value.toString();
			}
			result = htmlEscape(result);
			if (this.var != null) {
				pageContext.setAttribute(this.var, result, TagUtils.getScope(this.scope));
			}
			else {
				try {
					// Else, just print it out.
					pageContext.getOut().print(result);
				}
				catch (IOException ex) {
					throw new JspException(ex);
				}
			}
		}

		return SKIP_BODY;
	}
}
