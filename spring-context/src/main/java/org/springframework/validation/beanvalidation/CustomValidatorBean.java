package org.springframework.validation.beanvalidation;

import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

import org.springframework.beans.factory.InitializingBean;

/**
 * 可配置的bean类, 它通过其原始接口以及Spring {@link org.springframework.validation.Validator}接口公开特定的JSR-303 Validator.
 */
public class CustomValidatorBean extends SpringValidatorAdapter implements Validator, InitializingBean {

	private ValidatorFactory validatorFactory;

	private MessageInterpolator messageInterpolator;

	private TraversableResolver traversableResolver;


	/**
	 * 设置从中获取目标Validator的ValidatorFactory.
	 * <p>默认{@link javax.validation.Validation#buildDefaultValidatorFactory()}.
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validatorFactory = validatorFactory;
	}

	/**
	 * 指定要用于此Validator的自定义MessageInterpolator.
	 */
	public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
		this.messageInterpolator = messageInterpolator;
	}

	/**
	 * 指定要用于此Validator的自定义TraversableResolver.
	 */
	public void setTraversableResolver(TraversableResolver traversableResolver) {
		this.traversableResolver = traversableResolver;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.validatorFactory == null) {
			this.validatorFactory = Validation.buildDefaultValidatorFactory();
		}

		ValidatorContext validatorContext = this.validatorFactory.usingContext();
		MessageInterpolator targetInterpolator = this.messageInterpolator;
		if (targetInterpolator == null) {
			targetInterpolator = this.validatorFactory.getMessageInterpolator();
		}
		validatorContext.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));
		if (this.traversableResolver != null) {
			validatorContext.traversableResolver(this.traversableResolver);
		}

		setTargetValidator(validatorContext.getValidator());
	}

}
