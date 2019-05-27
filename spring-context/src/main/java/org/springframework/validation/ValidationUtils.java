package org.springframework.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 提供了调用{@link Validator}和拒绝空字段的便捷方法.
 *
 * <p>使用{@link #rejectIfEmpty} 或 {@link #rejectIfEmptyOrWhitespace}时, {@code Validator}实现中的空字段检查可以成为单行内容.
 */
public abstract class ValidationUtils {

	private static final Log logger = LogFactory.getLog(ValidationUtils.class);


	/**
	 * 为提供的对象和{@link Errors}实例调用给定的{@link Validator}.
	 * 
	 * @param validator 要调用的{@code Validator} (must not be {@code null})
	 * @param obj 要将参数绑定到的对象
	 * @param errors 应该存储错误的{@link Errors}实例 (must not be {@code null})
	 * 
	 * @throws IllegalArgumentException 如果{@code Validator}或{@code Errors}参数中的任何一个是{@code null},
	 * 或者如果提供的{@code Validator}不{@link Validator#supports(Class) 支持}提供的对象的类型验证
	 */
	public static void invokeValidator(Validator validator, Object obj, Errors errors) {
		invokeValidator(validator, obj, errors, (Object[]) null);
	}

	/**
	 * 为提供的对象和{@link Errors}实例调用给定的{@link Validator}/{@link SmartValidator}.
	 * 
	 * @param validator 要调用的{@code Validator} (must not be {@code null})
	 * @param obj 要将参数绑定到的对象
	 * @param errors 应该存储错误的{@link Errors}实例 (must not be {@code null})
	 * @param validationHints 要传递给验证引擎的一个或多个提示对象
	 * 
	 * @throws IllegalArgumentException 如果{@code Validator}或{@code Errors}参数中的任何一个是{@code null},
	 * 或者如果提供的{@code Validator}不{@link Validator#supports(Class) 支持}提供的对象的类型验证
	 */
	public static void invokeValidator(Validator validator, Object obj, Errors errors, Object... validationHints) {
		Assert.notNull(validator, "Validator must not be null");
		Assert.notNull(errors, "Errors object must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking validator [" + validator + "]");
		}
		if (obj != null && !validator.supports(obj.getClass())) {
			throw new IllegalArgumentException(
					"Validator [" + validator.getClass() + "] does not support [" + obj.getClass() + "]");
		}
		if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
			((SmartValidator) validator).validate(obj, errors, validationHints);
		}
		else {
			validator.validate(obj, errors);
		}
		if (logger.isDebugEnabled()) {
			if (errors.hasErrors()) {
				logger.debug("Validator found " + errors.getErrorCount() + " errors");
			}
			else {
				logger.debug("Validator found no errors");
			}
		}
	}


	/**
	 * 如果值为空, 则拒绝具有给定错误代码的给定字段.
	 * <p>此上下文中的'空'值表示{@code null}或空字符串"".
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode) {
		rejectIfEmpty(errors, field, errorCode, null, null);
	}

	/**
	 * 如果值为空, 则拒绝具有给定错误代码的给定字段.
	 * <p>此上下文中的'空'值表示{@code null}或空字符串"".
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param defaultMessage 后备默认消息
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode, String defaultMessage) {
		rejectIfEmpty(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * 如果值为空, 则拒绝具有给定错误代码的给定字段.
	 * <p>此上下文中的'空'值表示{@code null}或空字符串"".
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param errorArgs 错误参数, 用于通过MessageFormat进行参数绑定 (can be {@code null})
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode, Object[] errorArgs) {
		rejectIfEmpty(errors, field, errorCode, errorArgs, null);
	}

	/**
	 * 如果值为空, 则拒绝具有给定错误代码的给定字段.
	 * <p>此上下文中的'空'值表示{@code null}或空字符串"".
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param errorArgs 错误参数, 用于通过MessageFormat进行参数绑定 (can be {@code null})
	 * @param defaultMessage 后备默认消息
	 */
	public static void rejectIfEmpty(
			Errors errors, String field, String errorCode, Object[] errorArgs, String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null || !StringUtils.hasLength(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}

	/**
	 * 如果值为空或仅包含空格, 则使用给定的错误代码拒绝给定字段.
	 * <p>此上下文中的'空'值表示{@code null}, 或空字符串"", 或完全由空格组成.
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 */
	public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode) {
		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, null);
	}

	/**
	 * 如果值为空或仅包含空格, 则使用给定的错误代码拒绝给定字段.
	 * <p>此上下文中的'空'值表示{@code null}, 或空字符串"", 或完全由空格组成.
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param defaultMessage 后备默认消息
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, String defaultMessage) {

		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * 如果值为空或仅包含空格, 则使用给定的错误代码拒绝给定字段.
	 * <p>此上下文中的'空'值表示{@code null}, 或空字符串"", 或完全由空格组成.
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param errorArgs 错误参数, 用于通过MessageFormat进行参数绑定 (can be {@code null})
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, Object[] errorArgs) {

		rejectIfEmptyOrWhitespace(errors, field, errorCode, errorArgs, null);
	}

	/**
	 * 如果值为空或仅包含空格, 则使用给定的错误代码拒绝给定字段.
	 * <p>此上下文中的'空'值表示{@code null}, 或空字符串"", 或完全由空格组成.
	 * <p>无需传入其字段正在验证的对象, 因为{@link Errors}实例可以自行解析字段值
	 * (它通常会保存对目标对象的内部引用).
	 * 
	 * @param errors 用于注册错误的{@code Errors}实例
	 * @param field 要检查的字段名称
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param errorArgs 错误参数, 用于通过MessageFormat进行参数绑定 (can be {@code null})
	 * @param defaultMessage 后备默认消息
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, Object[] errorArgs, String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null ||!StringUtils.hasText(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}
}
