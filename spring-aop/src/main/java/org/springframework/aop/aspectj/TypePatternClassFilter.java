package org.springframework.aop.aspectj;

import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.TypePatternMatcher;

import org.springframework.aop.ClassFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用AspectJ类型匹配的Spring AOP {@link ClassFilter}实现.
 */
public class TypePatternClassFilter implements ClassFilter {

	private String typePattern;

	private TypePatternMatcher aspectJTypePatternMatcher;


	/**
	 * <p>确保设置{@link #setTypePattern(String) typePattern}属性, 否则毫无疑问会抛出致命的{@link IllegalStateException},
	 * 当第一次调用{@link #matches(Class)}方法时.
	 */
	public TypePatternClassFilter() {
	}

	/**
	 * 使用给定的类型模式创建完全配置的 {@link TypePatternClassFilter}.
	 * 
	 * @param typePattern AspectJ weaver应该解析的类型模式
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code typePattern}是{@code null}, 或被视为无效
	 */
	public TypePatternClassFilter(String typePattern) {
		setTypePattern(typePattern);
	}


	/**
	 * 设置AspectJ类型模式以匹配.
	 * <p>例子包括:
	 * <code class="code">
	 * org.springframework.beans.*
	 * </code>
	 * 这将匹配给定包中的任何类或接口.
	 * <code class="code">
	 * org.springframework.beans.ITestBean+
	 * </code>
	 * 这将匹配{@code ITestBean}接口和实现它的任何类.
	 * <p>这些约定由AspectJ建立, 不是Spring AOP.
	 * 
	 * @param typePattern AspectJ weaver应该解析的类型模式
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code typePattern}是{@code null}, 或被视为无效
	 */
	public void setTypePattern(String typePattern) {
		Assert.notNull(typePattern, "Type pattern must not be null");
		this.typePattern = typePattern;
		this.aspectJTypePatternMatcher =
				PointcutParser.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution().
				parseTypePattern(replaceBooleanOperators(typePattern));
	}

	/**
	 * 返回要匹配的AspectJ类型模式.
	 */
	public String getTypePattern() {
		return this.typePattern;
	}


	/**
	 * 切点是否应用于给定的接口或目标类?
	 * 
	 * @param clazz 候选目标类
	 * 
	 * @return 增强是否适用于此候选目标类
	 * @throws IllegalStateException 如果未设置{@link #setTypePattern(String)}
	 */
	@Override
	public boolean matches(Class<?> clazz) {
		Assert.state(this.aspectJTypePatternMatcher != null, "No type pattern has been set");
		return this.aspectJTypePatternMatcher.matches(clazz);
	}

	/**
	 * 如果已在XML中指定了类型模式, 用户不能将{@code and}写为 "&&" (虽然 &amp;&amp; 也可以运行).
	 * 也允许两个子表达式之间的{@code and}.
	 * <p>此方法将为AspectJ切点解析器转换回{@code &&}.
	 */
	private String replaceBooleanOperators(String pcExpr) {
		String result = StringUtils.replace(pcExpr," and "," && ");
		result = StringUtils.replace(result, " or ", " || ");
		return StringUtils.replace(result, " not ", " ! ");
	}
}
