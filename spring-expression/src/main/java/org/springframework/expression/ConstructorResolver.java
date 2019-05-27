package org.springframework.expression;

import java.util.List;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 构造函数解析器尝试定位构造函数, 并返回可用于调用该构造函数的ConstructorExecutor.
 * ConstructorExecutor将被缓存, 但如果它“过时”, 将再次调用解析器.
 */
public interface ConstructorResolver {

	/**
	 * 在提供的上下文中, 确定提供的类型上可以处理指定参数的适当构造函数.
	 * 返回一个可用于调用该构造函数的ConstructorExecutor (如果找不到构造函数, 则返回{@code null}).
	 * 
	 * @param context 当前评估上下文
	 * @param typeName 查找构造函数的类型
	 * @param argumentTypes 构造函数必须能够处理的参数
	 * 
	 * @return 可以调用构造函数的ConstructorExecutor, 或null
	 */
	ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
			throws AccessException;

}
