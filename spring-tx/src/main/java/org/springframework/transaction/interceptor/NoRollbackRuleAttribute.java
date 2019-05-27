package org.springframework.transaction.interceptor;

/**
 * Tag subclass of {@link RollbackRuleAttribute} that has the opposite behavior
 * to the {@code RollbackRuleAttribute} superclass.
 */
@SuppressWarnings("serial")
public class NoRollbackRuleAttribute extends RollbackRuleAttribute {

	/**
	 * Create a new instance of the {@code NoRollbackRuleAttribute} class
	 * for the supplied {@link Throwable} class.
	 * @param clazz the {@code Throwable} class
	 */
	public NoRollbackRuleAttribute(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * Create a new instance of the {@code NoRollbackRuleAttribute} class
	 * for the supplied {@code exceptionName}.
	 * @param exceptionName the exception name pattern
	 */
	public NoRollbackRuleAttribute(String exceptionName) {
		super(exceptionName);
	}

	@Override
	public String toString() {
		return "No" + super.toString();
	}

}
