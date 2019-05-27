package org.springframework.util.comparator;

import java.util.Comparator;

import org.springframework.util.Assert;

/**
 * 比较器, 可以安全地比较空值低于或高于其他对象.
 * 可以装饰给定的比较器, 或处理Comparable.
 */
public class NullSafeComparator<T> implements Comparator<T> {

	/**
	 * 此比较器的共享默认实例, 将 null视为低于非null对象.
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<Object>(true);

	/**
	 * 此比较器的共享默认实例, 将 null视为高于非null对象.
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_HIGH = new NullSafeComparator<Object>(false);

	private final Comparator<T> nonNullComparator;

	private final boolean nullsLow;


	/**
	 * 创建一个NullSafeComparator, 根据提供的标志对{@code null}进行排序, 处理 Comparable.
	 * <p>比较两个非null对象时, 将使用它们的Comparable实现:
	 * 这意味着非null元素 (将应用此Comparator) 需要实现Comparable.
	 * <p>为方便起见, 可以使用默认的共享实例:
	 * {@code NullSafeComparator.NULLS_LOW} 和 {@code NullSafeComparator.NULLS_HIGH}.
	 * 
	 * @param nullsLow 是否将null值视为低于或高于非null对象
	 */
	@SuppressWarnings({ "unchecked", "rawtypes"})
	private NullSafeComparator(boolean nullsLow) {
		this.nonNullComparator = new ComparableComparator();
		this.nullsLow = nullsLow;
	}

	/**
	 * 创建一个NullSafeComparator, 根据提供的标志对{@code null}进行排序, 装饰给定的Comparator.
	 * <p>比较两个非null对象时, 将使用指定的Comparator.
	 * 给定的底层Comparator必须能够处理此Comparator将应用于的元素.
	 * 
	 * @param comparator 比较两个非null对象时使用的比较器
	 * @param nullsLow 是否将null值视为低于或高于非null对象
	 */
	public NullSafeComparator(Comparator<T> comparator, boolean nullsLow) {
		Assert.notNull(comparator, "The non-null comparator is required");
		this.nonNullComparator = comparator;
		this.nullsLow = nullsLow;
	}


	@Override
	public int compare(T o1, T o2) {
		if (o1 == o2) {
			return 0;
		}
		if (o1 == null) {
			return (this.nullsLow ? -1 : 1);
		}
		if (o2 == null) {
			return (this.nullsLow ? 1 : -1);
		}
		return this.nonNullComparator.compare(o1, o2);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NullSafeComparator)) {
			return false;
		}
		NullSafeComparator<T> other = (NullSafeComparator<T>) obj;
		return (this.nonNullComparator.equals(other.nonNullComparator) && this.nullsLow == other.nullsLow);
	}

	@Override
	public int hashCode() {
		return (this.nullsLow ? -1 : 1) * this.nonNullComparator.hashCode();
	}

	@Override
	public String toString() {
		return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " +
				(this.nullsLow ? "nulls low" : "nulls high");
	}

}
