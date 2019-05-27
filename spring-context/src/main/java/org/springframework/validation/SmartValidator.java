package org.springframework.validation;

/**
 * {@link Validator}接口的扩展变体，添加了对验证'提示'的支持.
 */
public interface SmartValidator extends Validator {

	/**
	 * 验证提供的{@code target}对象, 该对象必须是{@link Class}的类型, {@link #supports(Class)}方法通常返回{@code true}.
	 * <p>提供的{@link Errors errors}实例可用于报告任何结果验证错误.
	 * <p><b>{@code validate()}的此变体支持验证提示, 例如针对JSR-303提供器的验证组</b>
	 * (在这种情况下, 提供的提示对象需要是{@code Class}类型的注解参数).
	 * <p>Note: 验证提示可能会被实际目标{@code Validator}忽略,
	 * 在这种情况下, 此方法的行为应与其常规{@link #validate(Object, Errors)}兄弟一样.
	 * 
	 * @param target 要验证的对象 (can be {@code null})
	 * @param errors 关于验证过程的上下文状态 (never {@code null})
	 * @param validationHints 要传递给验证引擎的一个或多个提示对象
	 */
	void validate(Object target, Errors errors, Object... validationHints);

}
