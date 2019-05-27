package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 抽象基本正则表达式切点bean. JavaBean属性是:
 * <ul>
 * <li>pattern: 要匹配的完全限定方法名称的正则表达式.
 * 确切的regexp语法将取决于子类 (e.g. Perl5正则表达式)
 * <li>patterns: 采用String数组模式的替代属性. 结果将是这些模式的结合.
 * </ul>
 *
 * <p>Note: 正则表达式必须匹配. 例如,
 * {@code .*get.*}将匹配 com.mycom.Foo.getBar().
 * {@code get.*}则不会.
 *
 * <p>此基类是可序列化的. 子类应声明所有字段 transient;
 * 在反序列化时将再次调用{@link #initPatternRepresentation}方法.
 */
@SuppressWarnings("serial")
public abstract class AbstractRegexpMethodPointcut extends StaticMethodMatcherPointcut
		implements Serializable {

	/**
	 * 匹配模式的正则表达式.
	 */
	private String[] patterns = new String[0];

	/**
	 * 排除模式的正则表达式.
	 */
	private String[] excludedPatterns = new String[0];


	/**
	 * 当我们只有一个模式时的便捷方法.
	 * 使用这个方法或 {@link #setPatterns}, 而不是两个都用.
	 */
	public void setPattern(String pattern) {
		setPatterns(pattern);
	}

	/**
	 * 设置正则表达式定义要匹配的方法.
	 * 匹配将是所有这些的结合; 如果有任何匹配, 切点匹配.
	 */
	public void setPatterns(String... patterns) {
		Assert.notEmpty(patterns, "'patterns' must not be empty");
		this.patterns = new String[patterns.length];
		for (int i = 0; i < patterns.length; i++) {
			this.patterns[i] = StringUtils.trimWhitespace(patterns[i]);
		}
		initPatternRepresentation(this.patterns);
	}

	/**
	 * 返回方法匹配的正则表达式.
	 */
	public String[] getPatterns() {
		return this.patterns;
	}

	/**
	 * 当我们只有一个排除模式时的便捷方法.
	 * 使用这个方法或 {@link #setExcludedPatterns}, 而不是两个都用.
	 */
	public void setExcludedPattern(String excludedPattern) {
		setExcludedPatterns(excludedPattern);
	}

	/**
	 * 设置正则表达式定义方法以匹配排除.
	 * 匹配将是所有这些的结合; 如果有任何匹配, 切点匹配.
	 */
	public void setExcludedPatterns(String... excludedPatterns) {
		Assert.notEmpty(excludedPatterns, "'excludedPatterns' must not be empty");
		this.excludedPatterns = new String[excludedPatterns.length];
		for (int i = 0; i < excludedPatterns.length; i++) {
			this.excludedPatterns[i] = StringUtils.trimWhitespace(excludedPatterns[i]);
		}
		initExcludedPatternRepresentation(this.excludedPatterns);
	}

	/**
	 * 返回排除匹配的正则表达式.
	 */
	public String[] getExcludedPatterns() {
		return this.excludedPatterns;
	}


	/**
	 * 尝试将正则表达式与目标类的完全限定名称以及方法的声明类加上方法的名称进行匹配.
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return ((targetClass != null && targetClass != method.getDeclaringClass() &&
				matchesPattern(ClassUtils.getQualifiedMethodName(method, targetClass))) ||
				matchesPattern(ClassUtils.getQualifiedMethodName(method, method.getDeclaringClass())));
	}

	/**
	 * 将指定的候选项与配置的模式匹配.
	 * 
	 * @param signatureString "java.lang.Object.hashCode"格式的签名
	 * 
	 * @return 候选人是否匹配至少一个指定的模式
	 */
	protected boolean matchesPattern(String signatureString) {
		for (int i = 0; i < this.patterns.length; i++) {
			boolean matched = matches(signatureString, i);
			if (matched) {
				for (int j = 0; j < this.excludedPatterns.length; j++) {
					boolean excluded = matchesExclusion(signatureString, j);
					if (excluded) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}


	/**
	 * 子类必须实现此操作以初始化regexp切点.
	 * 可以多次调用.
	 * <p>将从{@link #setPatterns}方法以及反序列化调用此方法.
	 * 
	 * @param patterns 要初始化的模式
	 * 
	 * @throws IllegalArgumentException 如果模式无效
	 */
	protected abstract void initPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * 子类必须实现此操作以初始化regexp切点.
	 * 可以多次调用.
	 * <p>将从 {@link #setExcludedPatterns}方法以及反序列化调用此方法.
	 * 
	 * @param patterns 要初始化的模式
	 * 
	 * @throws IllegalArgumentException 如果模式无效
	 */
	protected abstract void initExcludedPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * 给定索引处的模式是否与给定的String匹配?
	 * 
	 * @param pattern 要匹配的{@code String}模式
	 * @param patternIndex 模式的索引 (从 0 开始)
	 * 
	 * @return {@code true} 如果匹配, 否则{@code false}
	 */
	protected abstract boolean matches(String pattern, int patternIndex);

	/**
	 * 给定索引处的排除模式是否与给定的String匹配?
	 * 
	 * @param pattern 要匹配的{@code String}模式
	 * @param patternIndex 模式的索引 (从 0 开始)
	 * 
	 * @return {@code true} 如果匹配, 否则{@code false}
	 */
	protected abstract boolean matchesExclusion(String pattern, int patternIndex);


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractRegexpMethodPointcut)) {
			return false;
		}
		AbstractRegexpMethodPointcut otherPointcut = (AbstractRegexpMethodPointcut) other;
		return (Arrays.equals(this.patterns, otherPointcut.patterns) &&
				Arrays.equals(this.excludedPatterns, otherPointcut.excludedPatterns));
	}

	@Override
	public int hashCode() {
		int result = 27;
		for (String pattern : this.patterns) {
			result = 13 * result + pattern.hashCode();
		}
		for (String excludedPattern : this.excludedPatterns) {
			result = 13 * result + excludedPattern.hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": patterns " + ObjectUtils.nullSafeToString(this.patterns) +
				", excluded patterns " + ObjectUtils.nullSafeToString(this.excludedPatterns);
	}

}
