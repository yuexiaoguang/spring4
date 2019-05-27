package org.springframework.validation.beanvalidation;

import java.lang.reflect.Method;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.validator.HibernateValidator;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * AOP Alliance {@link MethodInterceptor}实现, 它委托给JSR-303提供程序, 用于对带注解的方法执行方法级验证.
 *
 * <p>适用的方法对其参数和/或返回值具有JSR-303约束注解
 * (在后一种情况下, 在方法级别指定, 通常作为内联注解).
 *
 * <p>E.g.: {@code public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)}
 *
 * <p>验证组可以通过Spring的{@link Validated}注解在包含目标类的类型级别指定, 适用于该类的所有public服务方法.
 * 默认情况下, JSR-303将仅针对其默认组进行验证.
 *
 * <p>从Spring 4.0开始, 此功能需要Bean Validation 1.1提供程序 (例如Hibernate Validator 5.x)
 * 或带有Hibernate Validator 4.3的Bean Validation 1.0 API.
 * 实际的提供程序将被自动检测并自动调整.
 */
public class MethodValidationInterceptor implements MethodInterceptor {

	private static Method forExecutablesMethod;

	private static Method validateParametersMethod;

	private static Method validateReturnValueMethod;

	static {
		try {
			forExecutablesMethod = Validator.class.getMethod("forExecutables");
			Class<?> executableValidatorClass = forExecutablesMethod.getReturnType();
			validateParametersMethod = executableValidatorClass.getMethod(
					"validateParameters", Object.class, Method.class, Object[].class, Class[].class);
			validateReturnValueMethod = executableValidatorClass.getMethod(
					"validateReturnValue", Object.class, Method.class, Object.class, Class[].class);
		}
		catch (Exception ex) {
			// Bean Validation 1.1 ExecutableValidator API not available
		}
	}


	private volatile Validator validator;


	/**
	 * 使用下面默认的JSR-303验证器.
	 */
	public MethodValidationInterceptor() {
		this(forExecutablesMethod != null ? Validation.buildDefaultValidatorFactory() :
				HibernateValidatorDelegate.buildValidatorFactory());
	}

	/**
	 * @param validatorFactory 要使用的JSR-303 ValidatorFactory
	 */
	public MethodValidationInterceptor(ValidatorFactory validatorFactory) {
		this(validatorFactory.getValidator());
	}

	/**
	 * @param validator 要使用的JSR-303 Validator
	 */
	public MethodValidationInterceptor(Validator validator) {
		this.validator = validator;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Class<?>[] groups = determineValidationGroups(invocation);

		if (forExecutablesMethod != null) {
			// Standard Bean Validation 1.1 API
			Object execVal;
			try {
				execVal = ReflectionUtils.invokeMethod(forExecutablesMethod, this.validator);
			}
			catch (AbstractMethodError err) {
				// 可能是没有BV 1.1支持的适配器 (可能是lazy-init代理)
				Validator nativeValidator = this.validator.unwrap(Validator.class);
				execVal = ReflectionUtils.invokeMethod(forExecutablesMethod, nativeValidator);
				// 如果成功, 请存储本机Validator以供进一步使用
				this.validator = nativeValidator;
			}

			Method methodToValidate = invocation.getMethod();
			Set<ConstraintViolation<?>> result;

			try {
				result = (Set<ConstraintViolation<?>>) ReflectionUtils.invokeMethod(validateParametersMethod,
						execVal, invocation.getThis(), methodToValidate, invocation.getArguments(), groups);
			}
			catch (IllegalArgumentException ex) {
				// 可能是SPR-12237 / HV-1011中报告的接口和impl之间的通用类型不匹配
				// 尝试在实现类中找到桥接方法...
				methodToValidate = BridgeMethodResolver.findBridgedMethod(
						ClassUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass()));
				result = (Set<ConstraintViolation<?>>) ReflectionUtils.invokeMethod(validateParametersMethod,
						execVal, invocation.getThis(), methodToValidate, invocation.getArguments(), groups);
			}
			if (!result.isEmpty()) {
				throw new ConstraintViolationException(result);
			}

			Object returnValue = invocation.proceed();
			result = (Set<ConstraintViolation<?>>) ReflectionUtils.invokeMethod(validateReturnValueMethod,
					execVal, invocation.getThis(), methodToValidate, returnValue, groups);
			if (!result.isEmpty()) {
				throw new ConstraintViolationException(result);
			}
			return returnValue;
		}

		else {
			// Hibernate Validator 4.3's native API
			return HibernateValidatorDelegate.invokeWithinValidation(invocation, this.validator, groups);
		}
	}

	/**
	 * 确定要针对给定方法调用进行验证的验证组.
	 * <p>默认值是包含方法的目标类的{@link Validated}注解中指定的验证组.
	 * 
	 * @param invocation 当前MethodInvocation
	 * 
	 * @return 适用的验证组
	 */
	protected Class<?>[] determineValidationGroups(MethodInvocation invocation) {
		Validated validatedAnn = AnnotationUtils.findAnnotation(invocation.getMethod(), Validated.class);
		if (validatedAnn == null) {
			validatedAnn = AnnotationUtils.findAnnotation(invocation.getThis().getClass(), Validated.class);
		}
		return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
	}


	/**
	 * 内部类, 避免硬编码的Hibernate Validator 4.3依赖.
	 */
	private static class HibernateValidatorDelegate {

		public static ValidatorFactory buildValidatorFactory() {
			return Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
		}

		@SuppressWarnings("deprecation")
		public static Object invokeWithinValidation(MethodInvocation invocation, Validator validator, Class<?>[] groups)
				throws Throwable {

			org.hibernate.validator.method.MethodValidator methodValidator =
					validator.unwrap(org.hibernate.validator.method.MethodValidator.class);
			Set<org.hibernate.validator.method.MethodConstraintViolation<Object>> result =
					methodValidator.validateAllParameters(
							invocation.getThis(), invocation.getMethod(), invocation.getArguments(), groups);
			if (!result.isEmpty()) {
				throw new org.hibernate.validator.method.MethodConstraintViolationException(result);
			}
			Object returnValue = invocation.proceed();
			result = methodValidator.validateReturnValue(
					invocation.getThis(), invocation.getMethod(), returnValue, groups);
			if (!result.isEmpty()) {
				throw new org.hibernate.validator.method.MethodConstraintViolationException(result);
			}
			return returnValue;
		}
	}
}
