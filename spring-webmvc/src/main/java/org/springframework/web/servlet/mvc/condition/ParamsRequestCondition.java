package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

/**
 * 一个逻辑连接 (' && ')请求条件, 它使用{@link RequestMapping#params()}中定义的语法匹配请求的一组参数表达式.
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

	private final Set<ParamExpression> expressions;


	/**
	 * @param params {@link RequestMapping#params()}中定义的语法的表达式; 如果为0, 则条件将匹配每个请求.
	 */
	public ParamsRequestCondition(String... params) {
		this(parseExpressions(params));
	}

	private ParamsRequestCondition(Collection<ParamExpression> conditions) {
		this.expressions = Collections.unmodifiableSet(new LinkedHashSet<ParamExpression>(conditions));
	}


	private static Collection<ParamExpression> parseExpressions(String... params) {
		Set<ParamExpression> expressions = new LinkedHashSet<ParamExpression>();
		if (params != null) {
			for (String param : params) {
				expressions.add(new ParamExpression(param));
			}
		}
		return expressions;
	}


	/**
	 * 返回包含的请求参数表达式.
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<NameValueExpression<String>>(this.expressions);
	}

	@Override
	protected Collection<ParamExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回一个新实例, 其中包含来自 "this"和"other"实例的param表达式的并集.
	 */
	@Override
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		Set<ParamExpression> set = new LinkedHashSet<ParamExpression>(this.expressions);
		set.addAll(other.expressions);
		return new ParamsRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有param表达式, 则返回"this"实例; 或者{@code null}.
	 */
	@Override
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		for (ParamExpression expression : expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 返回:
	 * <ul>
	 * <li>如果两个条件具有相同数量的参数表达式, 则为0
	 * <li>如果"this"实例具有更多参数表达式, 则小于0
	 * <li>如果"other"实例具有更多参数表达式, 则大于0
	 * </ul>
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的,
	 * 并且每个实例仅包含匹配的参数表达式, 否则为空.
	 */
	@Override
	public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
		return (other.expressions.size() - this.expressions.size());
	}


	/**
	 * 将单个param表达式解析并匹配到请求.
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		ParamExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return true;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return WebUtils.hasSubmitParameter(request, this.name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
		}
	}
}
