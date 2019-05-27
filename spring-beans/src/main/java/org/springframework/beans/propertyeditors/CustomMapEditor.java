package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Map的属性编辑器, 将任何源Map转换为给定的目标Map类型.
 */
public class CustomMapEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Map> mapType;

	private final boolean nullAsEmptyMap;


	/**
	 * 为给定的目标类型创建一个新的CustomMapEditor, 保持传入的{@code null}原样.
	 * 
	 * @param mapType 目标类型, 需要是Map的子接口或具体的Map类
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType) {
		this(mapType, false);
	}

	/**
	 * 为给定的目标类型创建一个新的CustomMapEditor.
	 * <p>如果传入的值是给定的类型, 它将按原样使用.
	 * 如果它是不同的Map类型或数组, 它将转换为给定Map类型的默认实现.
	 * 如果该值是其他任何值, 则将创建具有单个值的目标Map.
	 * <p>默认Map 实现是: TreeMap for SortedMap, LinkedHashMap for Map.
	 * 
	 * @param mapType 目标类型, 需要是Map的子接口或具体的Map类
	 * @param nullAsEmptyMap 是否将传入的{@code null}值转换为空Map (相应类型)
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType, boolean nullAsEmptyMap) {
		if (mapType == null) {
			throw new IllegalArgumentException("Map type is required");
		}
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException(
					"Map type [" + mapType.getName() + "] does not implement [java.util.Map]");
		}
		this.mapType = mapType;
		this.nullAsEmptyMap = nullAsEmptyMap;
	}


	/**
	 * 将给定的文本值转换为具有单个元素的Map.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * 将给定的值转换为目标类型的Map.
	 */
	@Override
	public void setValue(Object value) {
		if (value == null && this.nullAsEmptyMap) {
			super.setValue(createMap(this.mapType, 0));
		}
		else if (value == null || (this.mapType.isInstance(value) && !alwaysCreateNewMap())) {
			// 按原样使用源值, 因为它与目标类型匹配.
			super.setValue(value);
		}
		else if (value instanceof Map) {
			// Convert Map elements.
			Map<?, ?> source = (Map<?, ?>) value;
			Map<Object, Object> target = createMap(this.mapType, source.size());
			for (Map.Entry<?, ?> entry : source.entrySet()) {
				target.put(convertKey(entry.getKey()), convertValue(entry.getValue()));
			}
			super.setValue(target);
		}
		else {
			throw new IllegalArgumentException("Value cannot be converted to Map: " + value);
		}
	}

	/**
	 * 使用给定的初始容量, 创建给定类型的映射 (如果Map类型支持).
	 * 
	 * @param mapType Map的子接口
	 * @param initialCapacity 初始容量
	 * 
	 * @return 新的Map实例
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Map<Object, Object> createMap(Class<? extends Map> mapType, int initialCapacity) {
		if (!mapType.isInterface()) {
			try {
				return mapType.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate map class: " + mapType.getName(), ex);
			}
		}
		else if (SortedMap.class == mapType) {
			return new TreeMap<Object, Object>();
		}
		else {
			return new LinkedHashMap<Object, Object>(initialCapacity);
		}
	}

	/**
	 * 返回是否始终创建新Map, 即使传入Map的类型已匹配.
	 * <p>默认 "false"; 可以重写以强制创建新Map, 例如在任何情况下转换元素.
	 */
	protected boolean alwaysCreateNewMap() {
		return false;
	}

	/**
	 * 转换每个遇到的Map键的钩子.
	 * 默认实现只是按原样返回传入的键.
	 * <p>可以重写以执行某些键的转换, 例如从String转换为Integer.
	 * <p>仅在实际创建新Map时调用!
	 * 如果传入的Map的类型已匹配, 则默认情况下不是这种情况.
	 * 重写{@link #alwaysCreateNewMap()} 在每种情况下强制创建新的Map.
	 * 
	 * @param key 源Key
	 * 
	 * @return 要在目标Map中使用的Key
	 */
	protected Object convertKey(Object key) {
		return key;
	}

	/**
	 * 转换每个遇到的Map值的钩子.
	 * 默认实现只是按原样返回传入的值.
	 * <p>可以重写以执行某些值的转换, 例如从String转换为Integer.
	 * <p>仅在实际创建新Map时调用!
	 * 如果传入的Map的类型已匹配, 则默认情况下不是这种情况.
	 * 重写{@link #alwaysCreateNewMap()} 在每种情况下强制创建新的Map.
	 * 
	 * @param value 源值
	 * 
	 * @return 要在目标Map中使用的值
	 */
	protected Object convertValue(Object value) {
		return value;
	}


	/**
	 * 此实现返回{@code null}以指示没有适当的文本表示.
	 */
	@Override
	public String getAsText() {
		return null;
	}

}
