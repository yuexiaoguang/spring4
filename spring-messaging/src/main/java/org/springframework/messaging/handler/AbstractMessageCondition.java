package org.springframework.messaging.handler;

import java.util.Collection;
import java.util.Iterator;

/**
 * {@link MessageCondition}类型的基类, 提供{@link #equals(Object)}, {@link #hashCode()}, 和 {@link #toString()}的实现.
 */
public abstract class AbstractMessageCondition<T extends AbstractMessageCondition<T>> implements MessageCondition<T> {

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && getClass() == obj.getClass()) {
			AbstractMessageCondition<?> other = (AbstractMessageCondition<?>) obj;
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
	 * 返回组成消息条件的对象集合 (e.g. 目标模式), never {@code null}.
	 */
	protected abstract Collection<?> getContent();

	/**
	 * 打印离散内容项时使用的表示法.
	 * 例如, " || " 表示URL模式, 或" && " 表示参数表达式.
	 */
	protected abstract String getToStringInfix();

}
