package org.springframework.validation;

/**
 * 特定于应用程序的对象的验证器.
 *
 * <p>此接口完全脱离任何基础结构或上下文;
 * 也就是说, 它不会仅仅验证Web层, 数据访问层或任何层中的对象.
 * 因此, 它适用于应用程序的任何层, 并支持将验证逻辑封装为本身的一等公民.
 *
 * <p>在下面找到一个简单但完整的{@code Validator}实现, 它验证{@code UserLogin}实例的各种{@link String}属性是否为空
 * (也就是说, 它们不是{@code null}, 并且不完全由空格组成), 并且存在的任何密码的长度至少为{@code 'MINIMUM_PASSWORD_LENGTH'}个字符.
 *
 * <pre class="code"> public class UserLoginValidator implements Validator {
 *
 *    private static final int MINIMUM_PASSWORD_LENGTH = 6;
 *
 *    public boolean supports(Class clazz) {
 *       return UserLogin.class.isAssignableFrom(clazz);
 *    }
 *
 *    public void validate(Object target, Errors errors) {
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *       UserLogin login = (UserLogin) target;
 *       if (login.getPassword() != null
 *             && login.getPassword().trim().length() < MINIMUM_PASSWORD_LENGTH) {
 *          errors.rejectValue("password", "field.min.length",
 *                new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *                "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length.");
 *       }
 *    }
 * }</pre>
 *
 * <p>另请参阅Spring参考手册, 以更全面地讨论{@code Validator}接口及其在企业应用程序中的角色.
 */
public interface Validator {

	/**
	 * 此{@link Validator}是否可以{@link #validate(Object, Errors) 验证}提供的{@code clazz}的实例?
	 * <p>该方法通常像这样实现:
	 * <pre class="code">return Foo.class.isAssignableFrom(clazz);</pre>
	 * (其中{@code Foo}是实际对象实例的类 (或超类), 该对象实例被{@link #validate(Object, Errors)验证}.)
	 * 
	 * @param clazz 这个{@link Validator}的{@link Class}是否可以{@link #validate(Object, Errors) 验证}
	 * 
	 * @return {@code true} 如果这个{@link Validator}确实可以{@link #validate(Object, Errors) 验证}提供的{@code clazz}实例
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 验证提供的{@code target}对象, 该对象必须是{@link Class}, {@link #supports(Class)}方法通常返回{@code true}.
	 * <p>提供的{@link Errors errors}实例可用于报告任何结果验证错误.
	 * 
	 * @param target 要验证的对象 (can be {@code null})
	 * @param errors 关于验证过程的上下文状态(never {@code null})
	 */
	void validate(Object target, Errors errors);

}
