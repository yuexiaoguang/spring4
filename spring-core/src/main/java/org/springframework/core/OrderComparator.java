package org.springframework.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.ObjectUtils;

/**
 * {@link Ordered}对象的{@link Comparator}实现, 按顺序值升序排序, 分别按优先级降序排序.
 *
 * <h3>Same Order Objects</h3>
 * <p>具有相同顺序值的对象将相对于具有相同顺序值的其他对象以任意顺序排序.
 *
 * <h3>Non-ordered Objects</h3>
 * <p>任何不提供自己的顺序值的对象都会被隐式赋值为{@link Ordered#LOWEST_PRECEDENCE},
 * 因此, 相对于具有相同顺序值的其他对象, 以任意顺序结束于已排序集合的末尾.
 */
public class OrderComparator implements Comparator<Object> {

	/**
	 * {@code OrderComparator}的共享默认实例.
	 */
	public static final OrderComparator INSTANCE = new OrderComparator();


	/**
	 * 使用给定的源提供器构建适配的顺序比较器.
	 * 
	 * @param sourceProvider 要使用的顺序源提供器
	 * 
	 * @return 已适配的比较器
	 */
	public Comparator<Object> withSourceProvider(final OrderSourceProvider sourceProvider) {
		return new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return doCompare(o1, o2, sourceProvider);
			}
		};
	}

	@Override
	public int compare(Object o1, Object o2) {
		return doCompare(o1, o2, null);
	}

	private int doCompare(Object o1, Object o2, OrderSourceProvider sourceProvider) {
		boolean p1 = (o1 instanceof PriorityOrdered);
		boolean p2 = (o2 instanceof PriorityOrdered);
		if (p1 && !p2) {
			return -1;
		}
		else if (p2 && !p1) {
			return 1;
		}

		// 直接评估而不是 Integer.compareTo 以避免不必要的对象创建.
		int i1 = getOrder(o1, sourceProvider);
		int i2 = getOrder(o2, sourceProvider);
		return (i1 < i2) ? -1 : (i1 > i2) ? 1 : 0;
	}

	/**
	 * 确定给定对象的顺序值.
	 * <p>默认实现使用{@link #findOrder}检查给定的{@link OrderSourceProvider}, 并回退到常规{@link #getOrder(Object)}调用.
	 * 
	 * @param obj 要检查的对象
	 * 
	 * @return 顺序值, 或{@code Ordered.LOWEST_PRECEDENCE}作为回退
	 */
	private int getOrder(Object obj, OrderSourceProvider sourceProvider) {
		Integer order = null;
		if (sourceProvider != null) {
			Object orderSource = sourceProvider.getOrderSource(obj);
			if (orderSource != null && orderSource.getClass().isArray()) {
				Object[] sources = ObjectUtils.toObjectArray(orderSource);
				for (Object source : sources) {
					order = findOrder(source);
					if (order != null) {
						break;
					}
				}
			}
			else {
				order = findOrder(orderSource);
			}
		}
		return (order != null ? order : getOrder(obj));
	}

	/**
	 * 确定给定对象的顺序值.
	 * <p>默认实现通过委派给{@link #findOrder}来检查{@link Ordered}接口. 可以在子类中重写.
	 * 
	 * @param obj 要检查的对象
	 * 
	 * @return 顺序值, 或{@code Ordered.LOWEST_PRECEDENCE}作为回退
	 */
	protected int getOrder(Object obj) {
		Integer order = findOrder(obj);
		return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
	}

	/**
	 * 查找给定对象指示的顺序值.
	 * <p>默认实现检查{@link Ordered}接口.
	 * 可以在子类中重写.
	 * 
	 * @param obj 要检查的对象
	 * 
	 * @return 顺序值, 或{@code null}
	 */
	protected Integer findOrder(Object obj) {
		return (obj instanceof Ordered ? ((Ordered) obj).getOrder() : null);
	}

	/**
	 * 确定给定对象的优先级值.
	 * <p>默认实现始终返回{@code null}.
	 * 除了它们的'顺序'语义之外, 子类可以重写它以使特定类型的值成为'优先级'特征.
	 * 优先级表示除了用于列表/数组中的排序目的之外, 它还可以用于选择一个对象而不是另一个对象.
	 * 
	 * @param obj 要检查的对象
	 * 
	 * @return 优先级值, 或{@code null}
	 */
	public Integer getPriority(Object obj) {
		return null;
	}


	/**
	 * 使用默认的OrderComparator对给定的List进行排序.
	 * <p>优化为跳过排序大小为0或1的列表, 以避免不必要的数组提取.
	 * 
	 * @param list 要排序的列表
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			Collections.sort(list, INSTANCE);
		}
	}

	/**
	 * 使用默认的OrderComparator对给定数组进行排序.
	 * <p>优化为跳过排序大小为0或1的列表, 以避免不必要的数组提取.
	 * 
	 * @param array 要排序的数组
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * 使用默认的OrderComparator对给定的数组或List进行排序. 在给定任何其他值时, 只需跳过排序.
	 * <p>优化为跳过排序大小为0或1的列表, 以避免不必要的数组提取.
	 * 
	 * @param value 要排序的数组或列表
	 */
	public static void sortIfNecessary(Object value) {
		if (value instanceof Object[]) {
			sort((Object[]) value);
		}
		else if (value instanceof List) {
			sort((List<?>) value);
		}
	}


	/**
	 * 策略接口, 为给定对象提供顺序源.
	 */
	public interface OrderSourceProvider {

		/**
		 * 返回指定对象的顺序源, i.e. 应检查顺序值以替换给定对象的对象.
		 * <p>也可以是顺序源对象的数组.
		 * <p>如果返回的对象未指示任何顺序, 则比较器将回退以检查原始对象.
		 * 
		 * @param obj 要查找顺序源的对象
		 * 
		 * @return 该对象的顺序源, 或{@code null}
		 */
		Object getOrderSource(Object obj);
	}

}
