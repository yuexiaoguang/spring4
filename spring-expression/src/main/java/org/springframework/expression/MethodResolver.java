package org.springframework.expression;

import java.util.List;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 方法解析器尝试定位方法, 并返回可用于调用该方法的命令执行器.
 * 命令执行器将被缓存, 但如果它'过时', 将再次调用解析器.
 */
public interface MethodResolver {

	/**
	 * 在提供的上下文中, 在提供的对象上确定一个可以处理指定参数的合适方法.
	 * 返回可用于调用该方法的{@link MethodExecutor}, 如果找不到任何方法, 则返回{@code null}.
	 * 
	 * @param context 当前评估上下文
	 * @param targetObject 调用方法的对象
	 * @param argumentTypes 构造函数必须能够处理的参数
	 * 
	 * @return 可以调用该方法的MethodExecutor, 如果找不到该方法, 则返回null
	 */
	MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException;

}
