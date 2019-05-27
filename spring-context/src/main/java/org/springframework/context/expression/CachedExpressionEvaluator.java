package org.springframework.context.expression;

import java.util.Map;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 共享的实用程序类, 用于评估和缓存在{@link java.lang.reflect.AnnotatedElement}上定义的SpEL表达式.
 */
public abstract class CachedExpressionEvaluator {

	private final SpelExpressionParser parser;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	protected CachedExpressionEvaluator(SpelExpressionParser parser) {
		Assert.notNull(parser, "SpelExpressionParser must not be null");
		this.parser = parser;
	}

	protected CachedExpressionEvaluator() {
		this(new SpelExpressionParser());
	}


	/**
	 * 返回要使用的{@link SpelExpressionParser}.
	 */
	protected SpelExpressionParser getParser() {
		return this.parser;
	}

	/**
	 * 返回一个共享的参数名称发现者, 它在内部缓存数据.
	 * @since 4.3
	 */
	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}


	/**
	 * 返回指定SpEL值的{@link Expression}.
	 * <p>解析表达式.
	 * 
	 * @param cache 要使用的缓存
	 * @param elementKey 定义表达式的元素
	 * @param expression 要解析的表达式
	 */
	protected Expression getExpression(Map<ExpressionKey, Expression> cache,
			AnnotatedElementKey elementKey, String expression) {

		ExpressionKey expressionKey = createKey(elementKey, expression);
		Expression expr = cache.get(expressionKey);
		if (expr == null) {
			expr = getParser().parseExpression(expression);
			cache.put(expressionKey, expr);
		}
		return expr;
	}

	private ExpressionKey createKey(AnnotatedElementKey elementKey, String expression) {
		return new ExpressionKey(elementKey, expression);
	}


	protected static class ExpressionKey implements Comparable<ExpressionKey> {

		private final AnnotatedElementKey element;

		private final String expression;

		protected ExpressionKey(AnnotatedElementKey element, String expression) {
			this.element = element;
			this.expression = expression;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ExpressionKey)) {
				return false;
			}
			ExpressionKey otherKey = (ExpressionKey) other;
			return (this.element.equals(otherKey.element) &&
					ObjectUtils.nullSafeEquals(this.expression, otherKey.expression));
		}

		@Override
		public int hashCode() {
			return this.element.hashCode() + (this.expression != null ? this.expression.hashCode() * 29 : 0);
		}

		@Override
		public String toString() {
			return this.element + (this.expression != null ? " with expression \"" + this.expression : "\"");
		}

		@Override
		public int compareTo(ExpressionKey other) {
			int result = this.element.toString().compareTo(other.element.toString());
			if (result == 0 && this.expression != null) {
				result = this.expression.compareTo(other.expression);
			}
			return result;
		}
	}

}
