package org.springframework.expression.spel.support;

import java.lang.reflect.Constructor;

import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;

/**
 * 一个简单的ConstructorExecutor实现, 它使用反射调用运行构造函数.
 */
public class ReflectiveConstructorExecutor implements ConstructorExecutor {

	private final Constructor<?> ctor;

	private final Integer varargsPosition;


	public ReflectiveConstructorExecutor(Constructor<?> ctor) {
		this.ctor = ctor;
		if (ctor.isVarArgs()) {
			Class<?>[] paramTypes = ctor.getParameterTypes();
			this.varargsPosition = paramTypes.length - 1;
		}
		else {
			this.varargsPosition = null;
		}
	}

	@Override
	public TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException {
		try {
			if (arguments != null) {
				ReflectionHelper.convertArguments(context.getTypeConverter(), arguments, this.ctor, this.varargsPosition);
			}
			if (this.ctor.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(this.ctor.getParameterTypes(), arguments);
			}
			ReflectionUtils.makeAccessible(this.ctor);
			return new TypedValue(this.ctor.newInstance(arguments));
		}
		catch (Exception ex) {
			throw new AccessException("Problem invoking constructor: " + this.ctor, ex);
		}
	}

	public Constructor<?> getConstructor() {
		return this.ctor;
	}

}
