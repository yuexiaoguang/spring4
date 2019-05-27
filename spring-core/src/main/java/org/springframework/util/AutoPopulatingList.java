package org.springframework.util;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * 简单的{@link List}包装类, 允许在请求时自动填充元素.
 * 这对于绑定到{@link List Lists}的数据特别有用, 允许以"及时"方式创建元素并将其添加到{@link List}.
 *
 * <p>Note: 这个类不是线程安全的. 要创建线程安全版本, 使用{@link java.util.Collections#synchronizedList}工具方法.
 *
 * <p>灵感来自Commons Collections的{@code LazyList}.
 */
@SuppressWarnings("serial")
public class AutoPopulatingList<E> implements List<E>, Serializable {

	/**
	 * 所有操作最终委派给的{@link List}.
	 */
	private final List<E> backingList;

	/**
	 * 用于按需创建新的{@link List}元素的{@link ElementFactory}.
	 */
	private final ElementFactory<E> elementFactory;


	/**
	 * 创建一个由标准{@link ArrayList}支持的新{@code AutoPopulatingList},
	 * 并根据需要将所提供的{@link Class element Class}的新实例添加到支持{@link List}中.
	 */
	public AutoPopulatingList(Class<? extends E> elementClass) {
		this(new ArrayList<E>(), elementClass);
	}

	/**
	 * 创建一个由提供的{@link List}支持的新{@code AutoPopulatingList},
	 * 并根据需要将提供的{@link Class element Class}的新实例添加到支持{@link List}中.
	 */
	public AutoPopulatingList(List<E> backingList, Class<? extends E> elementClass) {
		this(backingList, new ReflectiveElementFactory<E>(elementClass));
	}

	/**
	 * 创建一个由标准{@link ArrayList}支持的新{@code AutoPopulatingList},
	 * 并使用提供的{@link ElementFactory}按需创建新元素.
	 */
	public AutoPopulatingList(ElementFactory<E> elementFactory) {
		this(new ArrayList<E>(), elementFactory);
	}

	/**
	 * 创建一个由提供的{@link List}支持的新{@code AutoPopulatingList},
	 * 并使用提供的{@link ElementFactory}按需创建新元素.
	 */
	public AutoPopulatingList(List<E> backingList, ElementFactory<E> elementFactory) {
		Assert.notNull(backingList, "Backing List must not be null");
		Assert.notNull(elementFactory, "Element factory must not be null");
		this.backingList = backingList;
		this.elementFactory = elementFactory;
	}


	@Override
	public void add(int index, E element) {
		this.backingList.add(index, element);
	}

	@Override
	public boolean add(E o) {
		return this.backingList.add(o);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return this.backingList.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return this.backingList.addAll(index, c);
	}

	@Override
	public void clear() {
		this.backingList.clear();
	}

	@Override
	public boolean contains(Object o) {
		return this.backingList.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.backingList.containsAll(c);
	}

	/**
	 * 获取提供的索引处的元素; 如果该索引处没有元素, 则创建它.
	 */
	@Override
	public E get(int index) {
		int backingListSize = this.backingList.size();
		E element = null;
		if (index < backingListSize) {
			element = this.backingList.get(index);
			if (element == null) {
				element = this.elementFactory.createElement(index);
				this.backingList.set(index, element);
			}
		}
		else {
			for (int x = backingListSize; x < index; x++) {
				this.backingList.add(null);
			}
			element = this.elementFactory.createElement(index);
			this.backingList.add(element);
		}
		return element;
	}

	@Override
	public int indexOf(Object o) {
		return this.backingList.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return this.backingList.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.backingList.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.backingList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return this.backingList.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return this.backingList.listIterator(index);
	}

	@Override
	public E remove(int index) {
		return this.backingList.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		return this.backingList.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.backingList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.backingList.retainAll(c);
	}

	@Override
	public E set(int index, E element) {
		return this.backingList.set(index, element);
	}

	@Override
	public int size() {
		return this.backingList.size();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return this.backingList.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return this.backingList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.backingList.toArray(a);
	}


	@Override
	public boolean equals(Object other) {
		return this.backingList.equals(other);
	}

	@Override
	public int hashCode() {
		return this.backingList.hashCode();
	}


	/**
	 * 用于为基于索引的访问数据结构创建元素的工厂接口, 例如{@link java.util.List}.
	 */
	public interface ElementFactory<E> {

		/**
		 * 为提供的索引创建元素.
		 * 
		 * @return 元素对象
		 * @throws ElementInstantiationException 如果实例化过程失败
		 * (目标构造函数抛出的任何异常都应该按原样传播)
		 */
		E createElement(int index) throws ElementInstantiationException;
	}


	/**
	 * 从ElementFactory抛出的异常.
	 */
	public static class ElementInstantiationException extends RuntimeException {

		public ElementInstantiationException(String msg) {
			super(msg);
		}

		public ElementInstantiationException(String message, Throwable cause) {
			super(message, cause);
		}
	}


	/**
	 * ElementFactory接口的反射实现, 使用给定的元素类上的{@code Class.newInstance()}.
	 */
	private static class ReflectiveElementFactory<E> implements ElementFactory<E>, Serializable {

		private final Class<? extends E> elementClass;

		public ReflectiveElementFactory(Class<? extends E> elementClass) {
			Assert.notNull(elementClass, "Element class must not be null");
			Assert.isTrue(!elementClass.isInterface(), "Element class must not be an interface type");
			Assert.isTrue(!Modifier.isAbstract(elementClass.getModifiers()), "Element class cannot be an abstract class");
			this.elementClass = elementClass;
		}

		@Override
		public E createElement(int index) {
			try {
				return this.elementClass.newInstance();
			}
			catch (InstantiationException ex) {
				throw new ElementInstantiationException(
						"Unable to instantiate element class: " + this.elementClass.getName(), ex);
			}
			catch (IllegalAccessException ex) {
				throw new ElementInstantiationException(
						"Could not access element constructor: " + this.elementClass.getName(), ex);
			}
		}
	}
}
