package org.springframework.util.comparator;

import java.util.Comparator;

import org.springframework.util.Assert;

/**
 * 根据任意类顺序比较对象.
 * 允许基于它们继承的类的类型对对象进行排序, 例如:
 * 此比较器可用于对列表{@code Number}进行排序, 以便{@code Long}出现在{@code Integer}之前.
 *
 * <p>在比较期间仅考虑指定的{@code instanceOrder}类.
 * 如果两个对象都是有序类型的实例, 则此比较器将返回{@code 0}.
 * 如果需要额外的排序, 请考虑与{@link CompoundComparator}结合使用.
 *
 * @param <T> 被比较的对象的类型
 */
public class InstanceComparator<T> implements Comparator<T> {

	private final Class<?>[] instanceOrder;


	/**
	 * @param instanceOrder 比较对象时应使用的有序类列表.
	 * 列表中较早的类将被赋予更高的优先级.
	 */
	public InstanceComparator(Class<?>... instanceOrder) {
		Assert.notNull(instanceOrder, "'instanceOrder' must not be null");
		this.instanceOrder = instanceOrder;
	}


	@Override
	public int compare(T o1, T o2) {
		int i1 = getOrder(o1);
		int i2 = getOrder(o2);
		return (i1 < i2 ? -1 : (i1 == i2 ? 0 : 1));
	}

	private int getOrder(T object) {
		if (object != null) {
			for (int i = 0; i < this.instanceOrder.length; i++) {
				if (this.instanceOrder[i].isInstance(object)) {
					return i;
				}
			}
		}
		return this.instanceOrder.length;
	}

}
