package org.springframework.util.comparator;

import java.io.Serializable;
import java.util.Comparator;

import org.springframework.util.Assert;

/**
 * 比较器的装饰器, 带有"升序"标志, 表示比较结果是应该以正向 (标准升序) 顺序处理还是翻转为反向 (降序)顺序.
 */
@SuppressWarnings("serial")
public class InvertibleComparator<T> implements Comparator<T>, Serializable {

	private final Comparator<T> comparator;

	private boolean ascending = true;


	/**
	 * 创建一个InvertibleComparator, 默认情况下对升序进行排序.
	 * 对于实际比较, 将使用指定的Comparator.
	 * 
	 * @param comparator 要装饰的比较器
	 */
	public InvertibleComparator(Comparator<T> comparator) {
		Assert.notNull(comparator, "Comparator must not be null");
		this.comparator = comparator;
	}

	/**
	 * 创建一个InvertibleComparator, 根据提供的顺序进行排序.
	 * 对于实际比较, 将使用指定的Comparator.
	 * 
	 * @param comparator 要装饰的比较器
	 * @param ascending 排序顺序: 升序 (true) 或降序 (false)
	 */
	public InvertibleComparator(Comparator<T> comparator, boolean ascending) {
		Assert.notNull(comparator, "Comparator must not be null");
		this.comparator = comparator;
		setAscending(ascending);
	}


	/**
	 * 指定排序顺序: 升序 (true) 或降序 (false).
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * 返回排序顺序: 升序 (true) 或降序 (false).
	 */
	public boolean isAscending() {
		return this.ascending;
	}

	/**
	 * 反转排序顺序: 升序 -> 降序 or 降序 -> 升序.
	 */
	public void invertOrder() {
		this.ascending = !this.ascending;
	}


	@Override
	public int compare(T o1, T o2) {
		int result = this.comparator.compare(o1, o2);
		if (result != 0) {
			// 如果是反向排序, 则反转顺序.
			if (!this.ascending) {
				if (Integer.MIN_VALUE == result) {
					result = Integer.MAX_VALUE;
				}
				else {
					result *= -1;
				}
			}
			return result;
		}
		return 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof InvertibleComparator)) {
			return false;
		}
		InvertibleComparator<T> other = (InvertibleComparator<T>) obj;
		return (this.comparator.equals(other.comparator) && this.ascending == other.ascending);
	}

	@Override
	public int hashCode() {
		return this.comparator.hashCode();
	}

	@Override
	public String toString() {
		return "InvertibleComparator: [" + this.comparator + "]; ascending=" + this.ascending;
	}
}
