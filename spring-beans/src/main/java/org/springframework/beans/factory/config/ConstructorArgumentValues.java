package org.springframework.beans.factory.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 保存构造函数参数值, 通常作为bean定义的一部分.
 *
 * <p>支持构造函数参数列表中特定索引的值, 以及类型的泛型参数匹配.
 */
public class ConstructorArgumentValues {

	private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<Integer, ValueHolder>(0);

	private final List<ValueHolder> genericArgumentValues = new LinkedList<ValueHolder>();


	public ConstructorArgumentValues() {
	}

	/**
	 * 深克隆构造函数.
	 * 
	 * @param original 要克隆的ConstructorArgumentValues
	 */
	public ConstructorArgumentValues(ConstructorArgumentValues original) {
		addArgumentValues(original);
	}


	/**
	 * 将所有给定的参数值复制到此对象中, 使用单独的holder实例来保持值独立于原始对象.
	 * <p>Note: 相同的ValueHolder实例只会注册一次, 允许合并和重新合并参数值定义.
	 * 当然允许携带相同内容的不同ValueHolder实例.
	 */
	public void addArgumentValues(ConstructorArgumentValues other) {
		if (other != null) {
			for (Map.Entry<Integer, ValueHolder> entry : other.indexedArgumentValues.entrySet()) {
				addOrMergeIndexedArgumentValue(entry.getKey(), entry.getValue().copy());
			}
			for (ValueHolder valueHolder : other.genericArgumentValues) {
				if (!this.genericArgumentValues.contains(valueHolder)) {
					addOrMergeGenericArgumentValue(valueHolder.copy());
				}
			}
		}
	}


	/**
	 * 在构造函数参数列表中为给定索引添加参数值.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param value 参数值
	 */
	public void addIndexedArgumentValue(int index, Object value) {
		addIndexedArgumentValue(index, new ValueHolder(value));
	}

	/**
	 * 在构造函数参数列表中为给定索引添加参数值.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param value 参数值
	 * @param type 构造函数参数的类型
	 */
	public void addIndexedArgumentValue(int index, Object value, String type) {
		addIndexedArgumentValue(index, new ValueHolder(value, type));
	}

	/**
	 * 在构造函数参数列表中为给定索引添加参数值.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param newValue ValueHolder形式的参数值
	 */
	public void addIndexedArgumentValue(int index, ValueHolder newValue) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		Assert.notNull(newValue, "ValueHolder must not be null");
		addOrMergeIndexedArgumentValue(index, newValue);
	}

	/**
	 * 在构造函数参数列表中为给定索引添加参数值, 如果需要, 将新值(通常是集合)与当前值合并:
	 * see {@link org.springframework.beans.Mergeable}.
	 * 
	 * @param key 构造函数参数列表中的索引
	 * @param newValue ValueHolder形式的参数值
	 */
	private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
		ValueHolder currentValue = this.indexedArgumentValues.get(key);
		if (currentValue != null && newValue.getValue() instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) newValue.getValue();
			if (mergeable.isMergeEnabled()) {
				newValue.setValue(mergeable.merge(currentValue.getValue()));
			}
		}
		this.indexedArgumentValues.put(key, newValue);
	}

	/**
	 * 检查是否已为给定索引注册参数值.
	 * 
	 * @param index 构造函数参数列表中的索引
	 */
	public boolean hasIndexedArgumentValue(int index) {
		return this.indexedArgumentValues.containsKey(index);
	}

	/**
	 * 获取构造函数参数列表中给定索引的参数值.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param requiredType 要匹配的类型 (可以是{@code null}仅匹配无类型值)
	 * 
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getIndexedArgumentValue(int index, Class<?> requiredType) {
		return getIndexedArgumentValue(index, requiredType, null);
	}

	/**
	 * 获取构造函数参数列表中给定索引的参数值.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param requiredType 要匹配的类型 (可以是{@code null}仅匹配无类型值)
	 * @param requiredName 要匹配的名称 (可以是{@code null}仅匹配未命名的值, 或者空字符串以匹配任何名称)
	 * 
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getIndexedArgumentValue(int index, Class<?> requiredType, String requiredName) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = this.indexedArgumentValues.get(index);
		if (valueHolder != null &&
				(valueHolder.getType() == null ||
						(requiredType != null && ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) &&
				(valueHolder.getName() == null || "".equals(requiredName) ||
						(requiredName != null && requiredName.equals(valueHolder.getName())))) {
			return valueHolder;
		}
		return null;
	}

	/**
	 * 返回索引参数值的映射.
	 * 
	 * @return unmodifiable 将Integer索引作为键, 将ValueHolder作为值
	 */
	public Map<Integer, ValueHolder> getIndexedArgumentValues() {
		return Collections.unmodifiableMap(this.indexedArgumentValues);
	}


	/**
	 * 添加要按类型匹配的通用参数值.
	 * <p>Note: 单个通用参数值将只使用一次, 而不是多次匹配.
	 * 
	 * @param value 参数值
	 */
	public void addGenericArgumentValue(Object value) {
		this.genericArgumentValues.add(new ValueHolder(value));
	}

	/**
	 * 添加要按类型匹配的通用参数值.
	 * <p>Note: 单个通用参数值将只使用一次, 而不是多次匹配.
	 * 
	 * @param value 参数值
	 * @param type 构造函数参数的类型
	 */
	public void addGenericArgumentValue(Object value, String type) {
		this.genericArgumentValues.add(new ValueHolder(value, type));
	}

	/**
	 * 添加要通过类型或名称匹配的通用参数值.
	 * <p>Note: 单个通用参数值将只使用一次, 而不是多次匹配.
	 * 
	 * @param newValue ValueHolder形式的参数值
	 * <p>Note: 相同的ValueHolder实例只会注册一次, 允许合并和重新合并参数值定义.
	 * 允许携带相同内容的不同ValueHolder实例.
	 */
	public void addGenericArgumentValue(ValueHolder newValue) {
		Assert.notNull(newValue, "ValueHolder must not be null");
		if (!this.genericArgumentValues.contains(newValue)) {
			addOrMergeGenericArgumentValue(newValue);
		}
	}

	/**
	 * 添加通用参数值, 如果需要, 将新值(通常是集合)与当前值合并:
	 * see {@link org.springframework.beans.Mergeable}.
	 * 
	 * @param newValue ValueHolder形式的参数值
	 */
	private void addOrMergeGenericArgumentValue(ValueHolder newValue) {
		if (newValue.getName() != null) {
			for (Iterator<ValueHolder> it = this.genericArgumentValues.iterator(); it.hasNext();) {
				ValueHolder currentValue = it.next();
				if (newValue.getName().equals(currentValue.getName())) {
					if (newValue.getValue() instanceof Mergeable) {
						Mergeable mergeable = (Mergeable) newValue.getValue();
						if (mergeable.isMergeEnabled()) {
							newValue.setValue(mergeable.merge(currentValue.getValue()));
						}
					}
					it.remove();
				}
			}
		}
		this.genericArgumentValues.add(newValue);
	}

	/**
	 * 查找与给定类型匹配的泛型参数值.
	 * 
	 * @param requiredType 要匹配的类型
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getGenericArgumentValue(Class<?> requiredType) {
		return getGenericArgumentValue(requiredType, null, null);
	}

	/**
	 * 查找与给定类型匹配的泛型参数值.
	 * 
	 * @param requiredType 要匹配的类型
	 * @param requiredName 要匹配的名称
	 * 
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName) {
		return getGenericArgumentValue(requiredType, requiredName, null);
	}

	/**
	 * 查找与给定类型匹配的下一个通用参数值, 忽略已在当前解析过程中使用的参数值.
	 * 
	 * @param requiredType 要匹配的类型 (可以是{@code null}来查找任意的下一个通用参数值)
	 * @param requiredName 要匹配的名称 (可以是{@code null}不按名称匹配参数值, 或者使用空String来匹配任何名称)
	 * @param usedValueHolders 已在当前解析过程中使用过的ValueHolder对象, 不应该再次返回
	 * 
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName, Set<ValueHolder> usedValueHolders) {
		for (ValueHolder valueHolder : this.genericArgumentValues) {
			if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
				continue;
			}
			if (valueHolder.getName() != null && !"".equals(requiredName) &&
					(requiredName == null || !valueHolder.getName().equals(requiredName))) {
				continue;
			}
			if (valueHolder.getType() != null &&
					(requiredType == null || !ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
				continue;
			}
			if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
					!ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
				continue;
			}
			return valueHolder;
		}
		return null;
	}

	/**
	 * 返回通用参数值的列表.
	 * 
	 * @return unmodifiable ValueHolders的列表
	 */
	public List<ValueHolder> getGenericArgumentValues() {
		return Collections.unmodifiableList(this.genericArgumentValues);
	}


	/**
	 * 查找与构造函数参数列表中的给定索引相对应的参数值, 或者按类型进行一般匹配.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param requiredType 要匹配的参数类型
	 * 
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getArgumentValue(int index, Class<?> requiredType) {
		return getArgumentValue(index, requiredType, null, null);
	}

	/**
	 * 查找与构造函数参数列表中的给定索引相对应的参数值, 或者按类型进行一般匹配.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param requiredType 要匹配的参数类型
	 * @param requiredName 要匹配的参数名称
	 * 
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName) {
		return getArgumentValue(index, requiredType, requiredName, null);
	}

	/**
	 * 查找与构造函数参数列表中的给定索引相对应的参数值, 或者按类型进行一般匹配.
	 * 
	 * @param index 构造函数参数列表中的索引
	 * @param requiredType 要匹配的参数类型 (可以是{@code null}来查找无类型参数值)
	 * @param requiredName 要匹配的参数名称 (可以是{@code null}查找未命名的参数值, 或者使用空String来匹配任何名称)
	 * @param usedValueHolders 已在当前解析过程中使用过的ValueHolder对象, 不应再次返回
	 * (允许在多个相同类型的通用参数值的情况下, 返回下一个通用参数匹配)
	 * 
	 * @return 参数的ValueHolder, 或{@code null}
	 */
	public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName, Set<ValueHolder> usedValueHolders) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
		if (valueHolder == null) {
			valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
		}
		return valueHolder;
	}

	/**
	 * 返回此实例中保存的参数值的数量, 计算索引和泛型参数值.
	 */
	public int getArgumentCount() {
		return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
	}

	/**
	 * 返回此holder是否包含参数值, 不论是索引值, 还是通用值.
	 */
	public boolean isEmpty() {
		return (this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty());
	}

	/**
	 * 清空整个holder, 删除所有参数值.
	 */
	public void clear() {
		this.indexedArgumentValues.clear();
		this.genericArgumentValues.clear();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ConstructorArgumentValues)) {
			return false;
		}
		ConstructorArgumentValues that = (ConstructorArgumentValues) other;
		if (this.genericArgumentValues.size() != that.genericArgumentValues.size() ||
				this.indexedArgumentValues.size() != that.indexedArgumentValues.size()) {
			return false;
		}
		Iterator<ValueHolder> it1 = this.genericArgumentValues.iterator();
		Iterator<ValueHolder> it2 = that.genericArgumentValues.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			ValueHolder vh1 = it1.next();
			ValueHolder vh2 = it2.next();
			if (!vh1.contentEquals(vh2)) {
				return false;
			}
		}
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			ValueHolder vh1 = entry.getValue();
			ValueHolder vh2 = that.indexedArgumentValues.get(entry.getKey());
			if (!vh1.contentEquals(vh2)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 7;
		for (ValueHolder valueHolder : this.genericArgumentValues) {
			hashCode = 31 * hashCode + valueHolder.contentHashCode();
		}
		hashCode = 29 * hashCode;
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			hashCode = 31 * hashCode + (entry.getValue().contentHashCode() ^ entry.getKey().hashCode());
		}
		return hashCode;
	}


	/**
	 * 构造函数参数值的Holder, 带有可选的type属性, 指示实际构造函数参数的目标类型.
	 */
	public static class ValueHolder implements BeanMetadataElement {

		private Object value;

		private String type;

		private String name;

		private Object source;

		private boolean converted = false;

		private Object convertedValue;

		/**
		 * @param value 参数值
		 */
		public ValueHolder(Object value) {
			this.value = value;
		}

		/**
		 * @param value 参数值
		 * @param type 构造函数参数的类型
		 */
		public ValueHolder(Object value, String type) {
			this.value = value;
			this.type = type;
		}

		/**
		 * @param value 参数值
		 * @param type 构造函数参数的类型
		 * @param name 构造函数参数的名称
		 */
		public ValueHolder(Object value, String type, String name) {
			this.value = value;
			this.type = type;
			this.name = name;
		}

		/**
		 * 设置构造函数参数的值.
		 */
		public void setValue(Object value) {
			this.value = value;
		}

		/**
		 * 返回构造函数参数的值.
		 */
		public Object getValue() {
			return this.value;
		}

		/**
		 * 设置构造函数参数的类型.
		 */
		public void setType(String type) {
			this.type = type;
		}

		/**
		 * 返回构造函数参数的类型.
		 */
		public String getType() {
			return this.type;
		}

		/**
		 * 设置构造函数参数的名称.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * 返回构造函数参数的名称.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * 为此元数据元素设置配置源{@code Object}.
		 * <p>对象的确切类型取决于所使用的配置机制.
		 */
		public void setSource(Object source) {
			this.source = source;
		}

		@Override
		public Object getSource() {
			return this.source;
		}

		/**
		 * 返回此holder是否已包含已转换的值 ({@code true}), 或者是否仍然需要转换值 ({@code false}).
		 */
		public synchronized boolean isConverted() {
			return this.converted;
		}

		/**
		 * 在类型转换后, 设置构造函数参数的转换值.
		 */
		public synchronized void setConvertedValue(Object value) {
			this.converted = true;
			this.convertedValue = value;
		}

		/**
		 * 在类型转换后, 返回构造函数参数的转换值.
		 */
		public synchronized Object getConvertedValue() {
			return this.convertedValue;
		}

		/**
		 * 确定此ValueHolder的内容是否等于给定的其他ValueHolder的内容.
		 * <p>请注意, ValueHolder不直接实现{@code equals}, 允许具有相同内容的多个ValueHolder实例驻留在同一个Set中.
		 */
		private boolean contentEquals(ValueHolder other) {
			return (this == other ||
					(ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.type, other.type)));
		}

		/**
		 * 确定此ValueHolder的内容是否为哈希码.
		 * <p>请注意, ValueHolder不直接实现{@code hashCode}, 允许具有相同内容的多个ValueHolder实例驻留在同一个Set中.
		 */
		private int contentHashCode() {
			return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.type);
		}

		/**
		 * 创建此ValueHolder的副本: 也就是说, 具有相同内容的独立ValueHolder实例.
		 */
		public ValueHolder copy() {
			ValueHolder copy = new ValueHolder(this.value, this.type, this.name);
			copy.setSource(this.source);
			return copy;
		}
	}
}
