package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * 逻辑或 (' || ') 请求条件, 用于将请求的'Accept' header与媒体类型表达式列表进行匹配.
 * 支持两种媒体类型表达式, 在{@link RequestMapping#produces()}
 * 和{@link RequestMapping#headers()}中描述, 其中header名称为'Accept'.
 * 无论使用哪种语法, 语义都是相同的.
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

	private static final ProducesRequestCondition PRE_FLIGHT_MATCH = new ProducesRequestCondition();

	private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

	private static final List<ProduceMediaTypeExpression> MEDIA_TYPE_ALL_LIST =
			Collections.singletonList(new ProduceMediaTypeExpression("*/*"));


	private final List<ProduceMediaTypeExpression> expressions;

	private final ContentNegotiationManager contentNegotiationManager;


	/**
	 * 如果总共提供0个表达式, 则此条件将与任何请求匹配.
	 * 
	 * @param produces 语法由{@link RequestMapping#produces()}定义的表达式
	 */
	public ProducesRequestCondition(String... produces) {
		this(produces, null, null);
	}

	/**
	 * header名称不是'Accept'或没有定义header值的"Header"表达式将被忽略.
	 * 如果总共提供0个表达式, 则此条件将与任何请求匹配.
	 * 
	 * @param produces 语法由{@link RequestMapping#produces()}定义的表达式
	 * @param headers 语法由{@link RequestMapping#headers()}定义的表达式
	 */
	public ProducesRequestCondition(String[] produces, String[] headers) {
		this(produces, headers, null);
	}

	/**
	 * 与{@link #ProducesRequestCondition(String[], String[])}相同, 但也接受{@link ContentNegotiationManager}.
	 * 
	 * @param produces 语法由{@link RequestMapping#produces()}定义的表达式
	 * @param headers 语法由{@link RequestMapping#headers()}定义的表达式
	 * @param manager 用于确定请求的媒体类型
	 */
	public ProducesRequestCondition(String[] produces, String[] headers, ContentNegotiationManager manager) {
		this.expressions = new ArrayList<ProduceMediaTypeExpression>(parseExpressions(produces, headers));
		Collections.sort(this.expressions);
		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
	}

	/**
	 * 具有已解析的媒体类型表达式.
	 */
	private ProducesRequestCondition(Collection<ProduceMediaTypeExpression> expressions, ContentNegotiationManager manager) {
		this.expressions = new ArrayList<ProduceMediaTypeExpression>(expressions);
		Collections.sort(this.expressions);
		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
	}


	private Set<ProduceMediaTypeExpression> parseExpressions(String[] produces, String[] headers) {
		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<ProduceMediaTypeExpression>();
		if (headers != null) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name)) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ProduceMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		if (produces != null) {
			for (String produce : produces) {
				result.add(new ProduceMediaTypeExpression(produce));
			}
		}
		return result;
	}

	/**
	 * 返回包含的"produces"表达式.
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<MediaTypeExpression>(this.expressions);
	}

	/**
	 * 返回包含的可生成的媒体类型, 不包括否定表达式.
	 */
	public Set<MediaType> getProducibleMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<MediaType>();
		for (ProduceMediaTypeExpression expression : this.expressions) {
			if (!expression.isNegated()) {
				result.add(expression.getMediaType());
			}
		}
		return result;
	}

	/**
	 * 条件是否具有任何媒体类型表达式.
	 */
	@Override
	public boolean isEmpty() {
		return this.expressions.isEmpty();
	}

	@Override
	protected List<ProduceMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 如果它有任何表达式, 则返回"other"实例; 否则返回"this"实例.
	 * 实际上, 这意味着方法级"produces"会覆盖类型级"produces"条件.
	 */
	@Override
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查是否有任何包含的媒体类型表达式与给定的请求'Content-Type' header匹配, 并返回一个保证仅包含匹配表达式的实例.
	 * 匹配通过{@link MediaType#isCompatibleWith(MediaType)}执行.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 如果没有表达式, 则为同一实例;
	 * 或具有匹配表达式的新条件; 如果没有表达式匹配, 则为{@code null}.
	 */
	@Override
	public ProducesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		if (isEmpty()) {
			return this;
		}

		List<MediaType> acceptedMediaTypes;
		try {
			acceptedMediaTypes = getAcceptedMediaTypes(request);
		}
		catch (HttpMediaTypeException ex) {
			return null;
		}

		Set<ProduceMediaTypeExpression> result = new LinkedHashSet<ProduceMediaTypeExpression>(expressions);
		for (Iterator<ProduceMediaTypeExpression> iterator = result.iterator(); iterator.hasNext();) {
			ProduceMediaTypeExpression expression = iterator.next();
			if (!expression.match(acceptedMediaTypes)) {
				iterator.remove();
			}
		}
		if (!result.isEmpty()) {
			return new ProducesRequestCondition(result, this.contentNegotiationManager);
		}
		else if (acceptedMediaTypes.contains(MediaType.ALL)) {
			return EMPTY_CONDITION;
		}
		else {
			return null;
		}
	}

	/**
	 * 比较这个和另一个"produces"条件如下:
	 * <ol>
	 * <li>通过{@link MediaType#sortByQualityValue(List)}按质量值排序'Accept' header媒体类型, 并迭代列表.
	 * <li>获取每个"produces"条件中匹配媒体类型的第一个索引, 首先与{@link MediaType#equals(Object)}匹配,
	 * 然后使用{@link MediaType#includes(MediaType)}.
	 * <li>如果找到较低的索引, 则该索引处的条件获胜.
	 * <li>如果两个索引相等, 则索引中的媒体类型将与{@link MediaType#SPECIFICITY_COMPARATOR}进一步比较.
	 * </ol>
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的,
	 * 并且每个实例仅包含匹配的可生成媒体类型表达式, 否则为空.
	 */
	@Override
	public int compareTo(ProducesRequestCondition other, HttpServletRequest request) {
		try {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(request);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				int thisIndex = this.indexOfEqualMediaType(acceptedMediaType);
				int otherIndex = other.indexOfEqualMediaType(acceptedMediaType);
				int result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
				thisIndex = this.indexOfIncludedMediaType(acceptedMediaType);
				otherIndex = other.indexOfIncludedMediaType(acceptedMediaType);
				result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			// should never happen
			throw new IllegalStateException("Cannot compare without having any requested media types", ex);
		}
	}

	private List<MediaType> getAcceptedMediaTypes(HttpServletRequest request) throws HttpMediaTypeNotAcceptableException {
		List<MediaType> mediaTypes = this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
		return mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes;
	}

	private int indexOfEqualMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			MediaType currentMediaType = getExpressionsToCompare().get(i).getMediaType();
			if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType()) &&
					mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
				return i;
			}
		}
		return -1;
	}

	private int indexOfIncludedMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			if (mediaType.includes(getExpressionsToCompare().get(i).getMediaType())) {
				return i;
			}
		}
		return -1;
	}

	private int compareMatchingMediaTypes(ProducesRequestCondition condition1, int index1,
			ProducesRequestCondition condition2, int index2) {

		int result = 0;
		if (index1 != index2) {
			result = index2 - index1;
		}
		else if (index1 != -1) {
			ProduceMediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
			ProduceMediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
			result = expr1.compareTo(expr2);
			result = (result != 0) ? result : expr1.getMediaType().compareTo(expr2.getMediaType());
		}
		return result;
	}

	/**
	 * 返回包含的"produces"表达式, 如果该表达式为空, 则返回带有{@code MediaType_ALL}表达式的列表.
	 */
	private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
		return (this.expressions.isEmpty() ? MEDIA_TYPE_ALL_LIST : this.expressions);
	}


	/**
	 * 将单个媒体类型表达式解析并匹配到请求的'Accept' header.
	 */
	static class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		public final boolean match(List<MediaType> acceptedMediaTypes) {
			boolean match = matchMediaType(acceptedMediaTypes);
			return (!isNegated() ? match : !match);
		}

		private boolean matchMediaType(List<MediaType> acceptedMediaTypes) {
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				if (getMediaType().isCompatibleWith(acceptedMediaType)) {
					return true;
				}
			}
			return false;
		}
	}

}
