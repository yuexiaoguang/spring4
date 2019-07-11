package org.springframework.transaction.interceptor;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * {@link TransactionAttribute}对象的PropertyEditor.
 * 接受只需传播代码的
 * <p>{@code PROPAGATION_NAME, ISOLATION_NAME, readOnly, timeout_NNNN,+Exception1,-Exception2}
 * <p>的格式的字符串. 示例:
 * <p>{@code PROPAGATION_MANDATORY, ISOLATION_DEFAULT}
 *
 * <p>可以是<strong>任何</strong>顺序.
 * 传播和隔离代码必须使用TransactionDefinition类中的常量名称.
 * 超时值以秒为单位. 如果未指定超时, 则事务管理器将应用特定于特定事务管理器的默认超时.
 *
 * <p>异常名称子字符串之前的"+"表示即使抛出此异常, 事务也应该提交; "-"表示应该回滚.
 */
public class TransactionAttributeEditor extends PropertyEditorSupport {

	/**
	 * 格式为PROPAGATION_NAME,ISOLATION_NAME,readOnly,timeout_NNNN,+Exception1,-Exception2.
	 * null或空字符串表示该方法是非事务性的.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasLength(text)) {
			// tokenize it with ","
			String[] tokens = StringUtils.commaDelimitedListToStringArray(text);
			RuleBasedTransactionAttribute attr = new RuleBasedTransactionAttribute();
			for (int i = 0; i < tokens.length; i++) {
				// 去除首尾空格.
				String token = StringUtils.trimWhitespace(tokens[i].trim());
				// 检查是否包含文本中的非法空格.
				if (StringUtils.containsWhitespace(token)) {
					throw new IllegalArgumentException(
							"Transaction attribute token contains illegal whitespace: [" + token + "]");
				}
				// Check token type.
				if (token.startsWith(RuleBasedTransactionAttribute.PREFIX_PROPAGATION)) {
					attr.setPropagationBehaviorName(token);
				}
				else if (token.startsWith(RuleBasedTransactionAttribute.PREFIX_ISOLATION)) {
					attr.setIsolationLevelName(token);
				}
				else if (token.startsWith(RuleBasedTransactionAttribute.PREFIX_TIMEOUT)) {
					String value = token.substring(DefaultTransactionAttribute.PREFIX_TIMEOUT.length());
					attr.setTimeout(Integer.parseInt(value));
				}
				else if (token.equals(RuleBasedTransactionAttribute.READ_ONLY_MARKER)) {
					attr.setReadOnly(true);
				}
				else if (token.startsWith(RuleBasedTransactionAttribute.PREFIX_COMMIT_RULE)) {
					attr.getRollbackRules().add(new NoRollbackRuleAttribute(token.substring(1)));
				}
				else if (token.startsWith(RuleBasedTransactionAttribute.PREFIX_ROLLBACK_RULE)) {
					attr.getRollbackRules().add(new RollbackRuleAttribute(token.substring(1)));
				}
				else {
					throw new IllegalArgumentException("Invalid transaction attribute token: [" + token + "]");
				}
			}
			setValue(attr);
		}
		else {
			setValue(null);
		}
	}

}
