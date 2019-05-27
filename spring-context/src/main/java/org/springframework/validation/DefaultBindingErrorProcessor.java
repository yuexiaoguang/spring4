package org.springframework.validation;

import org.springframework.beans.PropertyAccessException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 默认{@link BindingErrorProcessor}实现.
 *
 * <p>使用"required" 错误代码和字段名称来解析缺少字段错误的消息代码.
 *
 * <p>为每个给定的{@code PropertyAccessException}创建{@code FieldError},
 * 使用{@code PropertyAccessException}的错误代码 ("typeMismatch", "methodInvocation")来解析消息代码.
 */
public class DefaultBindingErrorProcessor implements BindingErrorProcessor {

	/**
	 * 注册缺少字段错误(i.e. 在属性值列表中找不到必填字段)使用的错误代码:
	 * "required".
	 */
	public static final String MISSING_FIELD_ERROR_CODE = "required";


	@Override
	public void processMissingFieldError(String missingField, BindingResult bindingResult) {
		// Create field error with code "required".
		String fixedField = bindingResult.getNestedPath() + missingField;
		String[] codes = bindingResult.resolveMessageCodes(MISSING_FIELD_ERROR_CODE, missingField);
		Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), fixedField);
		FieldError error = new FieldError(bindingResult.getObjectName(), fixedField, "", true,
				codes, arguments, "Field '" + fixedField + "' is required");
		bindingResult.addError(error);
	}

	@Override
	public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
		// Create field error with the exceptions's code, e.g. "typeMismatch".
		String field = ex.getPropertyName();
		String[] codes = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
		Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), field);
		Object rejectedValue = ex.getValue();
		if (ObjectUtils.isArray(rejectedValue)) {
			rejectedValue = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(rejectedValue));
		}
		FieldError error = new FieldError(bindingResult.getObjectName(), field, rejectedValue, true,
				codes, arguments, ex.getLocalizedMessage());
		bindingResult.addError(error);
	}

	/**
	 * 返回给定字段上的绑定错误的FieldError参数.
	 * 为每个缺少的必填字段和每个类型不匹配调用.
	 * <p>默认实现返回一个指示字段名称的参数
	 * (DefaultMessageSourceResolvable类型, 使用"objectName.field" 和 "field"作为代码).
	 * 
	 * @param objectName 目标对象的名称
	 * @param field 导致绑定错误的字段
	 * 
	 * @return 表示FieldError参数的Object数组
	 */
	protected Object[] getArgumentsForBindError(String objectName, String field) {
		String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new Object[] {new DefaultMessageSourceResolvable(codes, field)};
	}
}
