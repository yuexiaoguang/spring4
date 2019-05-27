package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;

public class ReflectiveMethodExecutor implements MethodExecutor {

	private final Method method;

	private final Integer varargsPosition;

	private boolean computedPublicDeclaringClass = false;

	private Class<?> publicDeclaringClass;

	private boolean argumentConversionOccurred = false;


	public ReflectiveMethodExecutor(Method method) {
		this.method = method;
		if (method.isVarArgs()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			this.varargsPosition = paramTypes.length - 1;
		}
		else {
			this.varargsPosition = null;
		}
	}


	public Method getMethod() {
		return this.method;
	}

	/**
	 * 在声明此方法的声明类层次结构的方法中查找第一个public类.
	 * 有时反射方法发现逻辑找到一个合适的方法, 可以通过反射轻松调用, 但由于可见性限制, 在编译表达式时无法从生成的代码调用.
	 * 例如, 如果非public类重写toString(), 则此辅助方法将向上走动类型层次结构, 以查找声明该方法的第一个public类型 (如果有的话!).
	 * 对于toString(), 它可以走到Object.
	 */
	public Class<?> getPublicDeclaringClass() {
		if (!this.computedPublicDeclaringClass) {
			this.publicDeclaringClass = discoverPublicClass(this.method, this.method.getDeclaringClass());
			this.computedPublicDeclaringClass = true;
		}
		return this.publicDeclaringClass;
	}

	private Class<?> discoverPublicClass(Method method, Class<?> clazz) {
		if (Modifier.isPublic(clazz.getModifiers())) {
			try {
				clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
				return clazz;
			}
			catch (NoSuchMethodException ex) {
				// Continue below...
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc: ifcs) {
			discoverPublicClass(method, ifc);
		}
		if (clazz.getSuperclass() != null) {
			return discoverPublicClass(method, clazz.getSuperclass());
		}
		return null;
	}

	public boolean didArgumentConversionOccur() {
		return this.argumentConversionOccurred;
	}


	@Override
	public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
		try {
			if (arguments != null) {
				this.argumentConversionOccurred = ReflectionHelper.convertArguments(
						context.getTypeConverter(), arguments, this.method, this.varargsPosition);
				if (this.method.isVarArgs()) {
					arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
							this.method.getParameterTypes(), arguments);
				}
			}
			ReflectionUtils.makeAccessible(this.method);
			Object value = this.method.invoke(target, arguments);
			return new TypedValue(value, new TypeDescriptor(new MethodParameter(this.method, -1)).narrow(value));
		}
		catch (Exception ex) {
			throw new AccessException("Problem invoking method: " + this.method, ex);
		}
	}

}
