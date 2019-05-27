package org.springframework.util.comparator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * 比较器, 链接一个或多个比较器的序列.
 *
 * <p>复合比较器按顺序调用每个比较器, 直到单个比较器返回非零结果, 或者比较器耗尽并返回零.
 *
 * <p>这有助于内存排序, 类似于SQL中的多列排序.
 * 列表中任何单个比较器的顺序也可以颠倒.
 */
@SuppressWarnings({"serial", "rawtypes"})
public class CompoundComparator<T> implements Comparator<T>, Serializable {

	private final List<InvertibleComparator> comparators;


	/**
	 * 构造一个最初没有Comparators的CompoundComparator.
	 * 客户端必须在调用compare方法之前添加至少一个Comparator, 否则抛出IllegalStateException.
	 */
	public CompoundComparator() {
		this.comparators = new ArrayList<InvertibleComparator>();
	}

	/**
	 * 从提供的数组中的Comparators构造一个CompoundComparator.
	 * <p>除非它们是InvertibleComparators, 否则所有比较器都将默认为升序排序.
	 * 
	 * @param comparators 要构建成复合比较器的比较器
	 */
	@SuppressWarnings("unchecked")
	public CompoundComparator(Comparator... comparators) {
		Assert.notNull(comparators, "Comparators must not be null");
		this.comparators = new ArrayList<InvertibleComparator>(comparators.length);
		for (Comparator comparator : comparators) {
			addComparator(comparator);
		}
	}


	/**
	 * 将比较器添加到链的末尾.
	 * <p>除非是InvertibleComparator, 否则Comparator将默认为升序排序.
	 * 
	 * @param comparator 将比较器添加到链的末尾
	 */
	@SuppressWarnings("unchecked")
	public void addComparator(Comparator<? extends T> comparator) {
		if (comparator instanceof InvertibleComparator) {
			this.comparators.add((InvertibleComparator) comparator);
		}
		else {
			this.comparators.add(new InvertibleComparator(comparator));
		}
	}

	/**
	 * 使用提供的排序顺序将比较器添加到链的末尾.
	 * 
	 * @param comparator 要添加到链的末尾的比较器
	 * @param ascending 排序顺序: 升序 (true) 或降序 (false)
	 */
	@SuppressWarnings("unchecked")
	public void addComparator(Comparator<? extends T> comparator, boolean ascending) {
		this.comparators.add(new InvertibleComparator(comparator, ascending));
	}

	/**
	 * 替换给定索引处的比较器.
	 * <p>除非是InvertibleComparator, 否则Comparator将默认为升序排序.
	 * 
	 * @param index 要替换的比较器的索引
	 * @param comparator 要放在给定的索引处的比较器
	 */
	@SuppressWarnings("unchecked")
	public void setComparator(int index, Comparator<? extends T> comparator) {
		if (comparator instanceof InvertibleComparator) {
			this.comparators.set(index, (InvertibleComparator) comparator);
		}
		else {
			this.comparators.set(index, new InvertibleComparator(comparator));
		}
	}

	/**
	 * 使用给定的排序顺序替换给定索引处的Comparator.
	 * 
	 * @param index 要替换的比较器的索引
	 * @param comparator 要放在给定的索引处的比较器
	 * @param ascending 排序顺序: 升序 (true) 或降序 (false)
	 */
	public void setComparator(int index, Comparator<T> comparator, boolean ascending) {
		this.comparators.set(index, new InvertibleComparator<T>(comparator, ascending));
	}

	/**
	 * 反转此复合比较器包含的每个排序定义的排序顺序.
	 */
	public void invertOrder() {
		for (InvertibleComparator comparator : this.comparators) {
			comparator.invertOrder();
		}
	}

	/**
	 * 反转指定索引处的排序定义的排序顺序.
	 * 
	 * @param index 要反转的比较器的索引
	 */
	public void invertOrder(int index) {
		this.comparators.get(index).invertOrder();
	}

	/**
	 * 将给定索引处的排序顺序更改为升序.
	 * 
	 * @param index 要改变的比较器的索引
	 */
	public void setAscendingOrder(int index) {
		this.comparators.get(index).setAscending(true);
	}

	/**
	 * 将给定索引处的排序顺序更改为降序排序.
	 * 
	 * @param index 要改变的比较器的索引
	 */
	public void setDescendingOrder(int index) {
		this.comparators.get(index).setAscending(false);
	}

	/**
	 * 返回聚合比较器的数量.
	 */
	public int getComparatorCount() {
		return this.comparators.size();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) {
		Assert.state(this.comparators.size() > 0,
				"No sort definitions have been added to this CompoundComparator to compare");
		for (InvertibleComparator comparator : this.comparators) {
			int result = comparator.compare(o1, o2);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CompoundComparator)) {
			return false;
		}
		CompoundComparator<T> other = (CompoundComparator<T>) obj;
		return this.comparators.equals(other.comparators);
	}

	@Override
	public int hashCode() {
		return this.comparators.hashCode();
	}

	@Override
	public String toString() {
		return "CompoundComparator: " + this.comparators;
	}
}
