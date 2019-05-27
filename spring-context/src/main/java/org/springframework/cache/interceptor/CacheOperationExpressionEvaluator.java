package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.Cache;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

/**
 * 处理SpEL表达式解析的工具类.
 * 意味着可以用作可重复使用的线程安全组件.
 *
 * <p>使用{@link AnnotatedElementKey}执行内部缓存以获取性能.
 */
class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

	/**
	 * 没有结果变量.
	 */
	public static final Object NO_RESULT = new Object();

	/**
	 * 根本不能使用结果变量.
	 */
	public static final Object RESULT_UNAVAILABLE = new Object();

	/**
	 * 保存结果对象的变量的名称.
	 */
	public static final String RESULT_VARIABLE = "result";


	private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

	private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

	private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

	private final Map<AnnotatedElementKey, Method> targetMethodCache =
			new ConcurrentHashMap<AnnotatedElementKey, Method>(64);


	/**
	 * 创建一个没有返回值的{@link EvaluationContext}.
	 */
	public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
			Method method, Object[] args, Object target, Class<?> targetClass, BeanFactory beanFactory) {

		return createEvaluationContext(caches, method, args, target, targetClass, NO_RESULT, beanFactory);
	}

	/**
	 * @param caches 当前的缓存
	 * @param method 方法
	 * @param args 方法参数
	 * @param target 目标对象
	 * @param targetClass 目标类
	 * @param result 返回值(can be {@code null}); 或{@link #NO_RESULT} 如果此时没有返回
	 * 
	 * @return 评估上下文
	 */
	public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
			Method method, Object[] args, Object target, Class<?> targetClass, Object result,
			BeanFactory beanFactory) {

		CacheExpressionRootObject rootObject = new CacheExpressionRootObject(
				caches, method, args, target, targetClass);
		Method targetMethod = getTargetMethod(targetClass, method);
		CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
				rootObject, targetMethod, args, getParameterNameDiscoverer());
		if (result == RESULT_UNAVAILABLE) {
			evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
		}
		else if (result != NO_RESULT) {
			evaluationContext.setVariable(RESULT_VARIABLE, result);
		}
		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		return evaluationContext;
	}

	public Object key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext);
	}

	public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return getExpression(this.conditionCache, methodKey, conditionExpression).getValue(evalContext, boolean.class);
	}

	public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return getExpression(this.unlessCache, methodKey, unlessExpression).getValue(evalContext, boolean.class);
	}

	/**
	 * 清除所有缓存.
	 */
	void clear() {
		this.keyCache.clear();
		this.conditionCache.clear();
		this.unlessCache.clear();
		this.targetMethodCache.clear();
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
