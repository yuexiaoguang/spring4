package org.springframework.transaction.interceptor;

/**
 * 标记{@link RollbackRuleAttribute}的子类，它与{@code RollbackRuleAttribute}超类具有相反的行为.
 */
@SuppressWarnings("serial")
public class NoRollbackRuleAttribute extends RollbackRuleAttribute {

	/**
	 * @param clazz {@code Throwable}类
	 */
	public NoRollbackRuleAttribute(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * @param exceptionName 异常名称模式
	 */
	public NoRollbackRuleAttribute(String exceptionName) {
		super(exceptionName);
	}

	@Override
	public String toString() {
		return "No" + super.toString();
	}

}
