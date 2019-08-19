package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.beans.PropertyAccessor;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.tags.EditorAwareTag;
import org.springframework.web.servlet.tags.NestedPathTag;

/**
 * 所有支持数据绑定的JSP表单标记的基本标记.
 *
 * <p>提供常见的{@link #setPath path}和{@link #setId id}属性.
 * 提供带有工具方法的子类, 用于访问其绑定值的{@link BindStatus},
 * 以及与{@link TagWriter}{@link #writeOptionalAttribute 交互}.
 */
@SuppressWarnings("serial")
public abstract class AbstractDataBoundFormElementTag extends AbstractFormTag implements EditorAwareTag {

	/**
	 * 此标记范围内的公开的路径变量的名称: "nestedPath".
	 * 和{@link org.springframework.web.servlet.tags.NestedPathTag#NESTED_PATH_VARIABLE_NAME}的值一样.
	 */
	protected static final String NESTED_PATH_VARIABLE_NAME = NestedPathTag.NESTED_PATH_VARIABLE_NAME;


	/**
	 * {@link FormTag#setModelAttribute 表单对象}的属性路径.
	 */
	private String path;

	/**
	 * '{@code id}'属性的值.
	 */
	private String id;

	/**
	 * 此标记的{@link BindStatus}.
	 */
	private BindStatus bindStatus;


	/**
	 * 设置{@link FormTag#setModelAttribute 表单对象}的属性路径.
	 * 可能是运行时表达式.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 获取{@link FormTag#setModelAttribute 表单对象}的{@link #evaluate 解析后}的属性路径.
	 */
	protected final String getPath() throws JspException {
		String resolvedPath = (String) evaluate("path", this.path);
		return (resolvedPath != null ? resolvedPath : "");
	}

	/**
	 * 设置'{@code id}'属性的值.
	 * <p>可能是运行时表达式; 默认值为{@link #getName()}.
	 * 请注意, 默认值可能对某些标记无效.
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 获取'{@code id}'属性的值.
	 */
	@Override
	public String getId() {
		return this.id;
	}


	/**
	 * 将默认属性集写入提供的{@link TagWriter}.
	 * 进一步的抽象子类应覆盖此方法以添加任何其他默认属性, 但<strong>必须</strong>记得调用{@code super}方法.
	 * <p>具体的子类应该在呈现默认属性时调用此方法.
	 * 
	 * @param tagWriter 要写入任何属性的{@link TagWriter}
	 */
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		writeOptionalAttribute(tagWriter, "id", resolveId());
		writeOptionalAttribute(tagWriter, "name", getName());
	}

	/**
	 * 确定此标记的'{@code id}'属性值, 如果未指定, 则自动生成一个.
	 */
	protected String resolveId() throws JspException {
		Object id = evaluate("id", getId());
		if (id != null) {
			String idString = id.toString();
			return (StringUtils.hasText(idString) ? idString : null);
		}
		return autogenerateId();
	}

	/**
	 * 自动生成此标记的'{@code id}'属性值.
	 * <p>默认实现只是委托给{@link #getName()}, 删除无效字符 (例如 "[" 或 "]").
	 */
	protected String autogenerateId() throws JspException {
		return StringUtils.deleteAny(getName(), "[]");
	}

	/**
	 * 获取HTML '{@code name}'属性的值.
	 * <p>默认实现只是委托给 {@link #getPropertyPath()}以使用属性路径作为名称.
	 * 在大多数情况下, 这是可取的, 因为它与数据绑定的服务器端期望相关联.
	 * 但是, 某些子类可能希望在不更改绑定路径的情况下更改'{@code name}'属性的值.
	 * 
	 * @return HTML '{@code name}'属性的值
	 */
	protected String getName() throws JspException {
		return getPropertyPath();
	}

	/**
	 * 获取此标记的{@link BindStatus}.
	 */
	protected BindStatus getBindStatus() throws JspException {
		if (this.bindStatus == null) {
			// 标记中的HTML转义由ValueFormatter类执行.
			String nestedPath = getNestedPath();
			String pathToUse = (nestedPath != null ? nestedPath + getPath() : getPath());
			if (pathToUse.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
				pathToUse = pathToUse.substring(0, pathToUse.length() - 1);
			}
			this.bindStatus = new BindStatus(getRequestContext(), pathToUse, false);
		}
		return this.bindStatus;
	}

	/**
	 * 获取{@link NestedPathTag}可能已公开的嵌套路径的值.
	 */
	protected String getNestedPath() {
		return (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
	}

	/**
	 * 构建此标记的属性路径, 包括嵌套路径, 但<i>不添加</i>表单属性的名称为前缀.
	 */
	protected String getPropertyPath() throws JspException {
		String expression = getBindStatus().getExpression();
		return (expression != null ? expression : "");
	}

	/**
	 * 获取绑定的值.
	 */
	protected final Object getBoundValue() throws JspException {
		return getBindStatus().getValue();
	}

	/**
	 * 获取{@link PropertyEditor}, 用于绑定到此标记的值.
	 */
	protected PropertyEditor getPropertyEditor() throws JspException {
		return getBindStatus().getEditor();
	}

	/**
	 * 为{@link EditorAwareTag}公开{@link PropertyEditor}.
	 * <p>使用{@link #getPropertyEditor()}进行内部渲染.
	 */
	@Override
	public final PropertyEditor getEditor() throws JspException {
		return getPropertyEditor();
	}

	/**
	 * 获取给定值的显示字符串, 由PropertyEditor转换, 可能已为该值的类注册BindStatus.
	 */
	protected String convertToDisplayString(Object value) throws JspException {
		PropertyEditor editor = (value != null ? getBindStatus().findEditor(value.getClass()) : null);
		return getDisplayString(value, editor);
	}

	/**
	 * 如果已配置或以其他方式返回相同的值, 则通过{@link RequestDataValueProcessor}实例处理给定的表单字段.
	 */
	protected final String processFieldValue(String name, String value, String type) {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			value = processor.processFormFieldValue((HttpServletRequest) request, name, value, type);
		}
		return value;
	}

	/**
	 * 处置{@link BindStatus}实例.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.bindStatus = null;
	}

}
