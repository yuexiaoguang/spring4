package org.springframework.web.servlet.support;

import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.util.HtmlUtils;

/**
 * 用于公开字段或对象的绑定状态的简单适配器.
 * 由JSP绑定标记和 Velocity/FreeMarker宏设置为变量.
 *
 * <p>显然，对象状态表示 (i.e. 对象级别的错误, 而不是字段级别) 没有表达式和值, 只有错误码和消息.
 * 为简单起见并且能够使用相同的标签和宏, 两种方案都使用相同的状态类.
 */
public class BindStatus {

	private final RequestContext requestContext;

	private final String path;

	private final boolean htmlEscape;

	private final String expression;

	private final Errors errors;

	private BindingResult bindingResult;

	private Object value;

	private Class<?> valueType;

	private Object actualValue;

	private PropertyEditor editor;

	private List<? extends ObjectError> objectErrors;

	private String[] errorCodes;

	private String[] errorMessages;


	/**
	 * @param requestContext 当前的RequestContext
	 * @param path 将为其解析值和错误的bean和属性路径 (e.g. "customer.address.street")
	 * @param htmlEscape 是否HTML转义错误消息和字符串值
	 * 
	 * @throws IllegalStateException 如果没有找到相应的Errors对象
	 */
	public BindStatus(RequestContext requestContext, String path, boolean htmlEscape) throws IllegalStateException {
		this.requestContext = requestContext;
		this.path = path;
		this.htmlEscape = htmlEscape;

		// 确定对象和属性的名称
		String beanName;
		int dotPos = path.indexOf('.');
		if (dotPos == -1) {
			// 属性未设置, 只有对象本身
			beanName = path;
			this.expression = null;
		}
		else {
			beanName = path.substring(0, dotPos);
			this.expression = path.substring(dotPos + 1);
		}

		this.errors = requestContext.getErrors(beanName, false);

		if (this.errors != null) {
			// 通常情况: BindingResult可用作请求属性.
			// 可以确定给定表达式的错误代码和消息.
			// 可以使用由表单控制器注册的自定义PropertyEditor.
			if (this.expression != null) {
				if ("*".equals(this.expression)) {
					this.objectErrors = this.errors.getAllErrors();
				}
				else if (this.expression.endsWith("*")) {
					this.objectErrors = this.errors.getFieldErrors(this.expression);
				}
				else {
					this.objectErrors = this.errors.getFieldErrors(this.expression);
					this.value = this.errors.getFieldValue(this.expression);
					this.valueType = this.errors.getFieldType(this.expression);
					if (this.errors instanceof BindingResult) {
						this.bindingResult = (BindingResult) this.errors;
						this.actualValue = this.bindingResult.getRawFieldValue(this.expression);
						this.editor = this.bindingResult.findEditor(this.expression, null);
					}
					else {
						this.actualValue = this.value;
					}
				}
			}
			else {
				this.objectErrors = this.errors.getGlobalErrors();
			}
			initErrorCodes();
		}

		else {
			// 没有BindingResult可用作请求属性: 可能直接转发到表单视图.
			// 尽力而为: 如果合适, 提取一个普通的目标.
			Object target = requestContext.getModelObject(beanName);
			if (target == null) {
				throw new IllegalStateException("Neither BindingResult nor plain target object for bean name '" +
						beanName + "' available as request attribute");
			}
			if (this.expression != null && !"*".equals(this.expression) && !this.expression.endsWith("*")) {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(target);
				this.value = bw.getPropertyValue(this.expression);
				this.valueType = bw.getPropertyType(this.expression);
				this.actualValue = this.value;
			}
			this.errorCodes = new String[0];
			this.errorMessages = new String[0];
		}

		if (htmlEscape && this.value instanceof String) {
			this.value = HtmlUtils.htmlEscape((String) this.value);
		}
	}

	/**
	 * 从ObjectError列表中提取错误代码.
	 */
	private void initErrorCodes() {
		this.errorCodes = new String[this.objectErrors.size()];
		for (int i = 0; i < this.objectErrors.size(); i++) {
			ObjectError error = this.objectErrors.get(i);
			this.errorCodes[i] = error.getCode();
		}
	}

	/**
	 * 从ObjectError列表中提取错误消息.
	 */
	private void initErrorMessages() throws NoSuchMessageException {
		if (this.errorMessages == null) {
			this.errorMessages = new String[this.objectErrors.size()];
			for (int i = 0; i < this.objectErrors.size(); i++) {
				ObjectError error = this.objectErrors.get(i);
				this.errorMessages[i] = this.requestContext.getMessage(error, this.htmlEscape);
			}
		}
	}


	/**
	 * 返回将解析其值和错误的bean和属性路径 (e.g. "customer.address.street").
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * 返回可以在HTML表单中用作相应字段的输入名称的绑定表达式, 如果不是特定于字段, 则返回{@code null}.
	 * <p>返回适合重新提交的绑定路径, e.g. "address.street".
	 * 请注意, 绑定标记所需的完整绑定路径是"customer.address.street", 如果绑定到"customer" bean.
	 */
	public String getExpression() {
		return this.expression;
	}

	/**
	 * 返回字段的当前值, i.e. 属性值或拒绝更新, 如果不是字段特定, 则返回{@code null}.
	 * <p>如果原始值已经是String, 则此值将是HTML转义的String.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 获取该字段的 '{@code Class}'类型.
	 * 使用这个而不是'{@code getValue().getClass()}', 因为'{@code getValue()}'可能会返回'{@code null}'.
	 */
	public Class<?> getValueType() {
		return this.valueType;
	}

	/**
	 * 返回字段的实际值, i.e. 原始属性值, 或{@code null}.
	 */
	public Object getActualValue() {
		return this.actualValue;
	}

	/**
	 * 返回字段的合适显示值, i.e. 字符串值, 或空字符串.
	 * <p>如果原始值为非null, 则此值将为HTML转义字符串:
	 * 原始值的{@code toString}结果将HTML转义.
	 */
	public String getDisplayValue() {
		if (this.value instanceof String) {
			return (String) this.value;
		}
		if (this.value != null) {
			return (this.htmlEscape ? HtmlUtils.htmlEscape(this.value.toString()) : this.value.toString());
		}
		return "";
	}

	/**
	 * 如果此状态表示字段或对象错误.
	 */
	public boolean isError() {
		return (this.errorCodes != null && this.errorCodes.length > 0);
	}

	/**
	 * 返回字段或对象的错误代码.
	 * 如果没有, 则返回空数组而不是null.
	 */
	public String[] getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * 返回字段或对象的第一个错误代码.
	 */
	public String getErrorCode() {
		return (this.errorCodes.length > 0 ? this.errorCodes[0] : "");
	}

	/**
	 * 返回字段或对象的已解析的错误消息.
	 * 如果没有, 则返回空数组而不是null.
	 */
	public String[] getErrorMessages() {
		initErrorMessages();
		return this.errorMessages;
	}

	/**
	 * 返回字段或对象的第一条错误消息.
	 */
	public String getErrorMessage() {
		initErrorMessages();
		return (this.errorMessages.length > 0 ? this.errorMessages[0] : "");
	}

	/**
	 * 返回错误消息字符串, 连接由给定分隔符分隔的所有消息.
	 * 
	 * @param delimiter 分隔符, e.g. ", " 或 "<br>"
	 * 
	 * @return 错误消息字符串
	 */
	public String getErrorMessagesAsString(String delimiter) {
		initErrorMessages();
		return StringUtils.arrayToDelimitedString(this.errorMessages, delimiter);
	}

	/**
	 * 返回此绑定状态当前与之关联的Errors实例(通常为BindingResult).
	 * 
	 * @return 当前的Errors实例, 或{@code null}
	 */
	public Errors getErrors() {
		return this.errors;
	}

	/**
	 * 返回PropertyEditor以获取此绑定状态当前绑定的属性.
	 * 
	 * @return 当前的PropertyEditor, 或{@code null}
	 */
	public PropertyEditor getEditor() {
		return this.editor;
	}

	/**
	 * 查找给定值类的PropertyEditor, 它与此绑定状态当前绑定的属性相关联.
	 * 
	 * @param valueClass 需要编辑器的值类
	 * 
	 * @return 关联的PropertyEditor, 或{@code null}
	 */
	public PropertyEditor findEditor(Class<?> valueClass) {
		return (this.bindingResult != null ? this.bindingResult.findEditor(this.expression, valueClass) : null);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("BindStatus: ");
		sb.append("expression=[").append(this.expression).append("]; ");
		sb.append("value=[").append(this.value).append("]");
		if (isError()) {
			sb.append("; errorCodes=").append(Arrays.asList(this.errorCodes));
		}
		return sb.toString();
	}
}
