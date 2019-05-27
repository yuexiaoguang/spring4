package org.springframework.util;

import java.util.Comparator;
import java.util.Map;

/**
 * 基于{@code String}的路径匹配的策略接口.
 *
 * <p>由{@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping},
 * {@link org.springframework.web.servlet.mvc.multiaction.PropertiesMethodNameResolver},
 * 和{@link org.springframework.web.servlet.mvc.WebContentInterceptor}使用.
 *
 * <p>默认实现是{@link AntPathMatcher}, 支持Ant样式模式语法.
 */
public interface PathMatcher {

	/**
	 * 给定的{@code path}是否表示可以通过此接口的实现进行匹配的模式?
	 * <p>如果返回值为{@code false}, 则不必使用{@link #match}方法,
	 * 因为在静态路径字符串上的直接相等比较将导致相同的结果.
	 * 
	 * @param path 要检查的路径String
	 * 
	 * @return {@code true}如果给定的{@code path}表示一个模式
	 */
	boolean isPattern(String path);

	/**
	 * 根据PathMatcher的匹配策略, 将给定的{@code path}与给定的{@code pattern}匹配.
	 * 
	 * @param pattern 要匹配的模式
	 * @param path 要测试的路径String
	 * 
	 * @return {@code true}如果提供的{@code path}匹配, 否则{@code false}
	 */
	boolean match(String pattern, String path);

	/**
	 * 根据PathMatcher的匹配策略, 将给定的{@code path}与给定{@code pattern}的相应部分进行匹配.
	 * <p>确定模式是否至少匹配给定的基本路径, 假设完整路径也可以匹配.
	 * 
	 * @param pattern 要匹配的模式
	 * @param path 要测试的路径String
	 * 
	 * @return {@code true}如果提供的{@code path}匹配, 否则{@code false}
	 */
	boolean matchStart(String pattern, String path);

	/**
	 * 给定模式和完整路径, 确定模式映射部分.
	 * <p>该方法应该找出路径的哪个部分通过实际模式动态匹配, 也就是说,
	 * 它从给定的完整路径中剥离静态定义的前导路径, 仅返回路径的实际模式匹配部分.
	 * <p>例如: 对于"myroot/*.html"作为模式和"myroot/myfile.html"作为完整路径, 此方法应返回"myfile.html".
	 * 详细的确定规则是针对此PathMatcher的匹配策略指定的.
	 * <p>一个简单的实现可以在实际模式的情况下返回给定的完整路径, 并且在模式不包含任何动态部分的情况下返回空String
	 * (i.e. {@code pattern}参数是一个不符合实际{@link #isPattern 模式}的静态路径).
	 * 复杂的实现将区分给定路径模式的静态部分和动态部分.
	 * 
	 * @param pattern 路径模式
	 * @param path 要内省的完整途径
	 * 
	 * @return 给定{@code path}的模式映射部分 (never {@code null})
	 */
	String extractPathWithinPattern(String pattern, String path);

	/**
	 * 给定模式和完整路径, 提取URI模板变量.
	 * URI模板变量通过大括号表示 ('{' and '}').
	 * <p>例如: 对于模式 "/hotels/{hotel}" 和路径 "/hotels/1", 此方法将返回包含 "hotel"->"1"的Map.
	 * 
	 * @param pattern 路径模式, 可能包含URI模板
	 * @param path 从中提取模板变量的完整路径
	 * 
	 * @return Map, 变量名称作为键; 变量值为值
	 */
	Map<String, String> extractUriTemplateVariables(String pattern, String path);

	/**
	 * 给定完整路径, 返回适合按照该路径的显式顺序对模式进行排序的{@link Comparator}.
	 * <p>使用的完整算法取决于底层实现, 但通常, 返回的{@code Comparator}将对列表进行
	 * {@linkplain java.util.Collections#sort(java.util.List, java.util.Comparator) 排序},
	 * 以便在通用模式之前出现更具体的模式.
	 * 
	 * @param path 用于比较的完整路径
	 * 
	 * @return 能够按照显式顺序对模式进行排序的比较器
	 */
	Comparator<String> getPatternComparator(String path);

	/**
	 * 将两种模式组合成一个新模式.
	 * <p>用于组合两种模式的完整算法取决于底层实现.
	 * 
	 * @param pattern1 第一个模式
	 * @param pattern2 第二个模式
	 * 
	 * @return 两种模式的组合
	 * @throws IllegalArgumentException 两种模式不能合并
	 */
	String combine(String pattern1, String pattern2);

}
