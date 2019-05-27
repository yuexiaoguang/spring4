package org.springframework.aop.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 基于{@code java.util.regex}包的正则表达式切点.
 * 支持以下JavaBean属性:
 * <ul>
 * <li>pattern: 要匹配完全限定方法名称的正则表达式
 * <li>patterns: 采用String数组模式的替代属性. 结果将是这些模式的结合.
 * </ul>
 *
 * <p>Note: 正则表达式必须匹配. 例如, {@code .*get.*} 将匹配 com.mycom.Foo.getBar(). {@code get.*}不会.
 */
@SuppressWarnings("serial")
public class JdkRegexpMethodPointcut extends AbstractRegexpMethodPointcut {

	/**
	 * 编译形式的模式.
	 */
	private Pattern[] compiledPatterns = new Pattern[0];

	/**
	 * 编译形式的排除模式.
	 */
	private Pattern[] compiledExclusionPatterns = new Pattern[0];


	/**
	 * 从提供的{@code String[]}初始化 {@link Pattern Patterns}.
	 */
	@Override
	protected void initPatternRepresentation(String[] patterns) throws PatternSyntaxException {
		this.compiledPatterns = compilePatterns(patterns);
	}

	/**
	 * 从提供的{@code String []}初始化排除{@link Pattern Patterns}.
	 */
	@Override
	protected void initExcludedPatternRepresentation(String[] excludedPatterns) throws PatternSyntaxException {
		this.compiledExclusionPatterns = compilePatterns(excludedPatterns);
	}

	/**
	 * 如果索引{@code patternIndex}中的{@link Pattern}与提供的候选{@code String}匹配，则返回{@code true}.
	 */
	@Override
	protected boolean matches(String pattern, int patternIndex) {
		Matcher matcher = this.compiledPatterns[patternIndex].matcher(pattern);
		return matcher.matches();
	}

	/**
	 * 如果索引{@code patternIndex}中的排除{@link Pattern}与提供的候选{@code String}匹配，则返回{@code true}.
	 */
	@Override
	protected boolean matchesExclusion(String candidate, int patternIndex) {
		Matcher matcher = this.compiledExclusionPatterns[patternIndex].matcher(candidate);
		return matcher.matches();
	}


	/**
	 * 将提供的{@code String []}编译为{@link Pattern}对象数组并返回该数组.
	 */
	private Pattern[] compilePatterns(String[] source) throws PatternSyntaxException {
		Pattern[] destination = new Pattern[source.length];
		for (int i = 0; i < source.length; i++) {
			destination[i] = Pattern.compile(source[i]);
		}
		return destination;
	}

}
