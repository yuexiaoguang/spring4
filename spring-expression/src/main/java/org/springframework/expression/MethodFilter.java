package org.springframework.expression;

import java.lang.reflect.Method;
import java.util.List;

/**
 * MethodFilter实例允许SpEL用户微调方法解析过程的行为.
 * 方法解析(从表达式中的方法名称转换为要调用的实际方法)通常会通过对'Class.getMethods()'的简单调用
 * 来检索调用的候选方法, 并将选择适合输入参数的第一个.
 * 通过注册MethodFilter, 用户可以接收回调并更改将被认为合适的方法.
 */
public interface MethodFilter {

	/**
	 * 由方法解析器调用, 以允许SpEL用户组织可以调用的候选方法列表.
	 * 过滤器可以删除不应被视为候选的方法, 并且可以对结果进行排序.
	 * 然后解析器将在查找要调用的合适候选者时, 搜索从过滤器返回的方法.
	 * 
	 * @param methods 解析器可供选择的完整方法列表
	 * 
	 * @return 输入方法的可能子集, 可以按相关性顺序排序
	 */
	List<Method> filter(List<Method> methods);

}
