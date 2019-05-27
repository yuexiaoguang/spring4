package org.springframework.aop.framework;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 切面链的工厂接口.
 */
public interface AdvisorChainFactory {

	/**
	 * 确定给定切面链配置的{@link org.aopalliance.intercept.MethodInterceptor}对象列表.
	 * 
	 * @param config Advised对象配置形式的AOP配置
	 * @param method 代理的方法
	 * @param targetClass 目标类 (可能是 {@code null} 指示没有目标对象的代理, 在这种情况下, 方法的声明类是下一个最佳选择)
	 * 
	 * @return 一组MethodInterceptors (可能也包括InterceptorAndDynamicMethodMatcher)
	 */
	List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, Class<?> targetClass);

}
