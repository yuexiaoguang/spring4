package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TransactionAttribute实现, 通过应用多个回滚规则(正面和负面), 来确定给定异常是否应该导致事务回滚.
 * 如果没有规则与异常相关，则其行为类似于DefaultTransactionAttribute (回滚运行时异常).
 *
 * <p>{@link TransactionAttributeEditor}创建此类的对象.
 */
@SuppressWarnings("serial")
public class RuleBasedTransactionAttribute extends DefaultTransactionAttribute implements Serializable {

	/** 描述字符串中的异常回滚规则的前缀 */
	public static final String PREFIX_ROLLBACK_RULE = "-";

	/** 描述字符串中的异常提交规则的前缀 */
	public static final String PREFIX_COMMIT_RULE = "+";


	/** Static for optimal serializability */
	private static final Log logger = LogFactory.getLog(RuleBasedTransactionAttribute.class);

	private List<RollbackRuleAttribute> rollbackRules;


	/**
	 * 使用默认设置.
	 */
	public RuleBasedTransactionAttribute() {
		super();
	}

	/**
	 * 复制构造函数.
	 */
	public RuleBasedTransactionAttribute(RuleBasedTransactionAttribute other) {
		super(other);
		this.rollbackRules = new ArrayList<RollbackRuleAttribute>(other.rollbackRules);
	}

	/**
	 * @param propagationBehavior TransactionDefinition接口中的传播常量之一
	 * @param rollbackRules 要应用的RollbackRuleAttributes列表
	 */
	public RuleBasedTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
		super(propagationBehavior);
		this.rollbackRules = rollbackRules;
	}


	/**
	 * 设置要应用的{@code RollbackRuleAttribute}对象列表 (和/或{@code NoRollbackRuleAttribute}对象).
	 */
	public void setRollbackRules(List<RollbackRuleAttribute> rollbackRules) {
		this.rollbackRules = rollbackRules;
	}

	/**
	 * 返回{@code RollbackRuleAttribute}对象的列表 (never {@code null}).
	 */
	public List<RollbackRuleAttribute> getRollbackRules() {
		if (this.rollbackRules == null) {
			this.rollbackRules = new LinkedList<RollbackRuleAttribute>();
		}
		return this.rollbackRules;
	}


	/**
	 * 最浅的规则 (即, 在异常的继承层次结构中最接近).
	 * 如果没有规则适用 (-1), 则返回false.
	 */
	@Override
	public boolean rollbackOn(Throwable ex) {
		if (logger.isTraceEnabled()) {
			logger.trace("Applying rules to determine whether transaction should rollback on " + ex);
		}

		RollbackRuleAttribute winner = null;
		int deepest = Integer.MAX_VALUE;

		if (this.rollbackRules != null) {
			for (RollbackRuleAttribute rule : this.rollbackRules) {
				int depth = rule.getDepth(ex);
				if (depth >= 0 && depth < deepest) {
					deepest = depth;
					winner = rule;
				}
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Winning rollback rule is: " + winner);
		}

		// 如果没有规则匹配, 则为用户超类行为 (非受检时回滚).
		if (winner == null) {
			logger.trace("No relevant rollback rule found: applying default rules");
			return super.rollbackOn(ex);
		}

		return !(winner instanceof NoRollbackRuleAttribute);
	}


	@Override
	public String toString() {
		StringBuilder result = getAttributeDescription();
		if (this.rollbackRules != null) {
			for (RollbackRuleAttribute rule : this.rollbackRules) {
				String sign = (rule instanceof NoRollbackRuleAttribute ? PREFIX_COMMIT_RULE : PREFIX_ROLLBACK_RULE);
				result.append(',').append(sign).append(rule.getExceptionName());
			}
		}
		return result.toString();
	}

}
