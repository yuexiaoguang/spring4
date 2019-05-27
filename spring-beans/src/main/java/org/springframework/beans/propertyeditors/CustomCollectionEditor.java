package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Collection的属性编辑器, 将任何源Collection转换为给定的目标 Collection类型.
 *
 * <p>默认情况下, 为 Set, SortedSet, List注册, 如果类型与target属性不匹配, 则自动将任何给定Collection转换为其中一个目标类型.
 */
public class CustomCollectionEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Collection> collectionType;

	private final boolean nullAsEmptyCollection;


	/**
	 * 为给定的目标类型创建一个新的CustomCollectionEditor, 保持传入的 {@code null}原样.
	 * 
	 * @param collectionType 目标类型, 需要是Collection的子接口或具体的Collection类
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType) {
		this(collectionType, false);
	}

	/**
	 * 为给定的目标类型创建一个新的CustomCollectionEditor.
	 * <p>如果传入值是给定的类型, 它将按原样使用.
	 * 如果它是不同的Collection类型或数组, 它将转换为给定Collection类型的默认实现.
	 * 如果该值是其他任何值, 则将创建具有该单个值的目标Collection.
	 * <p>默认的Collection实现是: ArrayList for List, TreeSet for SortedSet, LinkedHashSet for Set.
	 * 
	 * @param collectionType 目标类型, 需要是Collection的子接口或具体的Collection类
	 * @param nullAsEmptyCollection 是否将传入的{@code null}值转换为空 Collection (适当类型)
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection) {
		if (collectionType == null) {
			throw new IllegalArgumentException("Collection type is required");
		}
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException(
					"Collection type [" + collectionType.getName() + "] does not implement [java.util.Collection]");
		}
		this.collectionType = collectionType;
		this.nullAsEmptyCollection = nullAsEmptyCollection;
	}


	/**
	 * 将给定的文本值转换为具有单个元素的Collection.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * 将给定的值转换为目标类型的Collection.
	 */
	@Override
	public void setValue(Object value) {
		if (value == null && this.nullAsEmptyCollection) {
			super.setValue(createCollection(this.collectionType, 0));
		}
		else if (value == null || (this.collectionType.isInstance(value) && !alwaysCreateNewCollection())) {
			// 按原样使用源值, 因为它与目标类型匹配.
			super.setValue(value);
		}
		else if (value instanceof Collection) {
			// Convert Collection elements.
			Collection<?> source = (Collection<?>) value;
			Collection<Object> target = createCollection(this.collectionType, source.size());
			for (Object elem : source) {
				target.add(convertElement(elem));
			}
			super.setValue(target);
		}
		else if (value.getClass().isArray()) {
			// Convert array elements to Collection elements.
			int length = Array.getLength(value);
			Collection<Object> target = createCollection(this.collectionType, length);
			for (int i = 0; i < length; i++) {
				target.add(convertElement(Array.get(value, i)));
			}
			super.setValue(target);
		}
		else {
			// 一个普通的值: 将其转换为具有单个元素的Collection.
			Collection<Object> target = createCollection(this.collectionType, 1);
			target.add(convertElement(value));
			super.setValue(target);
		}
	}

	/**
	 * 使用给定的初始容量, 创建给定类型的集合 (如果Collection类型支持).
	 * 
	 * @param collectionType Collection的子接口
	 * @param initialCapacity 初始容量
	 * 
	 * @return 新的Collection实例
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
		if (!collectionType.isInterface()) {
			try {
				return collectionType.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate collection class: " + collectionType.getName(), ex);
			}
		}
		else if (List.class == collectionType) {
			return new ArrayList<Object>(initialCapacity);
		}
		else if (SortedSet.class == collectionType) {
			return new TreeSet<Object>();
		}
		else {
			return new LinkedHashSet<Object>(initialCapacity);
		}
	}

	/**
	 * 返回是否始终创建新Collection, 即使传入的Collection的类型已匹配.
	 * <p>默认是 "false"; 可以重写以强制创建新Collection, 例如在任何情况下都转换元素.
	 */
	protected boolean alwaysCreateNewCollection() {
		return false;
	}

	/**
	 * 转换每个遇到的Collection/数组元素的钩子.
	 * 默认实现只是按原样返回传入的元素.
	 * <p>可以重写以执行某些元素的转换, 例如String到Integer.
	 * <p>仅在实际创建新Collection时调用!
	 * 如果传入的Collection的类型已匹配, 则默认情况下不是这种情况.
	 * 重写{@link #alwaysCreateNewCollection()} 在每种情况下强制创建一个新的Collection.
	 * 
	 * @param element 源元素
	 * 
	 * @return 要在目标Collection中使用的元素
	 */
	protected Object convertElement(Object element) {
		return element;
	}


	/**
	 * 此实现返回{@code null}以指示没有适当的文本表示.
	 */
	@Override
	public String getAsText() {
		return null;
	}

}
