package org.springframework.transaction.support;

import java.io.Serializable;

import org.springframework.core.Constants;
import org.springframework.transaction.TransactionDefinition;

/**
 * {@link TransactionDefinition}接口的默认实现, 提供bean样式配置和合理的默认值
 * (PROPAGATION_REQUIRED, ISOLATION_DEFAULT, TIMEOUT_DEFAULT, readOnly=false).
 *
 * <p>{@link TransactionTemplate}和
 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}的基类.
 */
@SuppressWarnings("serial")
public class DefaultTransactionDefinition implements TransactionDefinition, Serializable {

	/** TransactionDefinition中定义的传播常量的前缀 */
	public static final String PREFIX_PROPAGATION = "PROPAGATION_";

	/** TransactionDefinition中定义的隔离常量的前缀 */
	public static final String PREFIX_ISOLATION = "ISOLATION_";

	/** 描述字符串中事务​​超时值的前缀 */
	public static final String PREFIX_TIMEOUT = "timeout_";

	/** 描述字符串中的只读事务的标记 */
	public static final String READ_ONLY_MARKER = "readOnly";


	/** TransactionDefinition的常量实例 */
	static final Constants constants = new Constants(TransactionDefinition.class);

	private int propagationBehavior = PROPAGATION_REQUIRED;

	private int isolationLevel = ISOLATION_DEFAULT;

	private int timeout = TIMEOUT_DEFAULT;

	private boolean readOnly = false;

	private String name;


	public DefaultTransactionDefinition() {
	}

	/**
	 * 复制构造函数.
	 */
	public DefaultTransactionDefinition(TransactionDefinition other) {
		this.propagationBehavior = other.getPropagationBehavior();
		this.isolationLevel = other.getIsolationLevel();
		this.timeout = other.getTimeout();
		this.readOnly = other.isReadOnly();
		this.name = other.getName();
	}

	/**
	 * @param propagationBehavior TransactionDefinition接口中的传播常量之一
	 */
	public DefaultTransactionDefinition(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}


	/**
	 * 在TransactionDefinition中通过相应常量的名称设置传播行为, e.g. "PROPAGATION_REQUIRED".
	 * 
	 * @param constantName 常量名称
	 * 
	 * @throws IllegalArgumentException 如果提供的值无法解析为{@code PROPAGATION_}常量之一, 或为{@code null}
	 */
	public final void setPropagationBehaviorName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith(PREFIX_PROPAGATION)) {
			throw new IllegalArgumentException("Only propagation constants allowed");
		}
		setPropagationBehavior(constants.asNumber(constantName).intValue());
	}

	/**
	 * 设置传播行为. 必须是TransactionDefinition接口中的传播常量之一.
	 * 默认 PROPAGATION_REQUIRED.
	 * <p>专门设计用于{@link #PROPAGATION_REQUIRED}或{@link #PROPAGATION_REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务.
	 * 如果希望隔离级别声明在参与具有不同隔离级别的现有事务时被拒绝,
	 * 在事务管理器上将 "validateExistingTransactions"标志切换为"true".
	 * <p>请注意, 不支持自定义隔离级别的事务管理器在给定除{@link #ISOLATION_DEFAULT}之外的任何其他级别时将抛出异常.
	 * 
	 * @throws IllegalArgumentException 如果提供的值不是{@code PROPAGATION_}常量之一
	 */
	public final void setPropagationBehavior(int propagationBehavior) {
		if (!constants.getValues(PREFIX_PROPAGATION).contains(propagationBehavior)) {
			throw new IllegalArgumentException("Only values of propagation constants allowed");
		}
		this.propagationBehavior = propagationBehavior;
	}

	@Override
	public final int getPropagationBehavior() {
		return this.propagationBehavior;
	}

	/**
	 * 在TransactionDefinition中通过相应常量的名称设置隔离级别, e.g. "ISOLATION_DEFAULT".
	 * 
	 * @param constantName 常量名称
	 * 
	 * @throws IllegalArgumentException 如果提供的值无法解析为{@code ISOLATION_}常量之一, 或为{@code null}
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith(PREFIX_ISOLATION)) {
			throw new IllegalArgumentException("Only isolation constants allowed");
		}
		setIsolationLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * 设置隔离级别.
	 * 必须是TransactionDefinition接口中的隔离常量之一. 默认 ISOLATION_DEFAULT.
	 * <p>专门设计用于{@link #PROPAGATION_REQUIRED}或{@link #PROPAGATION_REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务.
	 * 如果希望隔离级别声明在参与具有不同隔离级别的现有事务时被拒绝,
	 * 在事务管理器上将"validateExistingTransactions"标志切换为"true".
	 * <p>请注意, 不支持自定义隔离级别的事务管理器在给定除{@link #ISOLATION_DEFAULT}之外的任何其他级别时将抛出异常.
	 * 
	 * @throws IllegalArgumentException 如果提供的值不是{@code ISOLATION_}常量之一
	 */
	public final void setIsolationLevel(int isolationLevel) {
		if (!constants.getValues(PREFIX_ISOLATION).contains(isolationLevel)) {
			throw new IllegalArgumentException("Only values of isolation constants allowed");
		}
		this.isolationLevel = isolationLevel;
	}

	@Override
	public final int getIsolationLevel() {
		return this.isolationLevel;
	}

	/**
	 * 设置要应用的超时, 以秒为单位.
	 * 默认 TIMEOUT_DEFAULT (-1).
	 * <p>专门设计用于{@link #PROPAGATION_REQUIRED}或{@link #PROPAGATION_REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务.
	 * <p>请注意, 不支持超时的事务管理器在给出除{@link #TIMEOUT_DEFAULT}以外的任何其他超时时将抛出异常.
	 */
	public final void setTimeout(int timeout) {
		if (timeout < TIMEOUT_DEFAULT) {
			throw new IllegalArgumentException("Timeout must be a positive integer or TIMEOUT_DEFAULT");
		}
		this.timeout = timeout;
	}

	@Override
	public final int getTimeout() {
		return this.timeout;
	}

	/**
	 * 设置是否优化为只读事务.
	 * 默认 "false".
	 * <p>只读标志适用于任何事务上下文, 无论是由实际资源事务 ({@link #PROPAGATION_REQUIRED}/ {@link #PROPAGATION_REQUIRES_NEW})
	 * 还是在资源级别以非事务方式运行 ({@link #PROPAGATION_SUPPORTS}).
	 * 在后一种情况下, 该标志仅适用于应用程序内的托管资源, 例如Hibernate {@code Session}.
	 * <p>这仅仅是实际事务子系统的提示; 它<i>不一定</i>导致写访问尝试失败.
	 * 当被要求进行只读事务时, 无法解释只读提示的事务管理器将<i>不会</i>抛出异常.
	 */
	public final void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public final boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * 设置此事务的名称. 默认无.
	 * <p>这将用作事务监视器中显示的事务名称 (例如, WebLogic的).
	 */
	public final void setName(String name) {
		this.name = name;
	}

	@Override
	public final String getName() {
		return this.name;
	}


	/**
	 * 此实现比较{@code toString()}结果.
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof TransactionDefinition && toString().equals(other.toString())));
	}

	/**
	 * 此实现返回{@code toString()}的哈希码.
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * 返回此事务定义的标识说明.
	 * <p>格式与
	 * {@link org.springframework.transaction.interceptor.TransactionAttributeEditor}使用的格式匹配,
	 * 以便能够将{@code toString}结果提供给
	 * {@link org.springframework.transaction.interceptor.TransactionAttribute}类型的bean属性.
	 * <p>必须在子类中重写以获得正确的{@code equals}和{@code hashCode}行为.
	 * 或者, 可以覆盖{@link #equals}和{@link #hashCode}.
	 */
	@Override
	public String toString() {
		return getDefinitionDescription().toString();
	}

	/**
	 * 返回此事务定义的标识说明.
	 * <p>可用于子类, 包含在其{@code toString()}结果中.
	 */
	protected final StringBuilder getDefinitionDescription() {
		StringBuilder result = new StringBuilder();
		result.append(constants.toCode(this.propagationBehavior, PREFIX_PROPAGATION));
		result.append(',');
		result.append(constants.toCode(this.isolationLevel, PREFIX_ISOLATION));
		if (this.timeout != TIMEOUT_DEFAULT) {
			result.append(',');
			result.append(PREFIX_TIMEOUT).append(this.timeout);
		}
		if (this.readOnly) {
			result.append(',');
			result.append(READ_ONLY_MARKER);
		}
		return result;
	}

}
