package org.springframework.context.annotation;

/**
 * {@link IllegalStateException}的标记子类, 允许在调用代码中使用显式catch子句.
 */
@SuppressWarnings("serial")
class ConflictingBeanDefinitionException extends IllegalStateException {

	public ConflictingBeanDefinitionException(String message) {
		super(message);
	}

}
