package org.springframework.web.bind.support;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * JSR-303 {@link ConstraintValidatorFactory}实现,
 * 委托给当前的Spring {@link WebApplicationContext}, 用于创建自动装配的{@link ConstraintValidator}实例.
 *
 * <p>与{@link org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory}相比,
 * 此变体用于在标准{@code validation.xml}文件中进行声明性使用, e.g. 与JAX-RS或JAX-WS结合使用.
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
	 * 检索要使用的Spring {@link WebApplicationContext}.
	 * 默认实现返回为线程上下文类加载器注册的当前{@link WebApplicationContext}.
	 * 
	 * @return 当前WebApplicationContext (never {@code null})
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
