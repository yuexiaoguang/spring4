package org.springframework.validation.beanvalidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * JSR-303 {@link ConstraintValidatorFactory}实现, 它委托Spring BeanFactory创建自动装配的{@link ConstraintValidator}实例.
 *
 * <p>请注意, 此类用于编程用途, 而不用于标准{@code validation.xml}文件中的声明性用法.
 * 考虑{@link org.springframework.web.bind.support.SpringWebConstraintValidatorFactory}以在Web应用程序中进行声明性使用, e.g. 使用JAX-RS 或 JAX-WS.
 */
public class SpringConstraintValidatorFactory implements ConstraintValidatorFactory {

	private final AutowireCapableBeanFactory beanFactory;


	/**
	 * @param beanFactory 目标BeanFactory
	 */
	public SpringConstraintValidatorFactory(AutowireCapableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		return this.beanFactory.createBean(key);
	}

	// Bean Validation 1.1 releaseInstance method
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		this.beanFactory.destroyBean(instance);
	}

}
