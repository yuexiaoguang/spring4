package org.springframework.core;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.util.Assert;

/**
 * 此类的目的是启用捕获和传递泛型{@link Type}.
 * 为了捕获泛型类型并在运行时保留它, 需要创建一个子类 (理想情况下为匿名内联类), 如下所示:
 *
 * <pre class="code">
 * ParameterizedTypeReference&lt;List&lt;String&gt;&gt; typeRef = new ParameterizedTypeReference&lt;List&lt;String&gt;&gt;() {};
 * </pre>
 *
 * <p>然后, 生成的{@code typeRef}实例可用于获取{@link Type}实例, 该实例在运行时携带捕获的参数化类型信息.
 * 有关"super type tokens"的更多信息, 请参阅Neal Gafter博客文章的链接.
 */
public abstract class ParameterizedTypeReference<T> {

	private final Type type;


	protected ParameterizedTypeReference() {
		Class<?> parameterizedTypeReferenceSubclass = findParameterizedTypeReferenceSubclass(getClass());
		Type type = parameterizedTypeReferenceSubclass.getGenericSuperclass();
		Assert.isInstanceOf(ParameterizedType.class, type, "Type must be a parameterized type");
		ParameterizedType parameterizedType = (ParameterizedType) type;
		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
		Assert.isTrue(actualTypeArguments.length == 1, "Number of type arguments must be 1");
		this.type = actualTypeArguments[0];
	}

	private ParameterizedTypeReference(Type type) {
		this.type = type;
	}


	public Type getType() {
		return this.type;
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj || (obj instanceof ParameterizedTypeReference &&
				this.type.equals(((ParameterizedTypeReference<?>) obj).type)));
	}

	@Override
	public int hashCode() {
		return this.type.hashCode();
	}

	@Override
	public String toString() {
		return "ParameterizedTypeReference<" + this.type + ">";
	}


	/**
	 * 构建包装给定类型的{@code ParameterizedTypeReference}.
	 * 
	 * @param type 泛型类型 (可能通过反射获得, e.g. 从{@link java.lang.reflect.Method#getGenericReturnType()})
	 * 
	 * @return 可以传递给{@code ParameterizedTypeReference}-接受方法的相应引用
	 */
	public static <T> ParameterizedTypeReference<T> forType(Type type) {
		return new ParameterizedTypeReference<T>(type) {
		};
	}

	private static Class<?> findParameterizedTypeReferenceSubclass(Class<?> child) {
		Class<?> parent = child.getSuperclass();
		if (Object.class == parent) {
			throw new IllegalStateException("Expected ParameterizedTypeReference superclass");
		}
		else if (ParameterizedTypeReference.class == parent) {
			return child;
		}
		else {
			return findParameterizedTypeReferenceSubclass(parent);
		}
	}

}
