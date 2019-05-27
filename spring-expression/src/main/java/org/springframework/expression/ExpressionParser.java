package org.springframework.expression;

/**
 * 将表达式字符串解析为可以计算的已编译表达式.
 * 支持解析模板以及标准表达式字符串.
 */
public interface ExpressionParser {

	/**
	 * 解析表达式字符串并返回可用于重复评估的Expression对象.
	 * <p>Some examples:
	 * <pre class="code">
	 *     3 + 4
	 *     name.firstName
	 * </pre>
	 * 
	 * @param expressionString 要解析的原始表达式字符串
	 * 
	 * @return 解析的表达式的求值器
	 * @throws ParseException 解析期间发生异常
	 */
	Expression parseExpression(String expressionString) throws ParseException;

	/**
	 * 解析表达式字符串, 并返回可用于重复评估的Expression对象.
	 * <p>Some examples:
	 * <pre class="code">
	 *     3 + 4
	 *     name.firstName
	 * </pre>
	 * 
	 * @param expressionString 要解析的原始表达式字符串
	 * @param context 影响此表达式解析例程的上下文(可选)
	 * 
	 * @return 解析的表达式的求值器
	 * @throws ParseException 解析期间发生异常
	 */
	Expression parseExpression(String expressionString, ParserContext context) throws ParseException;

}
