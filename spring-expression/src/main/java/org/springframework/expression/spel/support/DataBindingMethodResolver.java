package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;

/**
 * 用于数据绑定目的的{@link org.springframework.expression.MethodResolver}变体,
 * 使用反射访问给定目标对象上的实例方法.
 *
 * <p>此访问器不解析静态方法, 也不解析{@code java.lang.Object}或{@code java.lang.Class}上的技术方法.
 * 要获得不受限制的解析, 请选择{@link ReflectiveMethodResolver}.
 */
public class DataBindingMethodResolver extends ReflectiveMethodResolver {

	private DataBindingMethodResolver() {
		super();
	}

	@Override
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		if (targetObject instanceof Class) {
			throw new IllegalArgumentException("DataBindingMethodResolver does not support Class targets");
		}
		return super.resolve(context, targetObject, name, argumentTypes);
	}

	@Override
	protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
		if (Modifier.isStatic(method.getModifiers())) {
			return false;
		}
		Class<?> clazz = method.getDeclaringClass();
		return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
	}


	/**
	 * 为实例方法解析创建新的数据绑定方法解析器.
	 */
	public static DataBindingMethodResolver forInstanceMethodInvocation() {
		return new DataBindingMethodResolver();
	}

}
