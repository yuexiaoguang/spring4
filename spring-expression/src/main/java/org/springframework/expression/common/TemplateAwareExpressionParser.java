package org.springframework.expression.common;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;

/**
 * 支持模板的表达式解析器.
 * 可以由不提供模板的第一类支持的表达式解析器进行子类化.
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

	/**
	 * 非模板表达式的默认ParserContext实例.
	 */
	private static final ParserContext NON_TEMPLATE_PARSER_CONTEXT = new ParserContext() {
		@Override
		public String getExpressionPrefix() {
			return null;
		}
		@Override
		public String getExpressionSuffix() {
			return null;
		}
		@Override
		public boolean isTemplate() {
			return false;
		}
	};


	@Override
	public Expression parseExpression(String expressionString) throws ParseException {
		return parseExpression(expressionString, NON_TEMPLATE_PARSER_CONTEXT);
	}

	@Override
	public Expression parseExpression(String expressionString, ParserContext context) throws ParseException {
		if (context == null) {
			context = NON_TEMPLATE_PARSER_CONTEXT;
		}

		if (context.isTemplate()) {
			return parseTemplate(expressionString, context);
		}
		else {
			return doParseExpression(expressionString, context);
		}
	}


	private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
		if (expressionString.isEmpty()) {
			return new LiteralExpression("");
		}

		Expression[] expressions = parseExpressions(expressionString, context);
		if (expressions.length == 1) {
			return expressions[0];
		}
		else {
			return new CompositeStringExpression(expressionString, expressions);
		}
	}

	/**
	 * 使用配置的解析器解析给定的表达式字符串.
	 * 表达式字符串可以包含 "${...}"标记中包含的任意数量的表达式.
	 * 例如: "foo${expr0}bar${expr1}".
	 * 静态文本片段也将作为只返回静态文本片段的表达式返回.
	 * 因此, 评估所有返回的表达式并连接结果将生成完整的评估字符串.
	 * 解包只发现了最外层的分隔符, 所以字符串 'hello ${foo${abc}}'会分成'hello ' 和 'foo${abc}'.
	 * 这意味着表达式语言支持使用 ${..}作为其功能的一部分.
	 * 解析可以意识到嵌入式表达式的结构.
	 * 它假定括号 '(', 方括号 '[' 和大括号 '}' 必须在表达式中成对出现, 除非它们在字符串文字中并且字符串文字以单引号开始和终止.
	 * 
	 * @param expressionString 表达式字符串
	 * 
	 * @return 解析后的表达式
	 * @throws ParseException 当表达式无法解析时
	 */
	private Expression[] parseExpressions(String expressionString, ParserContext context) throws ParseException {
		List<Expression> expressions = new LinkedList<Expression>();
		String prefix = context.getExpressionPrefix();
		String suffix = context.getExpressionSuffix();
		int startIdx = 0;

		while (startIdx < expressionString.length()) {
			int prefixIndex = expressionString.indexOf(prefix, startIdx);
			if (prefixIndex >= startIdx) {
				// 找到了一个内部表达式 - 这是一个复合的
				if (prefixIndex > startIdx) {
					expressions.add(new LiteralExpression(expressionString.substring(startIdx, prefixIndex)));
				}
				int afterPrefixIndex = prefixIndex + prefix.length();
				int suffixIndex = skipToCorrectEndSuffix(suffix, expressionString, afterPrefixIndex);
				if (suffixIndex == -1) {
					throw new ParseException(expressionString, prefixIndex,
							"No ending suffix '" + suffix + "' for expression starting at character " +
							prefixIndex + ": " + expressionString.substring(prefixIndex));
				}
				if (suffixIndex == afterPrefixIndex) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
							"' at character " + prefixIndex);
				}
				String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
				expr = expr.trim();
				if (expr.isEmpty()) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
							"' at character " + prefixIndex);
				}
				expressions.add(doParseExpression(expr, context));
				startIdx = suffixIndex + suffix.length();
			}
			else {
				// 在字符串中找不到更多的 ${expressions}, 将剩下的添加为静态文本
				expressions.add(new LiteralExpression(expressionString.substring(startIdx)));
				startIdx = expressionString.length();
			}
		}

		return expressions.toArray(new Expression[expressions.size()]);
	}

	/**
	 * 如果可以在提供的表达式字符串中的指定位置找到指定的后缀, 则返回true.
	 * 
	 * @param expressionString 可能包含后缀的表达式字符串
	 * @param pos 检查后缀的起始位置
	 * @param suffix 后缀字符串
	 */
	private boolean isSuffixHere(String expressionString, int pos, String suffix) {
		int suffixPosition = 0;
		for (int i = 0; i < suffix.length() && pos < expressionString.length(); i++) {
			if (expressionString.charAt(pos++) != suffix.charAt(suffixPosition++)) {
				return false;
			}
		}
		if (suffixPosition != suffix.length()) {
			// 在完全找到后缀之前, expressionString已经用完了
			return false;
		}
		return true;
	}

	/**
	 * 处理嵌套, 例如 '${...${...}}', 其中第一个${ 的正确结尾是最后的 }.
	 * 
	 * @param suffix 后缀
	 * @param expressionString 表达式字符串
	 * @param afterPrefixIndex 最近找到的前缀位置, 寻找其匹配的结尾后缀
	 * 
	 * @return 正确匹配nextSuffix的位置, 或-1
	 */
	private int skipToCorrectEndSuffix(String suffix, String expressionString, int afterPrefixIndex)
			throws ParseException {

		// 咀嚼表达文本 - 根据规则:
		// 括号必须成对出现: () [] {}
		// 字符串文字是 "..." 或 '...', 它们可能包含不匹配的括号
		int pos = afterPrefixIndex;
		int maxlen = expressionString.length();
		int nextSuffix = expressionString.indexOf(suffix, afterPrefixIndex);
		if (nextSuffix == -1) {
			return -1; // 缺少后缀
		}
		Stack<Bracket> stack = new Stack<Bracket>();
		while (pos < maxlen) {
			if (isSuffixHere(expressionString, pos, suffix) && stack.isEmpty()) {
				break;
			}
			char ch = expressionString.charAt(pos);
			switch (ch) {
				case '{':
				case '[':
				case '(':
					stack.push(new Bracket(ch, pos));
					break;
				case '}':
				case ']':
				case ')':
					if (stack.isEmpty()) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " without an opening '" +
								Bracket.theOpenBracketFor(ch) + "'");
					}
					Bracket p = stack.pop();
					if (!p.compatibleWithCloseBracket(ch)) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " but most recent opening is '" + p.bracket +
								"' at position " + p.pos);
					}
					break;
				case '\'':
				case '"':
					// 跳到文字的末尾
					int endLiteral = expressionString.indexOf(ch, pos + 1);
					if (endLiteral == -1) {
						throw new ParseException(expressionString, pos,
								"Found non terminating string literal starting at position " + pos);
					}
					pos = endLiteral;
					break;
			}
			pos++;
		}
		if (!stack.isEmpty()) {
			Bracket p = stack.pop();
			throw new ParseException(expressionString, p.pos, "Missing closing '" +
					Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket + "' at position " + p.pos);
		}
		if (!isSuffixHere(expressionString, pos, suffix)) {
			return -1;
		}
		return pos;
	}


	/**
	 * 实际解析表达式字符串, 并返回一个Expression对象.
	 * 
	 * @param expressionString 要解析的原始表达式字符串
	 * @param context 影响此表达式解析例程的上下文 (可选)
	 * 
	 * @return 解析表达式的求值器
	 * @throws ParseException 解析期间发生异常
	 */
	protected abstract Expression doParseExpression(String expressionString, ParserContext context)
			throws ParseException;


	/**
	 * 这捕获了一种括号及其在表达式中出现的位置.
	 * 如果由于无法找到相关的结尾括号而必须报告错误, 使用的位置信息.
	 * 括号用于描述: 方括号 [], 圆括号 (), 花括号 {}
	 */
	private static class Bracket {

		char bracket;

		int pos;

		Bracket(char bracket, int pos) {
			this.bracket = bracket;
			this.pos = pos;
		}

		boolean compatibleWithCloseBracket(char closeBracket) {
			if (this.bracket == '{') {
				return closeBracket == '}';
			}
			else if (this.bracket == '[') {
				return closeBracket == ']';
			}
			return closeBracket == ')';
		}

		static char theOpenBracketFor(char closeBracket) {
			if (closeBracket == '}') {
				return '{';
			}
			else if (closeBracket == ']') {
				return '[';
			}
			return '(';
		}

		static char theCloseBracketFor(char openBracket) {
			if (openBracket == '{') {
				return '}';
			}
			else if (openBracket == '[') {
				return ']';
			}
			return ')';
		}
	}

}
