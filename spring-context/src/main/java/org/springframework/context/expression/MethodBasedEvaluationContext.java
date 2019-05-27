package org.springframework.context.expression;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;

/**
 * 基于方法的 {@link org.springframework.expression.EvaluationContext}, 为基于方法的调用提供显式支持.
 *
 * <p>使用以下别名暴露实际的方法参数:
 * <ol>
 * <li>pX其中X是参数的索引 (p0 是第一个参数)</li>
 * <li>aX其中X是参数的索引 (a1 是第二个参数)</li>
 * <li>由可配置的{@link ParameterNameDiscoverer}发现的参数的名称</li>
 * </ol>
 */
public class MethodBasedEvaluationContext extends StandardEvaluationContext {

	private final Method method;

	private final Object[] arguments;

	private final ParameterNameDiscoverer parameterNameDiscoverer;

	private boolean argumentsLoaded = false;


	public MethodBasedEvaluationContext(Object rootObject, Method method, Object[] arguments,
			ParameterNameDiscoverer parameterNameDiscoverer) {

		super(rootObject);
		this.method = method;
		this.arguments = arguments;
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}


	@Override
	public Object lookupVariable(String name) {
		Object variable = super.lookupVariable(name);
		if (variable != null) {
			return variable;
		}
		if (!this.argumentsLoaded) {
			lazyLoadArguments();
			this.argumentsLoaded = true;
			variable = super.lookupVariable(name);
		}
		return variable;
	}

	/**
	 * 仅在需要时加载参数信息.
	 */
	protected void lazyLoadArguments() {
		// 如果不需要加载args, 则为快捷方式
		if (ObjectUtils.isEmpty(this.arguments)) {
			return;
		}

		// 公开索引的变量以及参数名称
		String[] paramNames = this.parameterNameDiscoverer.getParameterNames(this.method);
		int paramCount = (paramNames != null ? paramNames.length : this.method.getParameterTypes().length);
		int argsCount = this.arguments.length;

		for (int i = 0; i < paramCount; i++) {
			Object value = null;
			if (argsCount > paramCount && i == paramCount - 1) {
				// 将剩余参数公开为vararg数组, 以用于最后一个参数
				value = Arrays.copyOfRange(this.arguments, i, argsCount);
			}
			else if (argsCount > i) {
				// 找到的实际参数 - 否则保留为null
				value = this.arguments[i];
			}
			setVariable("a" + i, value);
			setVariable("p" + i, value);
			if (paramNames != null) {
				setVariable(paramNames[i], value);
			}
		}
	}

}
