package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.util.Assert;

/**
 * 解析autowire候选者的限定符.
 * 包含一个或多个此类限定符的bean定义, 允许对字段或参数上的注解进行细粒度匹配, 以进行自动装配.
 */
@SuppressWarnings("serial")
public class AutowireCandidateQualifier extends BeanMetadataAttributeAccessor {

	public static final String VALUE_KEY = "value";

	private final String typeName;


	/**
	 * 匹配给定类型的注解.
	 * 
	 * @param type 注解类型
	 */
	public AutowireCandidateQualifier(Class<?> type) {
		this(type.getName());
	}

	/**
	 * 匹配给定类型名称的注解.
	 * <p>类型名称可以匹配注解的完全限定类名或短类名 (不包括包名).
	 * 
	 * @param typeName 注解类型的名称
	 */
	public AutowireCandidateQualifier(String typeName) {
		Assert.notNull(typeName, "Type name must not be null");
		this.typeName = typeName;
	}

	/**
	 * 匹配给定类型的注解, 该注解的{@code value}属性也与指定值匹配.
	 * 
	 * @param type 注解类型
	 * @param value 要匹配的注解值
	 */
	public AutowireCandidateQualifier(Class<?> type, Object value) {
		this(type.getName(), value);
	}

	/**
	 * 匹配给定类型名称的注解, 该注解的{@code value}属性也与指定值匹配.
	 * <p>类型名称可以匹配注解的完全限定类名或短类名 (不包括包名).
	 * 
	 * @param typeName 注解类型的名称
	 * @param value 要匹配的注解值
	 */
	public AutowireCandidateQualifier(String typeName, Object value) {
		Assert.notNull(typeName, "Type name must not be null");
		this.typeName = typeName;
		setAttribute(VALUE_KEY, value);
	}


	/**
	 * 检索类型名称.
	 * 如果为构造函数提供了Class实例, 则此值将与提供给构造函数的类型名称或完全限定的类名相同.
	 */
	public String getTypeName() {
		return this.typeName;
	}

}
