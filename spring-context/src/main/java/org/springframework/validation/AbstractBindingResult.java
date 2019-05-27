package org.springframework.validation;

import java.beans.PropertyEditor;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BindingResult}接口及其超级接口{@link Errors}的抽象实现.
 * 封装{@link ObjectError ObjectErrors}和{@link FieldError FieldErrors}的常见管理.
 */
@SuppressWarnings("serial")
public abstract class AbstractBindingResult extends AbstractErrors implements BindingResult, Serializable {

	private final String objectName;

	private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

	private final List<ObjectError> errors = new LinkedList<ObjectError>();

	private final Set<String> suppressedFields = new HashSet<String>();


	/**
	 * @param objectName 目标对象的名称
	 */
	protected AbstractBindingResult(String objectName) {
		this.objectName = objectName;
	}

	/**
	 * 设置用于将错误解析为消息代码的策略.
	 * 默认是DefaultMessageCodesResolver.
	 */
	public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		Assert.notNull(messageCodesResolver, "MessageCodesResolver must not be null");
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * 返回用于将错误解析为消息代码的策略.
	 */
	public MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}


	//---------------------------------------------------------------------
	// Implementation of the Errors interface
	//---------------------------------------------------------------------

	@Override
	public String getObjectName() {
		return this.objectName;
	}


	@Override
	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		addError(new ObjectError(getObjectName(), resolveMessageCodes(errorCode), errorArgs, defaultMessage));
	}

	@Override
	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		if ("".equals(getNestedPath()) && !StringUtils.hasLength(field)) {
			// 位于嵌套对象层次结构的顶部, 因此当前级别不是字段而是顶级对象.
			// 能做的最好的就是在这里注册一个全局错误...
			reject(errorCode, errorArgs, defaultMessage);
			return;
		}
		String fixedField = fixedField(field);
		Object newVal = getActualFieldValue(fixedField);
		FieldError fe = new FieldError(
				getObjectName(), fixedField, newVal, false,
				resolveMessageCodes(errorCode, field), errorArgs, defaultMessage);
		addError(fe);
	}

	@Override
	public void addError(ObjectError error) {
		this.errors.add(error);
	}

	@Override
	public void addAllErrors(Errors errors) {
		if (!errors.getObjectName().equals(getObjectName())) {
			throw new IllegalArgumentException("Errors object needs to have same object name");
		}
		this.errors.addAll(errors.getAllErrors());
	}

	@Override
	public String[] resolveMessageCodes(String errorCode) {
		return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
	}

	@Override
	public String[] resolveMessageCodes(String errorCode, String field) {
		Class<?> fieldType = getFieldType(field);
		return getMessageCodesResolver().resolveMessageCodes(
				errorCode, getObjectName(), fixedField(field), fieldType);
	}


	@Override
	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	@Override
	public int getErrorCount() {
		return this.errors.size();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return Collections.unmodifiableList(this.errors);
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		List<ObjectError> result = new LinkedList<ObjectError>();
		for (ObjectError objectError : this.errors) {
			if (!(objectError instanceof FieldError)) {
				result.add(objectError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public ObjectError getGlobalError() {
		for (ObjectError objectError : this.errors) {
			if (!(objectError instanceof FieldError)) {
				return objectError;
			}
		}
		return null;
	}

	@Override
	public List<FieldError> getFieldErrors() {
		List<FieldError> result = new LinkedList<FieldError>();
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError) {
				result.add((FieldError) objectError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public FieldError getFieldError() {
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError) {
				return (FieldError) objectError;
			}
		}
		return null;
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		List<FieldError> result = new LinkedList<FieldError>();
		String fixedField = fixedField(field);
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError && isMatchingFieldError(fixedField, (FieldError) objectError)) {
				result.add((FieldError) objectError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public FieldError getFieldError(String field) {
		String fixedField = fixedField(field);
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError) {
				FieldError fieldError = (FieldError) objectError;
				if (isMatchingFieldError(fixedField, fieldError)) {
					return fieldError;
				}
			}
		}
		return null;
	}

	@Override
	public Object getFieldValue(String field) {
		FieldError fieldError = getFieldError(field);
		// 如果出现错误, 则使用被拒绝的值, 当前bean属性值为else.
		Object value = (fieldError != null ? fieldError.getRejectedValue() :
				getActualFieldValue(fixedField(field)));
		// Apply formatting, but not on binding failures like type mismatches.
		if (fieldError == null || !fieldError.isBindingFailure()) {
			value = formatFieldValue(field, value);
		}
		return value;
	}

	/**
	 * 此默认实现根据实际字段值确定类型.
	 * 子类应重写此方法以确定描述符中的类型, 即使对于{@code null}值也是如此.
	 */
	@Override
	public Class<?> getFieldType(String field) {
		Object value = getActualFieldValue(fixedField(field));
		if (value != null) {
			return value.getClass();
		}
		return null;
	}


	//---------------------------------------------------------------------
	// Implementation of BindingResult interface
	//---------------------------------------------------------------------

	/**
	 * 返回获取状态的模型Map, 将Errors实例公开为'{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName'和对象本身.
	 * <p>请注意, 每次调用此方法时都会构建Map.
	 * 将内容添加到Map然后重新调用此方法将不起作用.
	 * <p>此方法返回的模型Map中的属性通常包含在ModelAndView中, 用于使用Spring的绑定标记的表单视图, 该标记需要访问Errors实例.
	 */
	@Override
	public Map<String, Object> getModel() {
		Map<String, Object> model = new LinkedHashMap<String, Object>(2);
		// Mapping from name to target object.
		model.put(getObjectName(), getTarget());
		// Errors instance, even if no errors.
		model.put(MODEL_KEY_PREFIX + getObjectName(), this);
		return model;
	}

	@Override
	public Object getRawFieldValue(String field) {
		return getActualFieldValue(fixedField(field));
	}

	/**
	 * 此实现委托给{@link #getPropertyEditorRegistry() PropertyEditorRegistry}的编辑器查找工具.
	 */
	@Override
	public PropertyEditor findEditor(String field, Class<?> valueType) {
		PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();
		if (editorRegistry != null) {
			Class<?> valueTypeToUse = valueType;
			if (valueTypeToUse == null) {
				valueTypeToUse = getFieldType(field);
			}
			return editorRegistry.findCustomEditor(valueTypeToUse, fixedField(field));
		}
		else {
			return null;
		}
	}

	/**
	 * 此实现返回{@code null}.
	 */
	@Override
	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return null;
	}

	/**
	 * 将指定的不允许字段标记为已抑制.
	 * <p>数据绑定器会为检测到的每个字段值调用此方法以定位不允许的字段.
	 */
	@Override
	public void recordSuppressedField(String field) {
		this.suppressedFields.add(field);
	}

	/**
	 * 返回绑定过程中被抑制的字段列表.
	 * <p>可用于确定是否有任何字段值针对不允许的字段.
	 */
	@Override
	public String[] getSuppressedFields() {
		return StringUtils.toStringArray(this.suppressedFields);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BindingResult)) {
			return false;
		}
		BindingResult otherResult = (BindingResult) other;
		return (getObjectName().equals(otherResult.getObjectName()) &&
				ObjectUtils.nullSafeEquals(getTarget(), otherResult.getTarget()) &&
				getAllErrors().equals(otherResult.getAllErrors()));
	}

	@Override
	public int hashCode() {
		return getObjectName().hashCode();
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented/overridden by subclasses
	//---------------------------------------------------------------------

	/**
	 * 返回包装的目标对象.
	 */
	@Override
	public abstract Object getTarget();

	/**
	 * 提取给定字段的实际字段值.
	 * 
	 * @param field 要检查的字段
	 * 
	 * @return 该字段的当前值
	 */
	protected abstract Object getActualFieldValue(String field);

	/**
	 * 格式化指定字段的给定值.
	 * <p>默认实现只是按原样返回字段值.
	 * 
	 * @param field 要检查的字段
	 * @param value 字段的值 (来自绑定错误的拒绝值, 或实际字段值)
	 * 
	 * @return 格式化后的值
	 */
	protected Object formatFieldValue(String field, Object value) {
		return value;
	}

}
