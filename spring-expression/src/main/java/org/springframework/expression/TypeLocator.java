package org.springframework.expression;

/**
 * 期望该接口的实现者能够定位类型.
 * 他们可能会使用自定义的 {@link ClassLoader}和/或处理常见的包前缀 (e.g. {@code java.lang}).
 *
 * <p>有关示例实现, 请参阅{@link org.springframework.expression.spel.support.StandardTypeLocator}.
 */
public interface TypeLocator {

	/**
	 * 按名称查找类型. 该名称可能完全限定, 也可能不完全限定 (e.g. {@code String} 或 {@code java.lang.String}).
	 * 
	 * @param typeName 要定位的类型
	 * 
	 * @return 表示该类型的{@code Class}对象
	 * @throws EvaluationException 如果查找类型时有问题
	 */
	Class<?> findType(String typeName) throws EvaluationException;

}
