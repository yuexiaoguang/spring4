package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * 缓存特定的评估上下文, 以延迟方式将方法参数添加为SpEL变量.
 * 延迟本质消除了对参数发现的类字节代码的不必要的解析.
 *
 * <p>还定义一组 "不可用的变量" (i.e. 在访问时导致异常的变量).
 * 即使并非所有潜在变量都存在, 这对于验证条件不匹配也很有用.
 *
 * <p>为了限制对象的创建, 使用了丑陋的构造函数 (而不是一个专门的'封闭'类延迟执行类).
 */
class CacheEvaluationContext extends MethodBasedEvaluationContext {

	private final Set<String> unavailableVariables = new HashSet<String>(1);


	CacheEvaluationContext(Object rootObject, Method method, Object[] arguments,
			ParameterNameDiscoverer parameterNameDiscoverer) {

		super(rootObject, method, arguments, parameterNameDiscoverer);
	}


	/**
	 * 将指定的变量名称添加为该上下文不可用.
	 * 尝试访问此变量的任何表达式都应该导致异常.
	 * <p>这允许验证可能变量的表达式, 即使这样的变量尚不可用.
	 * 因此, 任何试图使用该变量的表达式都无法进行评估.
	 */
	public void addUnavailableVariable(String name) {
		this.unavailableVariables.add(name);
	}


	/**
	 * 仅在需要时加载参数信息.
	 */
	@Override
	public Object lookupVariable(String name) {
		if (this.unavailableVariables.contains(name)) {
			throw new VariableNotAvailableException(name);
		}
		return super.lookupVariable(name);
	}

}
