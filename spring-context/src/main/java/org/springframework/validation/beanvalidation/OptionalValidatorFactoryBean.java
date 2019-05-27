package org.springframework.validation.beanvalidation;

import javax.validation.ValidationException;

import org.apache.commons.logging.LogFactory;

/**
 * {@link LocalValidatorFactoryBean}子类,
 * 在没有Bean Validation提供程序可用的情况下, 将{@link org.springframework.validation.Validator}调用转换为no-ops.
 *
 * <p>这是Spring的MVC配置命名空间使用的实际类, 如果{@code javax.validation} API存在但没有配置显式Validator.
 */
public class OptionalValidatorFactoryBean extends LocalValidatorFactoryBean {

	@Override
	public void afterPropertiesSet() {
		try {
			super.afterPropertiesSet();
		}
		catch (ValidationException ex) {
			LogFactory.getLog(getClass()).debug("Failed to set up a Bean Validation provider", ex);
		}
	}

}
