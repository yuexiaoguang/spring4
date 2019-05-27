package org.springframework.expression.spel.support;

import java.lang.reflect.Method;

/**
 * 用于数据绑定目的的{@link org.springframework.expression.PropertyAccessor}变体,
 * 使用反射来访问用于读取和可能写入的属性.
 *
 * <p>可以通过 public getter方法 (在读取时)或 public setter方法 (在写入时), 以及作为 public字段引用属性.
 *
 * <p>此访问器是为用户声明的属性显式设计的, 不解析{@code java.lang.Object} 或 {@code java.lang.Class}上的技术属性.
 * 对于不受限制的解析, 请选择{@link ReflectivePropertyAccessor}.
 */
public class DataBindingPropertyAccessor extends ReflectivePropertyAccessor {

	/**
	 * 创建一个新的属性访问器, 用于读取和写入.
	 * 
	 * @param allowWrite 是否也允许写操作
	 */
	private DataBindingPropertyAccessor(boolean allowWrite) {
		super(allowWrite);
	}

	@Override
	protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
		Class<?> clazz = method.getDeclaringClass();
		return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
	}


	/**
	 * 为只读操作创建新的数据绑定属性访问器.
	 */
	public static DataBindingPropertyAccessor forReadOnlyAccess() {
		return new DataBindingPropertyAccessor(false);
	}

	/**
	 * 为读写操作创建新的数据绑定属性访问器.
	 */
	public static DataBindingPropertyAccessor forReadWriteAccess() {
		return new DataBindingPropertyAccessor(true);
	}

}
