package org.springframework.expression;


// TODO Is the resolver/executor model too pervasive in this package?
/**
 * 执行器由解析器构建, 可由基础结构缓存, 以便快速重复操作, 而无需返回解析器.
 * 例如, 反射构造函数解析器可以发现在类上运行的特定构造函数
 * - 然后它将构建一个执行该构造函数的ConstructorExecutor, 并且可以重用ConstructorExecutor, 而无需返回解析器再次发现构造函数.
 *
 * <p>它们可能变得陈旧, 在这种情况下应该抛出一个AccessException - 这将导致基础设施返回到解析器以请求新的.
 */
public interface ConstructorExecutor {

	/**
	 * 使用指定的参数在指定的上下文中执行构造函数.
	 * 
	 * @param context 正在执行命令的评估上下文
	 * @param arguments 构造函数调用的参数应该 (在数量和类型方面)与命令运行所需的参数匹配
	 * 
	 * @return 新的对象
	 * @throws AccessException 如果执行命令时出现问题或CommandExecutor不再有效
	 */
	TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
