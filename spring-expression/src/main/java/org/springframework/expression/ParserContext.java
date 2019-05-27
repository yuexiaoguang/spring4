package org.springframework.expression;

/**
 * 提供给表达式解析器的输入, 可以影响表达式解析/编译例程.
 */
public interface ParserContext {

	/**
	 * 被解析的表达式是否是模板.
	 * 模板表达式由可以和可评估的块混合的文字文本组成.
	 * Some examples:
	 * <pre class="code">
	 * 	   Some literal text
	 *     Hello #{name.firstName}!
	 *     #{3 + 4}
	 * </pre>
	 * 
	 * @return true 如果表达式是模板, 否则false
	 */
	boolean isTemplate();

	/**
	 * 对于模板表达式, 返回标识字符串中表达式块开头的前缀.
	 * 例如: "${"
	 * 
	 * @return 标识表达式块开头的前缀
	 */
	String getExpressionPrefix();

	/**
	 * 对于模板表达式, 返回标识字符串中表达式块开头的后缀.
	 * 例如: "}"
	 * 
	 * @return 标识表达式结尾的后缀
	 */
	String getExpressionSuffix();


	/**
	 * 默认的ParserContext实现, 它启用模板表达式解析模式.
	 * 表达式前缀是 #{, 表达式后缀是 }.
	 */
	public static final ParserContext TEMPLATE_EXPRESSION = new ParserContext() {

		@Override
		public String getExpressionPrefix() {
			return "#{";
		}

		@Override
		public String getExpressionSuffix() {
			return "}";
		}

		@Override
		public boolean isTemplate() {
			return true;
		}

	};

}
