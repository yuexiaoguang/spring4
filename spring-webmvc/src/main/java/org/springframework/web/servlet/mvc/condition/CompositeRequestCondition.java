package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 实现{@link RequestCondition}约定, 通过委托给多个{@code RequestCondition}类型,
 * 并使用逻辑连接(' && ')来确保所有条件与给定请求匹配.
 *
 * <p>组合或比较{@code CompositeRequestCondition}实例时, 预期它们
 * (a) 包含相同数量的条件
 * (b) 相应索引中的条件属于同一类型.
 * 可以向构造函数提供{@code null}条件或根本没有条件.
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

	private final RequestConditionHolder[] requestConditions;


	/**
	 * 使用相同数量的条件创建{@code CompositeRequestCondition}实例非常重要, 这样可以对它们进行比较和组合.
	 * 可以接受{@code null}条件.
	 */
	public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
		this.requestConditions = wrap(requestConditions);
	}

	private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
		this.requestConditions = requestConditions;
	}


	private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
		RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
		for (int i = 0; i < rawConditions.length; i++) {
			wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
		}
		return wrappedConditions;
	}

	/**
	 * 此实例是否包含0个条件.
	 */
	@Override
	public boolean isEmpty() {
		return ObjectUtils.isEmpty(this.requestConditions);
	}

	/**
	 * 返回底层条件 (可能为空但从不为{@code null}).
	 */
	public List<RequestCondition<?>> getConditions() {
		return unwrap();
	}

	private List<RequestCondition<?>> unwrap() {
		List<RequestCondition<?>> result = new ArrayList<RequestCondition<?>>();
		for (RequestConditionHolder holder : this.requestConditions) {
			result.add(holder.getCondition());
		}
		return result;
	}

	@Override
	protected Collection<?> getContent() {
		return (isEmpty()) ? Collections.emptyList() : getConditions();
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	private int getLength() {
		return this.requestConditions.length;
	}

	/**
	 * 如果一个实例为空, 则返回另一个实例.
	 * 如果两个实例都有条件, 在确保它们具有相同类型和编号后合并各个条件.
	 */
	@Override
	public CompositeRequestCondition combine(CompositeRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		else {
			assertNumberOfConditions(other);
			RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
			for (int i = 0; i < getLength(); i++) {
				combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
			}
			return new CompositeRequestCondition(combinedConditions);
		}
	}

	private void assertNumberOfConditions(CompositeRequestCondition other) {
		Assert.isTrue(getLength() == other.getLength(),
				"Cannot combine CompositeRequestConditions with a different number of conditions. " +
				ObjectUtils.nullSafeToString(this.requestConditions) + " and  " +
				ObjectUtils.nullSafeToString(other.requestConditions));
	}

	/**
	 * 委托给<em>所有</em>包含的条件, 以匹配请求并返回生成的"匹配"条件实例.
	 * <p>空的{@code CompositeRequestCondition}匹配所有请求.
	 */
	@Override
	public CompositeRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (isEmpty()) {
			return this;
		}
		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
		for (int i = 0; i < getLength(); i++) {
			matchingConditions[i] = this.requestConditions[i].getMatchingCondition(request);
			if (matchingConditions[i] == null) {
				return null;
			}
		}
		return new CompositeRequestCondition(matchingConditions);
	}

	/**
	 * 如果一个实例为空, 则另一个实例"wins".
	 * 如果两个实例都有条件, 按照提供的顺序对它们进行比较.
	 */
	@Override
	public int compareTo(CompositeRequestCondition other, HttpServletRequest request) {
		if (isEmpty() && other.isEmpty()) {
			return 0;
		}
		else if (isEmpty()) {
			return 1;
		}
		else if (other.isEmpty()) {
			return -1;
		}
		else {
			assertNumberOfConditions(other);
			for (int i = 0; i < getLength(); i++) {
				int result = this.requestConditions[i].compareTo(other.requestConditions[i], request);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
