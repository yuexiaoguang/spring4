package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ObjectUtils;

/**
 * 封装对象和描述它的{@link TypeDescriptor}.
 * 类型描述符可以包含通过对象上的简单{@code getClass()}调用无法访问的泛型声明.
 */
public class TypedValue {

	public static final TypedValue NULL = new TypedValue(null);


	private final Object value;

	private TypeDescriptor typeDescriptor;


	/**
	 * 为简单对象创建{@link TypedValue}.
	 * {@link TypeDescriptor}是从对象推断出来的, 因此不保留泛型声明.
	 * 
	 * @param value 对象值
	 */
	public TypedValue(Object value) {
		this.value = value;
		this.typeDescriptor = null;  // initialized when/if requested
	}

	/**
	 * 使用特定的{@link TypeDescriptor}为特定值创建{@link TypedValue}, 其中可能包含其他泛型声明.
	 * 
	 * @param value 对象值
	 * @param typeDescriptor 描述值类型的类型描述符
	 */
	public TypedValue(Object value, TypeDescriptor typeDescriptor) {
		this.value = value;
		this.typeDescriptor = typeDescriptor;
	}


	public Object getValue() {
		return this.value;
	}

	public TypeDescriptor getTypeDescriptor() {
		if (this.typeDescriptor == null && this.value != null) {
			this.typeDescriptor = TypeDescriptor.forObject(this.value);
		}
		return this.typeDescriptor;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypedValue)) {
			return false;
		}
		TypedValue otherTv = (TypedValue) other;
		// 如果没有必要, 请避免使用TypeDescriptor初始化
		return (ObjectUtils.nullSafeEquals(this.value, otherTv.value) &&
				((this.typeDescriptor == null && otherTv.typeDescriptor == null) ||
						ObjectUtils.nullSafeEquals(getTypeDescriptor(), otherTv.getTypeDescriptor())));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "TypedValue: '" + this.value + "' of [" + getTypeDescriptor() + "]";
	}
}
