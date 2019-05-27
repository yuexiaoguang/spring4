package org.springframework.expression.spel;

import org.springframework.core.SpringProperties;

/**
 * SpEL表达式解析器的配置对象.
 */
public class SpelParserConfiguration {

	private static final SpelCompilerMode defaultCompilerMode;

	static {
		String compilerMode = SpringProperties.getProperty("spring.expression.compiler.mode");
		defaultCompilerMode = (compilerMode != null ?
				SpelCompilerMode.valueOf(compilerMode.toUpperCase()) : SpelCompilerMode.OFF);
	}


	private final SpelCompilerMode compilerMode;

	private final ClassLoader compilerClassLoader;

	private final boolean autoGrowNullReferences;

	private final boolean autoGrowCollections;

	private final int maximumAutoGrowSize;


	/**
	 * 使用默认的设置.
	 */
	public SpelParserConfiguration() {
		this(null, null, false, false, Integer.MAX_VALUE);
	}

	/**
	 * @param compilerMode 解析器的编译器模式
	 * @param compilerClassLoader 用作表达式编译基础的ClassLoader
	 */
	public SpelParserConfiguration(SpelCompilerMode compilerMode, ClassLoader compilerClassLoader) {
		this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
	}

	/**
	 * @param autoGrowNullReferences 如果null引用应该自动增长
	 * @param autoGrowCollections 如果集合应该自动增长
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
	}

	/**
	 * @param autoGrowNullReferences 如果null引用应该自动增长
	 * @param autoGrowCollections 如果集合应该自动增长
	 * @param maximumAutoGrowSize 集合可以自动增长的最大大小
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
	}

	/**
	 * @param compilerMode 解析器的编译器模式
	 * @param compilerClassLoader 用作表达式编译基础的ClassLoader
	 * @param autoGrowNullReferences 如果null引用应该自动增长
	 * @param autoGrowCollections 如果集合应该自动增长
	 * @param maximumAutoGrowSize 集合可以自动增长的最大大小
	 */
	public SpelParserConfiguration(SpelCompilerMode compilerMode, ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

		this.compilerMode = (compilerMode != null ? compilerMode : defaultCompilerMode);
		this.compilerClassLoader = compilerClassLoader;
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
	}


	/**
	 * 返回使用此配置对象的解析器的配置模式.
	 */
	public SpelCompilerMode getCompilerMode() {
		return this.compilerMode;
	}

	/**
	 * 返回用作表达式编译的基础的ClassLoader.
	 */
	public ClassLoader getCompilerClassLoader() {
		return this.compilerClassLoader;
	}

	/**
	 * 如果{@code null}引用应该自动增长, 则返回{@code true}.
	 */
	public boolean isAutoGrowNullReferences() {
		return this.autoGrowNullReferences;
	}

	/**
	 * 如果集合应该自动增长, 则返回{@code true}.
	 */
	public boolean isAutoGrowCollections() {
		return this.autoGrowCollections;
	}

	/**
	 * 返回集合可以自动增长的最大大小.
	 */
	public int getMaximumAutoGrowSize() {
		return this.maximumAutoGrowSize;
	}
}
