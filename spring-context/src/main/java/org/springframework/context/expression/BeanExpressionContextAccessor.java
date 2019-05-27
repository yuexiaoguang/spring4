package org.springframework.context.expression;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * 知道如何遍历Spring {@link org.springframework.beans.factory.config.BeanExpressionContext}的bean和上下文对象的EL属性访问器.
 */
public class BeanExpressionContextAccessor implements PropertyAccessor {

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		return ((BeanExpressionContext) target).containsObject(name);
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		return new TypedValue(((BeanExpressionContext) target).getObject(name));
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		throw new AccessException("Beans in a BeanFactory are read-only");
	}

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return new Class<?>[] {BeanExpressionContext.class};
	}

}
