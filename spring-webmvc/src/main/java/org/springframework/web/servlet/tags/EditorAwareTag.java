package org.springframework.web.servlet.tags;

import java.beans.PropertyEditor;
import javax.servlet.jsp.JspException;

/**
 * Interface to be implemented by JSP tags that expose a
 * PropertyEditor for a property that they are currently bound to.
 */
public interface EditorAwareTag {

	/**
	 * Retrieve the PropertyEditor for the property that this tag is
	 * currently bound to. Intended for cooperating nesting tags.
	 * @return the current PropertyEditor, or {@code null} if none
	 * @throws JspException if resolving the editor failed
	 */
	PropertyEditor getEditor() throws JspException;

}
