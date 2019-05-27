package org.springframework.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 复合迭代器, 它结合了多个其他迭代器, 通过{@link #add(Iterator)}注册.
 *
 * <p>此实现维护一组迭代器, 这些迭代器按顺序调用, 直到所有迭代器都用完为止.
 */
public class CompositeIterator<E> implements Iterator<E> {

	private final Set<Iterator<E>> iterators = new LinkedHashSet<Iterator<E>>();

	private boolean inUse = false;


	/**
	 * 将给定迭代器添加到此组合中.
	 */
	public void add(Iterator<E> iterator) {
		Assert.state(!this.inUse, "You can no longer add iterators to a composite iterator that's already in use");
		if (this.iterators.contains(iterator)) {
			throw new IllegalArgumentException("You cannot add the same iterator twice");
		}
		this.iterators.add(iterator);
	}

	@Override
	public boolean hasNext() {
		this.inUse = true;
		for (Iterator<E> iterator : this.iterators) {
			if (iterator.hasNext()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public E next() {
		this.inUse = true;
		for (Iterator<E> iterator : this.iterators) {
			if (iterator.hasNext()) {
				return iterator.next();
			}
		}
		throw new NoSuchElementException("All iterators exhausted");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("CompositeIterator does not support remove()");
	}

}
