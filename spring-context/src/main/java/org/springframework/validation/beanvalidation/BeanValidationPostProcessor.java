package org.springframework.validation.beanvalidation;

import java.util.Iterator;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 简单的{@link BeanPostProcessor}, 用于检查Spring管理的bean中的JSR-303约束注解,
 * 在调用bean的init方法之前, 在出现约束冲突时抛出初始化异常.
 */
public class BeanValidationPostProcessor implements BeanPostProcessor, InitializingBean {

	private Validator validator;

	private boolean afterInitialization = false;


	/**
	 * 将JSR-303 Validator设置为委托给验证bean.
	 * <p>默认值是默认的ValidatorFactory的默认Validator.
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * 使用其默认的Validator, 设置要委托给的 JSR-303 ValidatorFactory, 以验证bean.
	 * <p>默认值是默认的ValidatorFactory的默认Validator.
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validator = validatorFactory.getValidator();
	}

	/**
	 * 选择是否在bean初始化之后(i.e. 在init方法之后) 而不是之前 (默认)执行验证.
	 * <p>默认"false" (初始化之前).
	 * 设置为"true" (初始化之后), 如果希望为init方法提供在验证之前填充约束字段的机会.
	 */
	public void setAfterInitialization(boolean afterInitialization) {
		this.afterInitialization = afterInitialization;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.validator == null) {
			this.validator = Validation.buildDefaultValidatorFactory().getValidator();
		}
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (!this.afterInitialization) {
			doValidate(bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (this.afterInitialization) {
			doValidate(bean);
		}
		return bean;
	}


	/**
	 * 执行给定bean的验证.
	 * 
	 * @param bean 要验证的bean实例
	 */
	protected void doValidate(Object bean) {
		Set<ConstraintViolation<Object>> result = this.validator.validate(bean);
		if (!result.isEmpty()) {
			StringBuilder sb = new StringBuilder("Bean state is invalid: ");
			for (Iterator<ConstraintViolation<Object>> it = result.iterator(); it.hasNext();) {
				ConstraintViolation<Object> violation = it.next();
				sb.append(violation.getPropertyPath()).append(" - ").append(violation.getMessage());
				if (it.hasNext()) {
					sb.append("; ");
				}
			}
			throw new BeanInitializationException(sb.toString());
		}
	}

}
