package org.springframework.transaction.interceptor;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * PropertyEditor for {@link TransactionAttribute} objects. Accepts a String of form
 * <p>{@code PROPAGATION_NAME, ISOLATION_NAME, readOnly, timeout_NNNN,+Exception1,-Exception2}
 * <p>where only propagation code is required. For example:
 * <p>{@code PROPAGATION_MANDATORY, ISOLATION_DEFAULT}
 *
 * <p>The tokens can be in <strong>any</strong> order. Propagation and isolation codes
 * must use the names of the constants in the TransactionDefinition class. Timeout values
 * are in seconds. If no timeout is specified, the transaction manager will apply a default
 * timeout specific to the particular transaction manager.
 *
 * <p>A "+" before an exception name substring indicates that transactions should commit
 * even if this exception is thrown; a "-" that they should roll back.
 */
public class TransactionAttributeEditor extends PropertyEditorSupport {

	/**
	 * Format is PROPAGATION_NAME,ISOLATION_NAME,readOnly,timeout_NNNN,+Exception1,-Exception2.
	 * Null or the empty string means that the method is non transactional.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasLength(text)) {
			// tokenize it with ","
			String[] tokens = StringUtils.commaDelimitedListToStringArray(text);
			RuleBasedTransactionAttribute attr = new RuleBasedTransactionAttribute();
			for (int i = 0; i < tokens.length; i++) {
				// Trim leading and trailing whitespace.
				String token = StringUtils.trimWhitespace(tokens[i].trim());
				// Check whether token contains illegal whitespace within text.
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
