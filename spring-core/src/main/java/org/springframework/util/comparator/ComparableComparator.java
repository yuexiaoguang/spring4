package org.springframework.util.comparator;

import java.util.Comparator;

/**
 * 将Comparables适配为Comparator接口的比较器.
 * 主要用于其他Comparator的内部使用, 当应用于 Comparable时.
 */
public class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {

	@SuppressWarnings("rawtypes")
	public static final ComparableComparator INSTANCE = new ComparableComparator();

	@Override
	public int compare(T o1, T o2) {
		return o1.compareTo(o2);
	}

}
