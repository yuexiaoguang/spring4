package org.springframework.core.env;

/**
 * 用于解析任何底层源的属性的接口.
 */
public interface PropertyResolver {

	/**
	 * 返回给定属性键是否可用于解析, i.e. 给定键的值是否不是 {@code null}.
	 */
	boolean containsProperty(String key);

	/**
	 * 返回与给定键关联的属性值, 或{@code null} 如果无法解析键.
	 * 
	 * @param key 要解析的属性名称
	 */
	String getProperty(String key);

	/**
	 * 返回与给定键关联的属性值, 或{@code defaultValue} 如果无法解析键.
	 * 
	 * @param key 要解析的属性名称
	 * @param defaultValue 如果未找到, 要返回的默认值
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * 返回与给定键关联的属性值, 或{@code null} 如果无法解析键.
	 * 
	 * @param key 要解析的属性名称
	 * @param targetType 属性值的类型
	 */
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * 返回与给定键关联的属性值, 或{@code defaultValue} 如果无法解析键.
	 * 
	 * @param key 要解析的属性名称
	 * @param targetType 属性值的类型
	 * @param defaultValue 如果未找到, 要返回的默认值
	 */
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * 如果键无法解析, 请将与给定键关联的属性值转换为{@code T}或{@code null}类型的{@code Class}.
	 * 
	 * @throws org.springframework.core.convert.ConversionException
	 * 如果无法找到或加载由属性值指定的类, 或者无法从属性值指定的类分配targetType
	 * @deprecated as of 4.3, in favor of {@link #getProperty} with manual conversion
	 * to {@code Class} via the application's {@code ClassLoader}
	 */
	@Deprecated
	<T> Class<T> getPropertyAsClass(String key, Class<T> targetType);

	/**
	 * 返回与给定键关联的属性值 (never {@code null}).
	 * 
	 * @throws IllegalStateException 如果键无法解析
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * 返回与给定键关联的属性值, 转换为给定的targetType (never {@code null}).
	 * 
	 * @throws IllegalStateException 如果键无法解析
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * 解析给定文本中的${...} 占位符, 将其替换为{@link #getProperty}解析的相应属性值.
	 * 没有默认值的无法解析的占位符将被忽略, 并传递原值.
	 * 
	 * @param text 要解析的文本
	 * 
	 * @return 解析后的字符串 (never {@code null})
	 * @throws IllegalArgumentException 如果给定文本是{@code null}
	 */
	String resolvePlaceholders(String text);

	/**
	 * 解析给定文本中的${...} 占位符, 将其替换为{@link #getProperty}解析的相应属性值.
	 * 没有默认值的无法解析的占位符将抛出IllegalArgumentException.
	 * 
	 * @return 解析后的字符串 (never {@code null})
	 * @throws IllegalArgumentException 如果给定的文本是{@code null}, 或者占位符是不可解析的
	 */
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
