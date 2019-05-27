package org.springframework.expression;

/**
 * MethodExecutors由解析器构建，可以由基础架构缓存，以便快速重复操作，而无需返回解析器.
 * 例如, 可以通过反射方法解析器发现在对象上运行的特定方法
 *  - 然后它将构建一个执行该方法的MethodExecutor, 并且可以重用MethodExecutor而无需返回解析器再次发现该方法.
 *
 * <p>它们可能变得陈旧, 在这种情况下应该抛出一个AccessException:
 * 这将导致基础设施返回解析器以请求新的执行器.
 */
public interface MethodExecutor {

	/**
	 * 使用指定的参数执行命令, 并使用指定的表达式状态.
	 * 
	 * @param context 正在执行命令的评估上下文
	 * @param target 调用的目标对象 - null 为静态方法
	 * @param arguments 执行器的参数应该(在数量和类型方面)与命令运行所需的参数匹配
	 * 
	 * @return 执行返回的值
	 * @throws AccessException 如果执行命令时出现问题或MethodExecutor不再有效
	 */
	TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException;

}
