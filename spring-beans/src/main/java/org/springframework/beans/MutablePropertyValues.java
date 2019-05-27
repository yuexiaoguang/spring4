package org.springframework.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * {@link PropertyValues}接口的默认实现.
 * 允许简单的操作属性, 并提供构造函数以支持Map的深度复制和构造.
 */
@SuppressWarnings("serial")
public class MutablePropertyValues implements PropertyValues, Serializable {

	private final List<PropertyValue> propertyValueList;

	private Set<String> processedProperties;

	private volatile boolean converted = false;


	/**
	 * <p>可以使用{@code add}方法添加属性值.
	 */
	public MutablePropertyValues() {
		this.propertyValueList = new ArrayList<PropertyValue>(0);
	}

	/**
	 * 深拷贝构造函数.
	 * 保证PropertyValue引用是独立的, 虽然它不能深度复制当前由各个PropertyValue对象引用的对象.
	 * 
	 * @param original 要复制的PropertyValues
	 */
	public MutablePropertyValues(PropertyValues original) {
		// 可以优化它, 因为它是全新的: 没有替换现有的属性值.
		if (original != null) {
			PropertyValue[] pvs = original.getPropertyValues();
			this.propertyValueList = new ArrayList<PropertyValue>(pvs.length);
			for (PropertyValue pv : pvs) {
				this.propertyValueList.add(new PropertyValue(pv));
			}
		}
		else {
			this.propertyValueList = new ArrayList<PropertyValue>(0);
		}
	}

	/**
	 * @param original
	 */
	public MutablePropertyValues(Map<?, ?> original) {
		// 可以优化它, 因为它是全新的: 没有替换现有的属性值.
		if (original != null) {
			this.propertyValueList = new ArrayList<PropertyValue>(original.size());
			for (Map.Entry<?, ?> entry : original.entrySet()) {
				this.propertyValueList.add(new PropertyValue(entry.getKey().toString(), entry.getValue()));
			}
		}
		else {
			this.propertyValueList = new ArrayList<PropertyValue>(0);
		}
	}

	/**
	 * <p>这是高级使用方案的构造函数. 它不适用于典型的程序化使用.
	 * 
	 * @param propertyValueList PropertyValue对象的列表
	 */
	public MutablePropertyValues(List<PropertyValue> propertyValueList) {
		this.propertyValueList =
				(propertyValueList != null ? propertyValueList : new ArrayList<PropertyValue>());
	}


	/**
	 * 以原始形式返回PropertyValue对象的底层List.
	 * 返回的List可以直接修改, 虽然不建议这样做.
	 * <p>这是对所有PropertyValue对象进行优化访问的访问器. 它不适用于典型的程序化使用.
	 */
	public List<PropertyValue> getPropertyValueList() {
		return this.propertyValueList;
	}

	/**
	 * 返回列表中PropertyValue条目的数量.
	 */
	public int size() {
		return this.propertyValueList.size();
	}

	/**
	 * 将所有给定的PropertyValues复制到此对象中.
	 * 保证PropertyValue引用是独立的, 虽然它不能深度复制当前由各个PropertyValue对象引用的对象.
	 * 
	 * @param other 要复制的PropertyValues
	 * 
	 * @return 这是为了允许在链中添加多个属性值
	 */
	public MutablePropertyValues addPropertyValues(PropertyValues other) {
		if (other != null) {
			PropertyValue[] pvs = other.getPropertyValues();
			for (PropertyValue pv : pvs) {
				addPropertyValue(new PropertyValue(pv));
			}
		}
		return this;
	}

	/**
	 * 添加给定Map中的所有属性值.
	 * 
	 * @param other 属性名(必须是String)对应属性值的Map
	 * 
	 * @return 这是为了允许在链中添加多个属性值
	 */
	public MutablePropertyValues addPropertyValues(Map<?, ?> other) {
		if (other != null) {
			for (Map.Entry<?, ?> entry : other.entrySet()) {
				addPropertyValue(new PropertyValue(entry.getKey().toString(), entry.getValue()));
			}
		}
		return this;
	}

	/**
	 * 添加PropertyValue对象, 替换相应属性的现有属性或与其合并.
	 * 
	 * @param pv 要添加的PropertyValue对象
	 * 
	 * @return 为了允许在链中添加多个属性值
	 */
	public MutablePropertyValues addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue currentPv = this.propertyValueList.get(i);
			if (currentPv.getName().equals(pv.getName())) {
				pv = mergeIfRequired(pv, currentPv);
				setPropertyValueAt(pv, i);
				return this;
			}
		}
		this.propertyValueList.add(pv);
		return this;
	}

	/**
	 * {@code addPropertyValue}的重载, 它带有属性名称和属性值.
	 * <p>Note: 截止Spring 3.0, 建议使用更简洁、更具链接性的变体{@link #add}.
	 * 
	 * @param propertyName 属性名
	 * @param propertyValue 属性值
	 */
	public void addPropertyValue(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
	}

	/**
	 * 添加PropertyValue 对象, 替换相应属性的现有属性或与其合并.
	 * 
	 * @param propertyName 属性名
	 * @param propertyValue 属性值
	 * 
	 * @return 为了允许在链中添加多个属性值
	 */
	public MutablePropertyValues add(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
		return this;
	}

	/**
	 * 修改此对象中保存的PropertyValue对象.
	 * Indexed from 0.
	 */
	public void setPropertyValueAt(PropertyValue pv, int i) {
		this.propertyValueList.set(i, pv);
	}

	/**
	 * 如果支持并启用合并, 则将提供的“新”{@link PropertyValue}的值与当前{@link PropertyValue}的值合并.
	 */
	private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
		Object value = newPv.getValue();
		if (value instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) value;
			if (mergeable.isMergeEnabled()) {
				Object merged = mergeable.merge(currentPv.getValue());
				return new PropertyValue(newPv.getName(), merged);
			}
		}
		return newPv;
	}

	/**
	 * 删除PropertyValue.
	 * 
	 * @param pv 要删除的PropertyValue
	 */
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValueList.remove(pv);
	}

	/**
	 * 带有属性名称的{@code removePropertyValue}的重载版本.
	 * 
	 * @param propertyName 属性名
	 */
	public void removePropertyValue(String propertyName) {
		this.propertyValueList.remove(getPropertyValue(propertyName));
	}


	@Override
	public PropertyValue[] getPropertyValues() {
		return this.propertyValueList.toArray(new PropertyValue[this.propertyValueList.size()]);
	}

	@Override
	public PropertyValue getPropertyValue(String propertyName) {
		for (PropertyValue pv : this.propertyValueList) {
			if (pv.getName().equals(propertyName)) {
				return pv;
			}
		}
		return null;
	}

	/**
	 * 获取原始属性值.
	 * 
	 * @param propertyName 要搜索的名称
	 * 
	 * @return 原始属性值, 或{@code null}
	 */
	public Object get(String propertyName) {
		PropertyValue pv = getPropertyValue(propertyName);
		return (pv != null ? pv.getValue() : null);
	}

	@Override
	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		if (old == this) {
			return changes;
		}

		// 对于新集中的每个属性值
		for (PropertyValue newPv : this.propertyValueList) {
			// if there wasn't an old one, add it
			PropertyValue pvOld = old.getPropertyValue(newPv.getName());
			if (pvOld == null || !pvOld.equals(newPv)) {
				changes.addPropertyValue(newPv);
			}
		}
		return changes;
	}

	@Override
	public boolean contains(String propertyName) {
		return (getPropertyValue(propertyName) != null ||
				(this.processedProperties != null && this.processedProperties.contains(propertyName)));
	}

	@Override
	public boolean isEmpty() {
		return this.propertyValueList.isEmpty();
	}


	/**
	 * 在某些处理器的意义上将指定的属性注册为“已处理”, 在PropertyValue 机制之外调用相应的setter方法.
	 * <p>这将导致调用指定属性的{@link #contains}将返回{@code true}.
	 * 
	 * @param propertyName the name of the property.
	 */
	public void registerProcessedProperty(String propertyName) {
		if (this.processedProperties == null) {
			this.processedProperties = new HashSet<String>();
		}
		this.processedProperties.add(propertyName);
	}

	/**
	 * 清除给定属性的“已处理”注册.
	 */
	public void clearProcessedProperty(String propertyName) {
		if (this.processedProperties != null) {
			this.processedProperties.remove(propertyName);
		}
	}

	/**
	 * 将此持有者标记为仅包含转换值 (i.e. 不再需要运行时解析).
	 */
	public void setConverted() {
		this.converted = true;
	}

	/**
	 * 返回此持有者是否仅包含转换值 ({@code true}), 或者是否仍然需要转换这些值 ({@code false}).
	 */
	public boolean isConverted() {
		return this.converted;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MutablePropertyValues)) {
			return false;
		}
		MutablePropertyValues that = (MutablePropertyValues) other;
		return this.propertyValueList.equals(that.propertyValueList);
	}

	@Override
	public int hashCode() {
		return this.propertyValueList.hashCode();
	}

	@Override
	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		StringBuilder sb = new StringBuilder("PropertyValues: length=").append(pvs.length);
		if (pvs.length > 0) {
			sb.append("; ").append(StringUtils.arrayToDelimitedString(pvs, "; "));
		}
		return sb.toString();
	}

}
