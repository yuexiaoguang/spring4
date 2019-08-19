package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Iterator;

/**
 * {@link RequestCondition}类型的基类, 提供{@link #equals(Object)}, {@link #hashCode()}, 和{@link #toString()}的实现.
 */
public abstract class AbstractRequestCondition<T extends AbstractRequestCondition<T>> implements RequestCondition<T> {

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && getClass() == obj.getClass()) {
			AbstractRequestCondition<?> other = (AbstractRequestCondition<?>) obj;
			return getContent().equals(other.getContent());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getContent().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		for (Iterator<?> iterator = getContent().iterator(); iterator.hasNext();) {
			Object expression = iterator.next();
			builder.append(expression.toString());
			if (iterator.hasNext()) {
				builder.append(getToStringInfix());
			}
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * 指示此条件是否为空, i.e. 它是否包含任何离散项.
	 * 
	 * @return {@code true} 如果为空; 否则{@code false}
	 */
	public boolean isEmpty() {
		return getContent().isEmpty();
	}


	/**
	 * 返回由请求条件组成的离散项.
	 * <p>例如URL模式, HTTP请求方法, param表达式等.
	 * 
	 * @return 对象集合, 不能是{@code null}
	 */
	protected abstract Collection<?> getContent();

	/**
	 * 打印离散内容项时使用的表示法.
	 * <p>例如, {@code " || "} 表示URL模式, 或{@code " && "}表示param表达式.
	 */
	protected abstract String getToStringInfix();

}
