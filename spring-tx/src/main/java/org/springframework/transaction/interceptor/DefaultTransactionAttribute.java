package org.springframework.transaction.interceptor;

import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Spring的常见事务属性实现.
 * 在运行时回滚, 但默认情况下为非受检异常.
 */
@SuppressWarnings("serial")
public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {

	private String qualifier;

	private String descriptor;


	/**
	 * 使用默认设置.
	 */
	public DefaultTransactionAttribute() {
		super();
	}

	/**
	 * 复制构造函数.
	 */
	public DefaultTransactionAttribute(TransactionAttribute other) {
		super(other);
	}

	/**
	 * @param propagationBehavior TransactionDefinition接口中的传播常量之一
	 */
	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}


	/**
	 * 将限定符值与此事务属性相关联.
	 * <p>这可以用于选择相应的事务管理器来处理该特定事务.
	 */
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	/**
	 * 返回与此事务属性关联的限定符值.
	 */
	@Override
	public String getQualifier() {
		return this.qualifier;
	}

	/**
	 * 为此事务属性设置描述符, e.g. 指示属性应用的位置.
	 */
	public void setDescriptor(String descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * 返回此事务属性的描述符, 或{@code null}.
	 */
	public String getDescriptor() {
		return this.descriptor;
	}

	/**
	 * 默认行为与EJB一样: 在非受检异常上回滚 ({@link RuntimeException}), 假设任何业务规则之外的意外结果.
	 * 此外, 还尝试在{@link Error}上回滚, 这显然也是一个意外结果.
	 * 相比之下, 受检异常被视为业务异常, 因此是事务性业务方法的常规预期结果,
	 * i.e. 替代返回值, 仍然允许完成资源操作.
	 * <p>这与TransactionTemplate的默认行为基本一致, 除了TransactionTemplate还回滚未声明的受检异常 (一个极端情况).
	 * 对于声明式事务, 希望将受检异常有意声明为业务异常, 从而导致默认提交.
	 */
	@Override
	public boolean rollbackOn(Throwable ex) {
		return (ex instanceof RuntimeException || ex instanceof Error);
	}


	/**
	 * 返回此事务属性的标识说明.
	 * <p>可用于子类, 包含在其{@code toString()}结果中.
	 */
	protected final StringBuilder getAttributeDescription() {
		StringBuilder result = getDefinitionDescription();
		if (this.qualifier != null) {
			result.append("; '").append(this.qualifier).append("'");
		}
		return result;
	}

}
