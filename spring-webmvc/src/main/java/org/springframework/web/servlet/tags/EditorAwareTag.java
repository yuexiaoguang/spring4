package org.springframework.web.servlet.tags;

import java.beans.PropertyEditor;
import javax.servlet.jsp.JspException;

/**
 * 由JSP标记实现的接口, 它为它们当前绑定的属性公开PropertyEditor.
 */
public interface EditorAwareTag {

	/**
	 * 检索此标记当前绑定到的属性的PropertyEditor. 用于协作嵌套标签.
	 * 
	 * @return 当前的PropertyEditor, 或{@code null}
	 * @throws JspException 如果解析编辑器失败
	 */
	PropertyEditor getEditor() throws JspException;

}
