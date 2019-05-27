package org.springframework.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 适用于了解Java 5, Java 6和Spring集合类型的集合的工厂.
 *
 * <p>主要供框架内部使用.
 */
public abstract class CollectionFactory {

	private static final Set<Class<?>> approximableCollectionTypes = new HashSet<Class<?>>();

	private static final Set<Class<?>> approximableMapTypes = new HashSet<Class<?>>();


	static {
		// 标准集合接口
		approximableCollectionTypes.add(Collection.class);
		approximableCollectionTypes.add(List.class);
		approximableCollectionTypes.add(Set.class);
		approximableCollectionTypes.add(SortedSet.class);
		approximableCollectionTypes.add(NavigableSet.class);
		approximableMapTypes.add(Map.class);
		approximableMapTypes.add(SortedMap.class);
		approximableMapTypes.add(NavigableMap.class);

		// 常见的具体集合类
		approximableCollectionTypes.add(ArrayList.class);
		approximableCollectionTypes.add(LinkedList.class);
		approximableCollectionTypes.add(HashSet.class);
		approximableCollectionTypes.add(LinkedHashSet.class);
		approximableCollectionTypes.add(TreeSet.class);
		approximableCollectionTypes.add(EnumSet.class);
		approximableMapTypes.add(HashMap.class);
		approximableMapTypes.add(LinkedHashMap.class);
		approximableMapTypes.add(TreeMap.class);
		approximableMapTypes.add(EnumMap.class);
	}


	/**
	 * 确定给定的集合类型是否为<em>近似</em>类型,
	 * i.e. {@link #createApproximateCollection}可以近似的类型.
	 * 
	 * @param collectionType 要检查的集合类型
	 * 
	 * @return {@code true}如果类型是<em>近似</em>
	 */
	public static boolean isApproximableCollectionType(Class<?> collectionType) {
		return (collectionType != null && approximableCollectionTypes.contains(collectionType));
	}

	/**
	 * 为给定集合创建最近似的集合.
	 * <p><strong>Warning</strong>: 由于参数化类型{@code E}未绑定到提供的{@code collection}中包含的元素类型,
	 * 因此如果提供的{@code collection}是{@link EnumSet}, 则无法保证类型安全性.
	 * 在这种情况下, 调用者负责确保提供的{@code collection}的元素类型是匹配类型{@code E}的枚举类型.
	 * 作为替代方案, 调用者可能希望将返回值视为{@link Object}的原始集合或集合.
	 * 
	 * @param collection 原始集合对象, 可能{@code null}
	 * @param capacity 初始容量
	 * 
	 * @return 一个新的空集合实例
	 */
	@SuppressWarnings({ "unchecked", "cast", "rawtypes" })
	public static <E> Collection<E> createApproximateCollection(Object collection, int capacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList<E>();
		}
		else if (collection instanceof List) {
			return new ArrayList<E>(capacity);
		}
		else if (collection instanceof EnumSet) {
			// 在Eclipse 4.4.1中编译是必需的.
			Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
			enumSet.clear();
			return enumSet;
		}
		else if (collection instanceof SortedSet) {
			return new TreeSet<E>(((SortedSet<E>) collection).comparator());
		}
		else {
			return new LinkedHashSet<E>(capacity);
		}
	}

	/**
	 * 为给定的集合类型创建最合适的集合.
	 * <p>使用{@code null}元素类型委托给{@link #createCollection(Class, Class, int)}.
	 * 
	 * @param collectionType 目标集合的所需类型; never {@code null}
	 * @param capacity 初始容量
	 * 
	 * @return 一个新的集合实例
	 * @throws IllegalArgumentException 如果提供的{@code collectionType}是{@code null}或类型为{@link EnumSet}
	 */
	public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
		return createCollection(collectionType, null, capacity);
	}

	/**
	 * 为给定的集合类型创建最合适的集合.
	 * <p><strong>Warning</strong>: 由于参数化类型{@code E}未绑定到提供的{@code collection}中包含的元素类型,
	 * 因此如果提供的{@code collection}是{@link EnumSet}, 则无法保证类型安全性.
	 * 在这种情况下, 调用者负责确保提供的{@code collection}的元素类型是匹配类型{@code E}的枚举类型.
	 * 作为替代方案, 调用者可能希望将返回值视为{@link Object}的原始集合或集合.
	 * 
	 * @param collectionType 目标集合的所需类型; never {@code null}
	 * @param elementType 集合的元素类型, 或{@code null} (note: 仅与{@link EnumSet}创建相关)
	 * @param capacity 初始容量
	 * 
	 * @return 一个新的集合实例
	 * @throws IllegalArgumentException 如果提供的{@code collectionType}是{@code null};
	 * 或者, 如果所需的{@code collectionType}是{@link EnumSet}, 并且提供的{@code elementType}不是{@link Enum}的子类型
	 */
	@SuppressWarnings({ "unchecked", "cast" })
	public static <E> Collection<E> createCollection(Class<?> collectionType, Class<?> elementType, int capacity) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (collectionType.isInterface()) {
			if (Set.class == collectionType || Collection.class == collectionType) {
				return new LinkedHashSet<E>(capacity);
			}
			else if (List.class == collectionType) {
				return new ArrayList<E>(capacity);
			}
			else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
				return new TreeSet<E>();
			}
			else {
				throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
			}
		}
		else if (EnumSet.class == collectionType) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			// 在Eclipse 4.4.1中编译是必需的.
			return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
		}
		else {
			if (!Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
			}
			try {
				return (Collection<E>) collectionType.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
					"Could not instantiate Collection type: " + collectionType.getName(), ex);
			}
		}
	}

	/**
	 * 确定给定的Map类型是否为<em>近似</em>类型,
	 * i.e. {@link #createApproximateMap}可以近似的类型.
	 * 
	 * @param mapType 要检查的Map类型
	 * 
	 * @return {@code true}如果类型是<em>近似</em>
	 */
	public static boolean isApproximableMapType(Class<?> mapType) {
		return (mapType != null && approximableMapTypes.contains(mapType));
	}

	/**
	 * 为给定Map创建最近似的Map.
	 * <p><strong>Warning</strong>: 由于参数化类型{@code K}未绑定到提供的{@code map}中包含的键类型,
	 * 因此如果提供的{@code map}是{@link EnumMap}, 则无法保证类型安全性.
	 * 在这种情况下, 调用者负责确保提供的{@code map}中的键类型是匹配类型{@code K}的枚举类型.
	 * 作为替代方案, 调用者可能希望将返回值视为使用{@link Object}键的原始Map或Map.
	 * 
	 * @param map 原始Map对象, 可能{@code null}
	 * @param capacity 初始容量
	 * 
	 * @return 一个新的空Map实例
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Map<K, V> createApproximateMap(Object map, int capacity) {
		if (map instanceof EnumMap) {
			EnumMap enumMap = new EnumMap((EnumMap) map);
			enumMap.clear();
			return enumMap;
		}
		else if (map instanceof SortedMap) {
			return new TreeMap<K, V>(((SortedMap<K, V>) map).comparator());
		}
		else {
			return new LinkedHashMap<K, V>(capacity);
		}
	}

	/**
	 * 为给定的Map类型创建最合适的Map.
	 * <p>使用{@code null}键类型委托给{@link #createMap(Class, Class, int)}.
	 * 
	 * @param mapType 目标Map的所需类型
	 * @param capacity 初始容量
	 * 
	 * @return 一个新的Map实例
	 * @throws IllegalArgumentException 如果提供的{@code mapType}是{@code null}或类型为{@link EnumMap}
	 */
	public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
		return createMap(mapType, null, capacity);
	}

	/**
	 * 为给定的Map类型创建最合适的Map.
	 * <p><strong>Warning</strong>: 由于参数化类型{@code K}未绑定到提供的{@code keyType},
	 * 如果所需的{@code mapType}为{@link EnumMap}, 则无法保证类型安全性.
	 * 在这种情况下, 调用者负责确保{@code keyType}是匹配类型{@code K}的枚举类型.
	 * 作为替代方案, 调用者可能希望将返回值视为使用{@link Object}键的原始Map或Map.
	 * 同样, 如果所需的{@code mapType}是{@link MultiValueMap}, 则无法强制执行类型安全性.
	 * 
	 * @param mapType 目标Map的所需类型; never {@code null}
	 * @param keyType Map键的类型, 或{@code null} (note: 仅与{@link EnumMap}创建相关)
	 * @param capacity 初始容量
	 * 
	 * @return 一个新的Map实例
	 * @throws IllegalArgumentException 如果提供的{@code mapType}是{@code null};
	 * 或者, 如果所需的{@code mapType}是{@link EnumMap}, 并且提供的{@code keyType}不是{@link Enum}的子类型
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Map<K, V> createMap(Class<?> mapType, Class<?> keyType, int capacity) {
		Assert.notNull(mapType, "Map type must not be null");
		if (mapType.isInterface()) {
			if (Map.class == mapType) {
				return new LinkedHashMap<K, V>(capacity);
			}
			else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
				return new TreeMap<K, V>();
			}
			else if (MultiValueMap.class == mapType) {
				return new LinkedMultiValueMap();
			}
			else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
			}
		}
		else if (EnumMap.class == mapType) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return new EnumMap(asEnumType(keyType));
		}
		else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return (Map<K, V>) mapType.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
			}
		}
	}

	/**
	 * 创建{@code java.util.Properties}的变体, 自动将非String值应用于{@link Properties#getProperty}上的String表示.
	 * 
	 * @return 新的{@code Properties}实例
	 */
	@SuppressWarnings("serial")
	public static Properties createStringAdaptingProperties() {
		return new Properties() {
			@Override
			public String getProperty(String key) {
				Object value = get(key);
				return (value != null ? value.toString() : null);
			}
		};
	}

	/**
	 * 将给定类型转换为{@link Enum}的子类型.
	 * 
	 * @param enumType 枚举类型, never {@code null}
	 * 
	 * @return 给定类型为{@link Enum}的子类型
	 * @throws IllegalArgumentException 如果给定的类型不是{@link Enum}的子类型
	 */
	@SuppressWarnings("rawtypes")
	private static Class<? extends Enum> asEnumType(Class<?> enumType) {
		Assert.notNull(enumType, "Enum type must not be null");
		if (!Enum.class.isAssignableFrom(enumType)) {
			throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
		}
		return enumType.asSubclass(Enum.class);
	}

}
