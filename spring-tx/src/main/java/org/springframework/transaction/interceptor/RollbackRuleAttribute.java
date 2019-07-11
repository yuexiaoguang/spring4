package org.springframework.transaction.interceptor;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * 确定给定异常 (和任何子类)是否应该导致回滚的规则.
 *
 * <p>可以应用多个此类规则来确定, 在抛出异常后, 事务是应该提交还是回滚.
 */
@SuppressWarnings("serial")
public class RollbackRuleAttribute implements Serializable{

	/**
	 * {@link RuntimeException RuntimeExceptions}的{@link RollbackRuleAttribute 回滚规则}.
	 */
	public static final RollbackRuleAttribute ROLLBACK_ON_RUNTIME_EXCEPTIONS =
			new RollbackRuleAttribute(RuntimeException.class);


	/**
	 * 可以保存异常, 解析类名, 但总是需要FQN.
	 * 这种方式进行多个字符串比较, 但多久决定是否在异常后回滚事务?
	 */
	private final String exceptionName;


	/**
	 * <p>这是构造与提供的{@link Exception}类(和子类)匹配的回滚规则的首选方法.
	 * 
	 * @param clazz 可以是{@link Throwable}及其子类
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code clazz}不是{@code Throwable}类型, 或是{@code null}
	 */
	public RollbackRuleAttribute(Class<?> clazz) {
		Assert.notNull(clazz, "'clazz' cannot be null");
		if (!Throwable.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException(
					"Cannot construct rollback rule from [" + clazz.getName() + "]: it's not a Throwable");
		}
		this.exceptionName = clazz.getName();
	}

	/**
	 * <p>这可以是子字符串, 目前没有通配符支持.
	 * 例如, "ServletException"将匹配{@code javax.servlet.ServletException}及其子类.
	 * <p><b>NB:</b> 仔细考虑模式的具体程度, 以及是否包含包信息 (这不是强制性的).
	 * 例如, "Exception"几乎可以匹配任何内容, 并且可能会隐藏其他规则.
	 * 如果"Exception"用于为所有受检异常定义规则, 则"java.lang.Exception"将是正确的.
	 * 使用更多不寻常的异常名称, 例如"BaseBusinessException", 不需要使用完全包限定名称.
	 * 
	 * @param exceptionName 异常名称模式; 也可以是完全包限定的类名
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code exceptionName}是{@code null}或为空
	 */
	public RollbackRuleAttribute(String exceptionName) {
		Assert.hasText(exceptionName, "'exceptionName' cannot be null or empty");
		this.exceptionName = exceptionName;
	}


	/**
	 * 返回异常名称的模式.
	 */
	public String getExceptionName() {
		return exceptionName;
	}

	/**
	 * 返回超类匹配的深度.
	 * <p>{@code 0}表示{@code ex}完全匹配. 如果没有匹配, 则返回{@code -1}.
	 * 否则, 最低深度获胜.
	 */
	public int getDepth(Throwable ex) {
		return getDepth(ex.getClass(), 0);
	}


	private int getDepth(Class<?> exceptionClass, int depth) {
		if (exceptionClass.getName().contains(this.exceptionName)) {
			// Found it!
			return depth;
		}
		// 找不到它...
		if (exceptionClass == Throwable.class) {
			return -1;
		}
		return getDepth(exceptionClass.getSuperclass(), depth + 1);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RollbackRuleAttribute)) {
			return false;
		}
		RollbackRuleAttribute rhs = (RollbackRuleAttribute) other;
		return this.exceptionName.equals(rhs.exceptionName);
	}

	@Override
	public int hashCode() {
		return this.exceptionName.hashCode();
	}

	@Override
	public String toString() {
		return "RollbackRuleAttribute with pattern [" + this.exceptionName + "]";
	}

}
