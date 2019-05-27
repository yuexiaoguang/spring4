package org.springframework.core;

/**
 * {@code Ordered}是一个应该<em>可排序</em>的对象实现的接口, 例如在{@code Collection}中.
 *
 * <p>实际的{@link #getOrder() order}可以解释为优先级, 第一个对象(具有最低的顺序值)具有最高优先级.
 *
 * <p>请注意, 此接口还有<em>priority</em>标记: {@link PriorityOrdered}.
 * {@code PriorityOrdered}对象表示的顺序值始终在<em>普通的</em>{@link Ordered}对象表示的相同顺序值之前应用.
 *
 * <p>有关非有序对象的排序语义的详细信息, 请参阅Javadoc以获取{@link OrderComparator}.
 */
public interface Ordered {

	/**
	 * 最高优先级值.
	 */
	int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	/**
	 * 最低优先级值.
	 */
	int LOWEST_PRECEDENCE = Integer.MAX_VALUE;


	/**
	 * 获取此对象的顺序值.
	 * <p>较高的值被解释为较低的优先级.
	 * 因此, 具有最低值的对象具有最高优先级(有点类似于Servlet {@code load-on-startup}值).
	 * <p>相同的顺序值将导致受影响的对象的任意排序位置.
	 * 
	 * @return 顺序值
	 */
	int getOrder();

}
