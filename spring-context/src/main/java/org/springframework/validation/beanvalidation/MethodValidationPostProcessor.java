package org.springframework.validation.beanvalidation;

import java.lang.annotation.Annotation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BeanPostProcessor}实现, 它委托给JSR-303提供程序, 用于对带注解的方法执行方法级验证.
 *
 * <p>适用的方法对其参数和/或返回值具有JSR-303约束注解
 * (在后一种情况下, 在方法级别指定, 通常作为内联注解), e.g.:
 *
 * <pre class="code">
 * public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)
 * </pre>
 *
 * <p>这种带注解方法的目标类需要在类型级别使用Spring的{@link Validated}注解进行注解, 以便搜索其内联约束注解的方法.
 * 验证组也可以通过{@code @Validated}指定. 默认情况下, JSR-303将仅针对其默认组进行验证.
 *
 * <p>从Spring 4.0开始, 此功能需要Bean Validation 1.1提供程序 (例如Hibernate Validator 5.x)
 * 或带有Hibernate Validator 4.3的Bean Validation 1.0 API.
 * 实际的提供程序将被自动检测并自动调整.
 */
@SuppressWarnings("serial")
public class MethodValidationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements InitializingBean {

	private Class<? extends Annotation> validatedAnnotationType = Validated.class;

	private Validator validator;


	/**
	 * 设置'validated'注解类型.
	 * 默认的验证注解类型是{@link Validated}注解.
	 * <p>此setter属性存在, 以便开发人员可以提供自己的(非Spring特定的)注解类型, 以指示应该在应用方法验证的意义上验证类.
	 * 
	 * @param validatedAnnotationType 所需的注解类型
	 */
	public void setValidatedAnnotationType(Class<? extends Annotation> validatedAnnotationType) {
		Assert.notNull(validatedAnnotationType, "'validatedAnnotationType' must not be null");
		this.validatedAnnotationType = validatedAnnotationType;
	}

	/**
	 * 设置用于验证方法的要委托给的JSR-303 Validator.
	 * <p>默认值是默认的ValidatorFactory的默认Validator.
	 */
	public void setValidator(Validator validator) {
		// 使用forExecutables支持解包到本机Validator
		if (validator instanceof LocalValidatorFactoryBean) {
			this.validator = ((LocalValidatorFactoryBean) validator).getValidator();
		}
		else if (validator instanceof SpringValidatorAdapter) {
			this.validator = validator.unwrap(Validator.class);
		}
		else {
			this.validator = validator;
		}
	}

	/**
	 * 使用其默认的Validator, 设置用于验证方法的要委托给的JSR-303 ValidatorFactory.
	 * <p>默认值是默认的ValidatorFactory的默认Validator.
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validator = validatorFactory.getValidator();
	}


	@Override
	public void afterPropertiesSet() {
		Pointcut pointcut = new AnnotationMatchingPointcut(this.validatedAnnotationType, true);
		this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(this.validator));
	}

	/**
	 * 创建用于方法验证的AOP增强, 与指定的'validated'注解的切点一起应用.
	 * 
	 * @param validator 要委托给的JSR-303 Validator
	 * 
	 * @return 要使用的拦截器 (通常但不一定是{@link MethodValidationInterceptor}或其子类)
	 */
	protected Advice createMethodValidationAdvice(Validator validator) {
		return (validator != null ? new MethodValidationInterceptor(validator) : new MethodValidationInterceptor());
	}

}
