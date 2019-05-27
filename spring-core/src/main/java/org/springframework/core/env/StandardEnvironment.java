package org.springframework.core.env;

/**
 * {@link Environment}实现适用于'标准' (i.e. non-web)应用程序.
 *
 * <p>除了{@link ConfigurableEnvironment}的常用功能之外, 例如属性解析和与配置文件相关的操作,
 * 此实现还配置了两个默认属性源, 按以下顺序搜索:
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() 系统属性}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}
 * </ul>
 *
 * 也就是说, 如果key "xyz" 既存在于JVM系统属性中, 也存在于当前进程的环境变量集中,
 * 则系统属性中的key "xyz"的值将返回, 通过调用{@code environment.getProperty("xyz")}.
 * 默认情况下会选择此排序, 因为系统属性是per-JVM, 而给定系统上的许多JVM上的环境变量可能相同.
 * 赋予系统属性优先权, 允许基于每个JVM覆盖环境变量.
 *
 * <p>可以删除, 重新排序, 或替换这些默认属性源;
 * 可以使用{@link #getPropertySources()}中提供的{@link MutablePropertySources}实例添加其他属性源.
 * 有关用法示例, 请参阅{@link ConfigurableEnvironment} Javadoc.
 *
 * <p>有关shell环境(该环境禁止使用变量名称中的句点字符)中属性名称的特殊处理(e.g. Bash)的详细信息,
 * 请参阅{@link SystemEnvironmentPropertySource} javadoc.
 */
public class StandardEnvironment extends AbstractEnvironment {

	/** 系统环境属性源名称: {@value} */
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/** JVM系统属性属性源名称: {@value} */
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * 自定义适用于任何标准Java环境的属性源集:
	 * <ul>
	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}中的属性优先于
	 * {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}中的属性.
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(new MapPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
		propertySources.addLast(new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
	}

}
