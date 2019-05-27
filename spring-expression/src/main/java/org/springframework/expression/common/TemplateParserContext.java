package org.springframework.expression.common;

import org.springframework.expression.ParserContext;

/**
 * 用于模板解析的可配置{@link ParserContext}实现.
 * Expects the expression prefix and suffix as constructor arguments.
 */
public class TemplateParserContext implements ParserContext {

	private final String expressionPrefix;

	private final String expressionSuffix;


	/**
	 * 使用默认的 "#{" 前缀和 "}" 后缀.
	 */
	public TemplateParserContext() {
		this("#{", "}");
	}

	/**
	 * @param expressionPrefix 要使用的表达式前缀
	 * @param expressionSuffix 要使用的表达式后缀
	 */
	public TemplateParserContext(String expressionPrefix, String expressionSuffix) {
		this.expressionPrefix = expressionPrefix;
		this.expressionSuffix = expressionSuffix;
	}


	@Override
	public final boolean isTemplate() {
		return true;
	}

	@Override
	public final String getExpressionPrefix() {
		return this.expressionPrefix;
	}

	@Override
	public final String getExpressionSuffix() {
		return this.expressionSuffix;
	}

}
