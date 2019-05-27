package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Holder for a typed String value.
 * 可以添加到bean定义中, 以便为String值显式指定目标类型, 例如对于集合元素.
 *
 * <p>该持有者将只存储String值和目标类型.
 * 实际的转换将由bean工厂执行.
 */
public class TypedStringValue implements BeanMetadataElement {

	private String value;

	private volatile Object targetType;

	private Object source;

	private String specifiedTypeName;

	private volatile boolean dynamic;


	/**
	 * @param value the String value
	 */
	public TypedStringValue(String value) {
		setValue(value);
	}

	/**
	 * @param value the String value
	 * @param targetType 要转换的类型
	 */
	public TypedStringValue(String value, Class<?> targetType) {
		setValue(value);
		setTargetType(targetType);
	}

	/**
	 * @param value the String value
	 * @param targetTypeName 要转换的类型
	 */
	public TypedStringValue(String value, String targetTypeName) {
		setValue(value);
		setTargetTypeName(targetTypeName);
	}


	/**
	 * Set the String value.
	 * <p>只有在操作注册值时才需要, 例如在BeanFactoryPostProcessors中.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Return the String value.
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * 设置要转换为的类型.
	 * <p>只有在操作注册值时才需要, 例如在BeanFactoryPostProcessors中.
	 */
	public void setTargetType(Class<?> targetType) {
		Assert.notNull(targetType, "'targetType' must not be null");
		this.targetType = targetType;
	}

	/**
	 * 返回要转换为的类型.
	 */
	public Class<?> getTargetType() {
		Object targetTypeValue = this.targetType;
		if (!(targetTypeValue instanceof Class)) {
			throw new IllegalStateException("Typed String value does not carry a resolved target type");
		}
		return (Class<?>) targetTypeValue;
	}

	/**
	 * 指定要转换的类型.
	 */
	public void setTargetTypeName(String targetTypeName) {
		Assert.notNull(targetTypeName, "'targetTypeName' must not be null");
		this.targetType = targetTypeName;
	}

	/**
	 * 返回要转换的类型.
	 */
	public String getTargetTypeName() {
		Object targetTypeValue = this.targetType;
		if (targetTypeValue instanceof Class) {
			return ((Class<?>) targetTypeValue).getName();
		}
		else {
			return (String) targetTypeValue;
		}
	}

	/**
	 * 返回此类型的String值是否包含目标类型.
	 */
	public boolean hasTargetType() {
		return (this.targetType instanceof Class);
	}

	/**
	 * 确定要转换的类型, 必要时从指定的类名解析它.
	 * 在使用已解析的目标类型调用时, 还将从其名称重新加载指定的Class.
	 * 
	 * @param classLoader 用于解析（潜在）类名的ClassLoader
	 * 
	 * @return 要转换为的已解析的类型
	 * @throws ClassNotFoundException 如果类型无法解析
	 */
	public Class<?> resolveTargetType(ClassLoader classLoader) throws ClassNotFoundException {
		if (this.targetType == null) {
			return null;
		}
		Class<?> resolvedClass = ClassUtils.forName(getTargetTypeName(), classLoader);
		this.targetType = resolvedClass;
		return resolvedClass;
	}


	/**
	 * 为此元数据元素设置配置源{@code Object}.
	 * <p>对象的精确类型取决于所使用的配置机制.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置实际为此特定值指定的类型名称.
	 */
	public void setSpecifiedTypeName(String specifiedTypeName) {
		this.specifiedTypeName = specifiedTypeName;
	}

	/**
	 * 返回实际为此特定值指定的类型名称.
	 */
	public String getSpecifiedTypeName() {
		return this.specifiedTypeName;
	}

	/**
	 * 将此值标记为动态, i.e. 因为包含表达式, 因此不缓存.
	 */
	public void setDynamic() {
		this.dynamic = true;
	}

	/**
	 * 返回此值是否已标记为动态.
	 */
	public boolean isDynamic() {
		return this.dynamic;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypedStringValue)) {
			return false;
		}
		TypedStringValue otherValue = (TypedStringValue) other;
		return (ObjectUtils.nullSafeEquals(this.value, otherValue.value) &&
				ObjectUtils.nullSafeEquals(this.targetType, otherValue.targetType));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.targetType);
	}

	@Override
	public String toString() {
		return "TypedStringValue: value [" + this.value + "], target type [" + this.targetType + "]";
	}

}
