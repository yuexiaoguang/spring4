package org.springframework.web.bind.support;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * JSR-303 {@link ConstraintValidatorFactory} implementation that delegates to
 * the current Spring {@link WebApplicationContext} for creating autowired
 * {@link ConstraintValidator} instances.
 *
 * <p>In contrast to
 * {@link org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory},
 * this variant is meant for declarative use in a standard {@code validation.xml} file,
 * e.g. in combination with JAX-RS or JAX-WS.
 */
public class SpringWebConstraintValidatorFactory implements ConstraintValidatorFactory {

	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		return getWebApplicationContext().getAutowireCapableBeanFactory().createBean(key);
	}

	// Bean Validation 1.1 releaseInstance method
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		getWebApplicationContext().getAutowireCapableBeanFactory().destroyBean(instance);
	}


	/**
	 * Retrieve the Spring {@link WebApplicationContext} to use.
	 * The default implementation returns the current {@link WebApplicationContext}
	 * as registered for the thread context class loader.
	 * @return the current WebApplicationContext (never {@code null})
	 */
	protected WebApplicationContext getWebApplicationContext() {
		WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext registered for current thread - " +
					"consider overriding SpringWebConstraintValidatorFactory.getWebApplicationContext()");
		}
		return wac;
	}

}
