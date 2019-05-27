package org.springframework.context.event;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

/**
 * 处理SpEL表达式解析的实用程序类. 意味着可以用作可重复使用的线程安全组件.
 */
class EventExpressionEvaluator extends CachedExpressionEvaluator {

	private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

	private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<AnnotatedElementKey, Method>(64);


	/**
	 * 为指定方法上的指定事件处理, 创建合适的{@link EvaluationContext}.
	 */
	public EvaluationContext createEvaluationContext(ApplicationEvent event, Class<?> targetClass,
			Method method, Object[] args, BeanFactory beanFactory) {

		Method targetMethod = getTargetMethod(targetClass, method);
		EventExpressionRootObject root = new EventExpressionRootObject(event, args);
		MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
				root, targetMethod, args, getParameterNameDiscoverer());
		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		return evaluationContext;
	}

	/**
	 * 指定表达式定义的条件是否匹配.
	 */
	public boolean condition(String conditionExpression,
			AnnotatedElementKey elementKey, EvaluationContext evalContext) {

		return getExpression(this.conditionCache, elementKey, conditionExpression).getValue(
				evalContext, boolean.class);
	}

	private Method getTargetMethod(Class<?> targetClass, Method method) {
		AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
		Method targetMethod = this.targetMethodCache.get(methodKey);
		if (targetMethod == null) {
			targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
			this.targetMethodCache.put(methodKey, targetMethod);
		}
		return targetMethod;
	}

}
