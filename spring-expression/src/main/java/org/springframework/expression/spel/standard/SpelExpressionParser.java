package org.springframework.expression.spel.standard;

import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.util.Assert;

/**
 * Spel解析器. 实例是可重用的和线程安全的.
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

	private final SpelParserConfiguration configuration;


	/**
	 * 使用默认设置创建解析器.
	 */
	public SpelExpressionParser() {
		this.configuration = new SpelParserConfiguration();
	}

	/**
	 * 使用指定的配置创建解析器.
	 * 
	 * @param configuration 自定义配置选项
	 */
	public SpelExpressionParser(SpelParserConfiguration configuration) {
		Assert.notNull(configuration, "SpelParserConfiguration must not be null");
		this.configuration = configuration;
	}


	public SpelExpression parseRaw(String expressionString) throws ParseException {
		return doParseExpression(expressionString, null);
	}

	@Override
	protected SpelExpression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		return new InternalSpelExpressionParser(this.configuration).doParseExpression(expressionString, context);
	}

}
