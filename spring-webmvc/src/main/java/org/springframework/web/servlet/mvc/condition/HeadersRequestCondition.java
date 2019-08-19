package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;

/**
 * 逻辑连接 (' && ') 请求条件, 它将请求与一组header表达式匹配, 并使用{@link RequestMapping#headers()}中定义的语法.
 *
 * <p>传递给构造函数的表达式名称为'Accept' 或 'Content-Type'将被忽略.
 * See {@link ConsumesRequestCondition} and {@link ProducesRequestCondition} for those.
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

	private final static HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();


	private final Set<HeaderExpression> expressions;


	/**
	 * header名称为'Accept' 或 'Content-Type'的表达式将被忽略.
	 * 有关这些内容, 请参阅{@link ConsumesRequestCondition}和{@link ProducesRequestCondition}.
	 * 
	 * @param headers 在{@link RequestMapping#headers()}中定义语法的媒体类型表达式; 如果为0, 则条件将匹配每个请求
	 */
	public HeadersRequestCondition(String... headers) {
		this(parseExpressions(headers));
	}

	private HeadersRequestCondition(Collection<HeaderExpression> conditions) {
		this.expressions = Collections.unmodifiableSet(new LinkedHashSet<HeaderExpression>(conditions));
	}


	private static Collection<HeaderExpression> parseExpressions(String... headers) {
		Set<HeaderExpression> expressions = new LinkedHashSet<HeaderExpression>();
		if (headers != null) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name) || "Content-Type".equalsIgnoreCase(expr.name)) {
					continue;
				}
				expressions.add(expr);
			}
		}
		return expressions;
	}

	/**
	 * 返回包含的请求header表达式.
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<NameValueExpression<String>>(this.expressions);
	}

	@Override
	protected Collection<HeaderExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回一个新实例, 其中包含"this"和"other"实例的header表达式的并集.
	 */
	@Override
	public HeadersRequestCondition combine(HeadersRequestCondition other) {
		Set<HeaderExpression> set = new LinkedHashSet<HeaderExpression>(this.expressions);
		set.addAll(other.expressions);
		return new HeadersRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有表达式, 则返回"this"实例; 或者{@code null}.
	 */
	@Override
	public HeadersRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		for (HeaderExpression expression : expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 返回:
	 * <ul>
	 * <li>如果两个条件具有相同数量的header表达式, 则为0
	 * <li>如果"this"实例具有更多header表达式, 则小于0
	 * <li>如果"other"实例具有更多header表达式, 则大于0
	 * </ul>
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的,
	 * 并且每个实例仅包含匹配的header表达式, 否则为空.
	 */
	@Override
	public int compareTo(HeadersRequestCondition other, HttpServletRequest request) {
		return other.expressions.size() - this.expressions.size();
	}


	/**
	 * 将单个header表达式解析并匹配到请求.
	 */
	static class HeaderExpression extends AbstractNameValueExpression<String> {

		public HeaderExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return false;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return (request.getHeader(this.name) != null);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getHeader(this.name));
		}
	}

}
