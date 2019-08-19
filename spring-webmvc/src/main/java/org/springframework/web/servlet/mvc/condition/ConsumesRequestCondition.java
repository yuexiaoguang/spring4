package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * 逻辑或 (' || ') 请求条件, 用于将请求的'Content-Type' header与媒体类型表达式列表进行匹配.
 * 支持两种媒体类型表达式, 在{@link RequestMapping#consumes()}
 * 和 {@link RequestMapping#headers()}中描述, 其中header名称为'Content-Type'.
 * 无论使用哪种语法, 语义都是相同的.
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	private final static ConsumesRequestCondition PRE_FLIGHT_MATCH = new ConsumesRequestCondition();

	private final List<ConsumeMediaTypeExpression> expressions;


	/**
	 * @param consumes 使用{@link RequestMapping#consumes()}中描述的语法的表达式; 如果提供了0表达式, 则条件将匹配每个请求
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * 使用"consumes"和"header"表达式创建一个新实例.
	 * header名称不是'Content-Type'或没有定义header值的"Header"表达式将被忽略.
	 * 如果总共提供0个表达式, 则条件将匹配每个请求.
	 * 
	 * @param consumes 如{@link RequestMapping#consumes()}中所述
	 * @param headers 如{@link RequestMapping#headers()}中所述
	 */
	public ConsumesRequestCondition(String[] consumes, String[] headers) {
		this(parseExpressions(consumes, headers));
	}

	/**
	 * 接受解析的媒体类型表达式.
	 */
	private ConsumesRequestCondition(Collection<ConsumeMediaTypeExpression> expressions) {
		this.expressions = new ArrayList<ConsumeMediaTypeExpression>(expressions);
		Collections.sort(this.expressions);
	}


	private static Set<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, String[] headers) {
		Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<ConsumeMediaTypeExpression>();
		if (headers != null) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Content-Type".equalsIgnoreCase(expr.name)) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ConsumeMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		if (consumes != null) {
			for (String consume : consumes) {
				result.add(new ConsumeMediaTypeExpression(consume));
			}
		}
		return result;
	}


	/**
	 * 返回包含的MediaType表达式.
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<MediaTypeExpression>(this.expressions);
	}

	/**
	 * 返回此条件的媒体类型, 不包括否定表达式.
	 */
	public Set<MediaType> getConsumableMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<MediaType>();
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			if (!expression.isNegated()) {
				result.add(expression.getMediaType());
			}
		}
		return result;
	}

	/**
	 * 条件是否具有媒体类型表达式.
	 */
	@Override
	public boolean isEmpty() {
		return this.expressions.isEmpty();
	}

	@Override
	protected Collection<ConsumeMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 如果它有任何表达式, 则返回"other"实例; 否则返回"this"实例.
	 * 实际上, 这意味着方法级"consumes"会覆盖类型级"consumes"条件.
	 */
	@Override
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return !other.expressions.isEmpty() ? other : this;
	}

	/**
	 * 检查是否有任何包含的媒体类型表达式与给定的请求'Content-Type' header匹配, 并返回一个保证仅包含匹配表达式的实例.
	 * 匹配通过{@link MediaType#includes(MediaType)}执行.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 如果条件不包含表达式, 则为同一实例;
	 * 或仅具有匹配表达式的新条件; 如果没有表达式匹配, 则为{@code null}
	 */
	@Override
	public ConsumesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		if (isEmpty()) {
			return this;
		}

		MediaType contentType;
		try {
			contentType = (StringUtils.hasLength(request.getContentType()) ?
					MediaType.parseMediaType(request.getContentType()) :
					MediaType.APPLICATION_OCTET_STREAM);
		}
		catch (InvalidMediaTypeException ex) {
			return null;
		}

		Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<ConsumeMediaTypeExpression>(this.expressions);
		for (Iterator<ConsumeMediaTypeExpression> iterator = result.iterator(); iterator.hasNext();) {
			ConsumeMediaTypeExpression expression = iterator.next();
			if (!expression.match(contentType)) {
				iterator.remove();
			}
		}
		return (!result.isEmpty() ? new ConsumesRequestCondition(result) : null);
	}

	/**
	 * 返回:
	 * <ul>
	 * <li>如果两个条件具有相同数量的表达式, 则为0
	 * <li>如果"this"具有更多或更多特定媒体类型表达式, 则小于0
	 * <li>如果"other"具有更多或更多特定媒体类型表达式, 则大于0
	 * </ul>
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的,
	 * 并且每个实例仅包含匹配的可消费媒体类型表达式, 否则为空.
	 */
	@Override
	public int compareTo(ConsumesRequestCondition other, HttpServletRequest request) {
		if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		}
		else if (this.expressions.isEmpty()) {
			return 1;
		}
		else if (other.expressions.isEmpty()) {
			return -1;
		}
		else {
			return this.expressions.get(0).compareTo(other.expressions.get(0));
		}
	}


	/**
	 * 将单个媒体类型表达式解析并匹配到请求的'Content-Type' header.
	 */
	static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		public final boolean match(MediaType contentType) {
			boolean match = getMediaType().includes(contentType);
			return (!isNegated() ? match : !match);
		}
	}
}
