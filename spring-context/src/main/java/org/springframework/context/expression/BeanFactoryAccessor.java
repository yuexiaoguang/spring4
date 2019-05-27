package org.springframework.context.expression;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * EL属性访问器, 知道如何遍历Spring {@link org.springframework.beans.factory.BeanFactory}的bean.
 */
public class BeanFactoryAccessor implements PropertyAccessor {

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return new Class<?>[] {BeanFactory.class};
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		return (((BeanFactory) target).containsBean(name));
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		return new TypedValue(((BeanFactory) target).getBean(name));
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		throw new AccessException("Beans in a BeanFactory are read-only");
	}

}
