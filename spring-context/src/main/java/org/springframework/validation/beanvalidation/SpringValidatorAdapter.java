package org.springframework.validation.beanvalidation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;

/**
 * 接受JSR-303 {@code javax.validator.Validator}并将其公开为Spring {@link org.springframework.validation.Validator},
 * 同时还公开原始JSR-303 Validator接口本身的适配器.
 *
 * <p>可以用作程序化包装器. 也用作{@link CustomValidatorBean}和{@link LocalValidatorFactoryBean}的基类.
 *
 * <p>请注意, 此适配器不支持Bean Validation 1.1的{@code #forExecutables}方法:
 * 不希望应用程序代码调用该方法; 考虑{@link MethodValidationInterceptor}.
 * 如果确实需要使用程序访问{@code #forExecutables}, 调用{@code #unwrap(Validator.class)},
 * 它将提供具有{@code #forExecutables}支持的本地{@link Validator}对象.
 */
public class SpringValidatorAdapter implements SmartValidator, javax.validation.Validator {

	private static final Set<String> internalAnnotationAttributes = new HashSet<String>(3);

	static {
		internalAnnotationAttributes.add("message");
		internalAnnotationAttributes.add("groups");
		internalAnnotationAttributes.add("payload");
	}

	private javax.validation.Validator targetValidator;


	/**
	 * @param targetValidator 要包装的JSR-303验证器
	 */
	public SpringValidatorAdapter(javax.validation.Validator targetValidator) {
		Assert.notNull(targetValidator, "Target Validator must not be null");
		this.targetValidator = targetValidator;
	}

	SpringValidatorAdapter() {
	}

	void setTargetValidator(javax.validation.Validator targetValidator) {
		this.targetValidator = targetValidator;
	}


	//---------------------------------------------------------------------
	// Implementation of Spring Validator interface
	//---------------------------------------------------------------------

	@Override
	public boolean supports(Class<?> clazz) {
		return (this.targetValidator != null);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (this.targetValidator != null) {
			processConstraintViolations(this.targetValidator.validate(target), errors);
		}
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		if (this.targetValidator != null) {
			Set<Class<?>> groups = new LinkedHashSet<Class<?>>();
			if (validationHints != null) {
				for (Object hint : validationHints) {
					if (hint instanceof Class) {
						groups.add((Class<?>) hint);
					}
				}
			}
			processConstraintViolations(
					this.targetValidator.validate(target, ClassUtils.toClassArray(groups)), errors);
		}
	}

	/**
	 * 处理给定的JSR-303 ConstraintViolations, 向提供的Spring {@link Errors}对象添加相应的错误.
	 * 
	 * @param violations JSR-303 ConstraintViolation结果
	 * @param errors 要注册的Spring errors对象
	 */
	protected void processConstraintViolations(Set<ConstraintViolation<Object>> violations, Errors errors) {
		for (ConstraintViolation<Object> violation : violations) {
			String field = determineField(violation);
			FieldError fieldError = errors.getFieldError(field);
			if (fieldError == null || !fieldError.isBindingFailure()) {
				try {
					ConstraintDescriptor<?> cd = violation.getConstraintDescriptor();
					String errorCode = determineErrorCode(cd);
					Object[] errorArgs = getArgumentsForConstraint(errors.getObjectName(), field, cd);
					if (errors instanceof BindingResult) {
						// 可以使用ConstraintViolation中的无效值进行自定义FieldError注册, 这是Hibernate Validator兼容性所必需的 (字段中的非索引设置路径)
						BindingResult bindingResult = (BindingResult) errors;
						String nestedField = bindingResult.getNestedPath() + field;
						if ("".equals(nestedField)) {
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode);
							bindingResult.addError(new ObjectError(
									errors.getObjectName(), errorCodes, errorArgs, violation.getMessage()));
						}
						else {
							Object rejectedValue = getRejectedValue(field, violation, bindingResult);
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode, field);
							bindingResult.addError(new FieldError(
									errors.getObjectName(), nestedField, rejectedValue, false,
									errorCodes, errorArgs, violation.getMessage()));
						}
					}
					else {
						// 没有BindingResult - 只能通过自动提取当前字段值来执行标准rejectValue调用
						errors.rejectValue(field, errorCode, errorArgs, violation.getMessage());
					}
				}
				catch (NotReadablePropertyException ex) {
					throw new IllegalStateException("JSR-303 validated property '" + field +
							"' does not have a corresponding accessor for Spring data binding - " +
							"check your DataBinder's configuration (bean property versus direct field access)", ex);
				}
			}
		}
	}

	/**
	 * 确定给定约束违规的字段.
	 * <p>默认实现返回字符串化的属性路径.
	 * 
	 * @param violation 当前JSR-303 ConstraintViolation
	 * 
	 * @return Spring报告的字段 (用于{@link Errors})
	 */
	protected String determineField(ConstraintViolation<Object> violation) {
		String path = violation.getPropertyPath().toString();
		int elementIndex = path.indexOf(".<");
		return (elementIndex >= 0 ? path.substring(0, elementIndex) : path);
	}

	/**
	 * 确定给定约束描述符的Spring报告错误代码.
	 * <p>默认实现返回描述符的注解类型的简单类名.
	 * 请注意, 配置的{@link org.springframework.validation.MessageCodesResolver}将自动生成包含对象名称和字段名称的错误代码变体.
	 * 
	 * @param descriptor 当前违规的JSR-303 ConstraintDescriptor
	 * 
	 * @return 相应的错误代码 (用于{@link Errors})
	 */
	protected String determineErrorCode(ConstraintDescriptor<?> descriptor) {
		return descriptor.getAnnotation().annotationType().getSimpleName();
	}

	/**
	 * 返回给定字段上的验证错误的FieldError参数.
	 * 为每个违反的约束调用.
	 * <p>默认实现返回指示字段名称的第一个参数 (see {@link #getResolvableField}).
	 * 然后, 它按属性名称的字母顺序添加所有实际约束注解属性 (i.e. 不包括"message", "groups" 和 "payload").
	 * <p>可以重写 e.g. 从约束描述符中添加更多属性.
	 * 
	 * @param objectName 目标对象的名称
	 * @param field 导致绑定错误的字段
	 * @param descriptor JSR-303约束描述符
	 * 
	 * @return 表示FieldError参数的Object数组
	 */
	protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintDescriptor<?> descriptor) {
		List<Object> arguments = new ArrayList<Object>();
		arguments.add(getResolvableField(objectName, field));
		// 使用TreeMap对属性名称进行字母顺序排序
		Map<String, Object> attributesToExpose = new TreeMap<String, Object>();
		for (Map.Entry<String, Object> entry : descriptor.getAttributes().entrySet()) {
			String attributeName = entry.getKey();
			Object attributeValue = entry.getValue();
			if (!internalAnnotationAttributes.contains(attributeName)) {
				if (attributeValue instanceof String) {
					attributeValue = new ResolvableAttribute(attributeValue.toString());
				}
				attributesToExpose.put(attributeName, attributeValue);
			}
		}
		arguments.addAll(attributesToExpose.values());
		return arguments.toArray();
	}

	/**
	 * 为指定字段构建可解析的包装器, 允许在{@code MessageSource}中解析字段的名称.
	 * <p>默认实现返回指示该字段的第一个参数:
	 * {@code DefaultMessageSourceResolvable}类型, 将"objectName.field" 和 "field"作为代码, 将普通字段名称作为默认消息.
	 * 
	 * @param objectName 目标对象的名称
	 * @param field 导致绑定错误的字段
	 * 
	 * @return 指定字段的相应{@code MessageSourceResolvable}
	 */
	protected MessageSourceResolvable getResolvableField(String objectName, String field) {
		String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new DefaultMessageSourceResolvable(codes, field);
	}

	/**
	 * 通过Spring错误表示提取给定约束违规后面的被拒绝值.
	 * 
	 * @param field 导致绑定错误的字段
	 * @param violation 相应的JSR-303 ConstraintViolation
	 * @param bindingResult 包含当前字段值的后备对象的Spring BindingResult
	 * 
	 * @return 作为字段错误的一部分公开的无效值
	 */
	protected Object getRejectedValue(String field, ConstraintViolation<Object> violation, BindingResult bindingResult) {
		Object invalidValue = violation.getInvalidValue();
		if (!"".equals(field) && !field.contains("[]") &&
				(invalidValue == violation.getLeafBean() || field.contains("[") || field.contains("."))) {
			// 可能是具有属性路径的bean约束: 检索实际的属性值.
			// 但是, 明确地避免使用无法处理的 "address[]"样式路径.
			invalidValue = bindingResult.getRawFieldValue(field);
		}
		return invalidValue;
	}


	//---------------------------------------------------------------------
	// Implementation of JSR-303 Validator interface
	//---------------------------------------------------------------------

	@Override
	public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validate(object, groups);
	}

	@Override
	public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validateProperty(object, propertyName, groups);
	}

	@Override
	public <T> Set<ConstraintViolation<T>> validateValue(
			Class<T> beanType, String propertyName, Object value, Class<?>... groups) {

		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validateValue(beanType, propertyName, value, groups);
	}

	@Override
	public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.getConstraintsForClass(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		try {
			return (type != null ? this.targetValidator.unwrap(type) : (T) this.targetValidator);
		}
		catch (ValidationException ex) {
			// ignore if just being asked for plain Validator
			if (javax.validation.Validator.class == type) {
				return (T) this.targetValidator;
			}
			throw ex;
		}
	}


	/**
	 * String属性的包装器, 可以通过{@code MessageSource}解析, 作为默认值回退到的原始属性.
	 */
	@SuppressWarnings("serial")
	private static class ResolvableAttribute implements MessageSourceResolvable, Serializable {

		private final String resolvableString;

		public ResolvableAttribute(String resolvableString) {
			this.resolvableString = resolvableString;
		}

		@Override
		public String[] getCodes() {
			return new String[] {this.resolvableString};
		}

		@Override
		public Object[] getArguments() {
			return null;
		}

		@Override
		public String getDefaultMessage() {
			return this.resolvableString;
		}
	}

}
