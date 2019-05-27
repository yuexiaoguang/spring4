package org.springframework.messaging.handler;

import org.springframework.messaging.Message;

/**
 * 将条件映射到消息的约定.
 *
 * <p>消息条件可以组合 (e.g. 类型 + 方法级条件), 与特定消息匹配, 以及在消息的上下文中相互比较以确定哪一个与请求更紧密地匹配.
 *
 * @param <T> 该条件可以与之组合或比较的条件的类型
 */
public interface MessageCondition<T> {

	/**
	 * 定义将此条件与另一个条件相结合的规则.
	 * 例如, 组合类型和方法级别的条件.
	 * 
	 * @param other 要组合的条件
	 * 
	 * @return 结果消息条件
	 */
	T combine(T other);

	/**
	 * 检查此条件是否与给定消息匹配, 并返回可能具有针对当前消息定制的内容的新条件.
	 * 例如, 具有目标模式的条件可能仅返回具有已排序匹配模式的新条件.
	 * 
	 * @return 匹配时的条件实例; 如果没有匹配, 则为{@code null}.
	 */
	T getMatchingCondition(Message<?> message);

	/**
	 * 在特定消息的上下文中将此条件与另一个条件进行比较.
	 * 假设两个实例都是通过{@link #getMatchingCondition(Message)}获得的, 以确保它们只有与当前消息相关的内容.
	 */
	int compareTo(T other, Message<?> message);

}
