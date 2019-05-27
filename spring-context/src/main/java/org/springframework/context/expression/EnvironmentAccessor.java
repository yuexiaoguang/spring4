package org.springframework.context.expression;

import org.springframework.core.env.Environment;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * 只读EL属性访问器, 它知道如何检索Spring {@link Environment}实例的Key.
 */
public class EnvironmentAccessor implements PropertyAccessor {

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return new Class<?>[] {Environment.class};
	}

	/**
	 * 可以读取任何 {@link Environment}, 因此总是返回true.
	 * 
	 * @return true
	 */
	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		return true;
	}

	/**
	 * 在给定目标环境中通过解析给定属性名称来访问给定目标对象.
	 */
	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		return new TypedValue(((Environment) target).getProperty(name));
	}

	/**
	 * Read-only: returns {@code false}.
	 */
	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return false;
	}

	/**
	 * Read-only: no-op.
	 */
	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
	}

}
