package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link RequestCondition}的保存器, 在请求条件的类型无法提前知道时有用, e.g. 自定义条件.
 * 由于此类也是{@code RequestCondition}的实现, 因此它有效地修饰了保存的请求条件,
 * 并允许它与类型和null安全方式中的其他请求条件进行组合和比较.
 *
 * <p>当两个{@code RequestConditionHolder}实例相互组合或进行比较时, 预计它们所拥有的条件属于同一类型.
 * 如果不是同一类型, 则引发{@link ClassCastException}.
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

	private final RequestCondition<Object> condition;


	/**
	 * @param requestCondition 要保存的条件, 可能是{@code null}
	 */
	@SuppressWarnings("unchecked")
	public RequestConditionHolder(RequestCondition<?> requestCondition) {
		this.condition = (RequestCondition<Object>) requestCondition;
	}


	/**
	 * 返回保存的请求条件, 或{@code null}.
	 */
	public RequestCondition<?> getCondition() {
		return this.condition;
	}

	@Override
	protected Collection<?> getContent() {
		return (this.condition != null ? Collections.singleton(this.condition) : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " ";
	}

	/**
	 * 在确保条件属于同一类型后, 合并两个RequestConditionHolder实例所保存的请求条件.
	 * 或者如果一个保存器是空的, 则返回另一个保存器.
	 */
	@Override
	public RequestConditionHolder combine(RequestConditionHolder other) {
		if (this.condition == null && other.condition == null) {
			return this;
		}
		else if (this.condition == null) {
			return other;
		}
		else if (other.condition == null) {
			return this;
		}
		else {
			assertEqualConditionTypes(other);
			RequestCondition<?> combined = (RequestCondition<?>) this.condition.combine(other.condition);
			return new RequestConditionHolder(combined);
		}
	}

	/**
	 * 确保保存的请求条件属于同一类型.
	 */
	private void assertEqualConditionTypes(RequestConditionHolder other) {
		Class<?> clazz = this.condition.getClass();
		Class<?> otherClazz = other.condition.getClass();
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " and " + otherClazz);
		}
	}

	/**
	 * 获取保存的请求条件的匹配条件, 将其包装在新的RequestConditionHolder实例中.
	 * 或者, 如果这是一个空的保存器, 则返回相同的保存器实例.
	 */
	@Override
	public RequestConditionHolder getMatchingCondition(HttpServletRequest request) {
		if (this.condition == null) {
			return this;
		}
		RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(request);
		return (match != null ? new RequestConditionHolder(match) : null);
	}

	/**
	 * 在确保条件属于同一类型之后, 比较两个RequestConditionHolder实例保存的请求条件.
	 * 或者如果一个保存器是空的, 则优选另一个保存器.
	 */
	@Override
	public int compareTo(RequestConditionHolder other, HttpServletRequest request) {
		if (this.condition == null && other.condition == null) {
			return 0;
		}
		else if (this.condition == null) {
			return 1;
		}
		else if (other.condition == null) {
			return -1;
		}
		else {
			assertEqualConditionTypes(other);
			return this.condition.compareTo(other.condition, request);
		}
	}
}
