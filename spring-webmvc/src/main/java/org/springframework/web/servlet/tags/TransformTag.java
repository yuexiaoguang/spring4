package org.springframework.web.servlet.tags;

import java.beans.PropertyEditor;
import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.web.util.TagUtils;

/**
 * Tag for transforming reference data values from form controllers and
 * other objects inside a {@code spring:bind} tag (or a data-bound
 * form element tag from Spring's form tag library).
 *
 * <p>The BindTag has a PropertyEditor that it uses to transform properties of
 * a bean to a String, usable in HTML forms. This tag uses that PropertyEditor
 * to transform objects passed into this tag.
 */
@SuppressWarnings("serial")
public class TransformTag extends HtmlEscapingAwareTag {

	/** the value to transform using the appropriate property editor */
	private Object value;

	/** the variable to put the result in */
	private String var;

	/** the scope of the variable the result will be put in */
	private String scope = TagUtils.SCOPE_PAGE;


	/**
	 * Set the value to transform, using the appropriate PropertyEditor
	 * from the enclosing BindTag.
	 * <p>The value can either be a plain value to transform (a hard-coded String
	 * value in a JSP or a JSP expression), or a JSP EL expression to be evaluated
	 * (transforming the result of the expression).
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Set PageContext attribute name under which to expose
	 * a variable that contains the result of the transformation.
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * Set the scope to export the variable to.
	 * Default is SCOPE_PAGE ("page").
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}


	@Override
	protected final int doStartTagInternal() throws JspException {
		if (this.value != null) {
			// Find the containing EditorAwareTag (e.g. BindTag), if applicable.
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
