package org.springframework.beans.factory.config;

import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.core.Constants;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;

/**
 * {@link PlaceholderConfigurerSupport}子类,
 * 针对{@link #setLocation local} {@link #setProperties properties}和/或系统属性和环境变量解析${...}占位符.
 *
 * <p>从Spring 3.1开始, {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer PropertySourcesPlaceholderConfigurer}
 * 应优先用于此实现;
 * 通过利用Spring 3.1中提供的{@link org.springframework.core.env.Environment Environment}
 * 和{@link org.springframework.core.env.PropertySource PropertySource}机制, 它更加灵活.
 *
 * <p>{@link PropertyPlaceholderConfigurer}仍然适合使用:
 * <ul>
 * <li>{@code spring-context}模块不可用 (i.e., 使用Spring的{@code BeanFactory} API, 而不是{@code ApplicationContext}).
 * <li>现有配置使用 {@link #setSystemPropertiesMode(int) "systemPropertiesMode"}
 * 和/或 {@link #setSystemPropertiesModeName(String) "systemPropertiesModeName"}属性.
 * 建议用户不要使用这些设置, 而是通过容器的{@code Environment}配置属性源搜索顺序;
 * 但是, 继续使用 {@code PropertyPlaceholderConfigurer}可以保持功能的准确保存.
 * </ul>
 *
 * <p>在Spring 3.1之前, {@code <context:property-placeholder/>}命名空间元素注册了{@code PropertyPlaceholderConfigurer}的实例.
 * 如果使用命名空间的 {@code spring-context-3.0.xsd}定义, 它仍然会这样做.
 * 也就是说, 即使使用Spring 3.1, 也可以通过命名空间保留{@code PropertyPlaceholderConfigurer}的注册;
 * 只是不更新​​ {@code xsi:schemaLocation}并继续使用3.0 XSD.
 */
public class PropertyPlaceholderConfigurer extends PlaceholderConfigurerSupport {

	/** 不要检查系统属性. */
	public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

	/**
	 * 如果在指定的属性中无法解析, 请检查系统属性.
	 * 这是默认的.
	 */
	public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

	/**
	 * 在尝试指定的属性之前, 首先检查系统属性.
	 * 这允许系统属性覆盖任何其他属性源.
	 */
	public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;


	private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

	private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

	private boolean searchSystemEnvironment =
			!SpringProperties.getFlag(AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME);


	/**
	 * 通过相应常量的名称设置系统属性模式,
	 * e.g. "SYSTEM_PROPERTIES_MODE_OVERRIDE".
	 * 
	 * @param constantName 常量的名称
	 * 
	 * @throws java.lang.IllegalArgumentException 如果指定了无效常量
	 */
	public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
		this.systemPropertiesMode = constants.asNumber(constantName).intValue();
	}

	/**
	 * 设置如何检查系统属性: 回调, 覆盖, 或从不.
	 * 例如, 将 ${user.dir} 解析为"user.dir"系统属性.
	 * <p>默认是 "fallback": 如果无法解析具有指定属性的占位符, 则将尝试系统属性.
	 * "override" 在尝试指定的属性之前, 将首先检查系统属性.
	 * "never"根本不会检查系统属性.
	 */
	public void setSystemPropertiesMode(int systemPropertiesMode) {
		this.systemPropertiesMode = systemPropertiesMode;
	}

	/**
	 * 设置是否在未找到匹配的系统属性的情况下, 搜索匹配的系统环境变量.
	 * 仅在"systemPropertyMode"处于启用状态时应用 (i.e. "fallback" or "override"), 在检查JVM系统属性之后.
	 * <p>默认 "true". 关闭此设置以永远不会针对系统环境变量解析占位符.
	 * 请注意, 通常建议将外部值作为JVM系统属性传递:
	 * 这可以在启动脚本中轻松实现, 即使对于现有环境变量也是如此.
	 * <p><b>NOTE:</b> 在最终为Sun VM 1.5重新启用之前, 对已禁用相应的{@link System#getenv}支持的Sun VM 1.4无法访问环境变量.
	 * 如果需要依赖环境变量支持, 请升级到1.5(或更高).
	 */
	public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
		this.searchSystemEnvironment = searchSystemEnvironment;
	}

	/**
	 * 使用给定属性解析给定占位符, 根据给定模式执行系统属性检查.
	 * <p>默认实现在系统属性检查之前/之后委托给 {@code resolvePlaceholder (placeholder, props)}.
	 * <p>子类可以为自定义解析策略覆盖此选项, 包括系统属性检查的自定义点.
	 * 
	 * @param placeholder 要解析的占位符
	 * @param props 此配置器的合并的属性
	 * @param systemPropertiesMode 系统属性模式, 根据此类中的常量
	 * 
	 * @return 解析后的值, 或 null
	 */
	protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
		String propVal = null;
		if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
			propVal = resolveSystemProperty(placeholder);
		}
		if (propVal == null) {
			propVal = resolvePlaceholder(placeholder, props);
		}
		if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
			propVal = resolveSystemProperty(placeholder);
		}
		return propVal;
	}

	/**
	 * 使用给定的属性解析给定的占位符.
	 * 默认实现只是检查相应的属性键.
	 * <p>子类可以为自定义的placeholder-to-key映射或自定义解析策略覆盖它, 可能只使用给定的属性作为回调.
	 * <p>请注意, 根据系统属性模式, 在调用此方法之后, 仍将分别检查系统属性.
	 * 
	 * @param placeholder 要解析的占位符
	 * @param props 此配置器的合并的属性
	 * 
	 * @return 解析后的值, 或 {@code null}
	 */
	protected String resolvePlaceholder(String placeholder, Properties props) {
		return props.getProperty(placeholder);
	}

	/**
	 * 将给定key解析为JVM系统属性, 如果未找到匹配的系统属性, 还可以选择将其解析为系统环境变量.
	 * 
	 * @param key 要解析为系统属性键的占位符
	 * 
	 * @return 系统属性值, 或{@code null}
	 */
	protected String resolveSystemProperty(String key) {
		try {
			String value = System.getProperty(key);
			if (value == null && this.searchSystemEnvironment) {
				value = System.getenv(key);
			}
			return value;
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access system property '" + key + "': " + ex);
			}
			return null;
		}
	}


	/**
	 * 访问给定bean工厂中的每个bean定义, 并尝试使用给定属性中的值替换 ${...}属性占位符.
	 */
	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {

		StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);
		doProcessProperties(beanFactoryToProcess, valueResolver);
	}

	/**
	 * 解析占位符.
	 * 
	 * @param strVal 要解析的String值
	 * @param props 要解析占位符的属性
	 * @param visitedPlaceholders 在当前解析尝试期间已访问过的占位符 (在此版本的代码中被忽略)
	 * 
	 * @deprecated as of Spring 3.0, in favor of using {@link #resolvePlaceholder} with {@link org.springframework.util.PropertyPlaceholderHelper}.
	 * Only retained for compatibility with Spring 2.5 extensions.
	 */
	@Deprecated
	protected String parseStringValue(String strVal, Properties props, Set<?> visitedPlaceholders) {
		PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
				placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
		PlaceholderResolver resolver = new PropertyPlaceholderConfigurerResolver(props);
		return helper.replacePlaceholders(strVal, resolver);
	}


	private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

		private final PropertyPlaceholderHelper helper;

		private final PlaceholderResolver resolver;

		public PlaceholderResolvingStringValueResolver(Properties props) {
			this.helper = new PropertyPlaceholderHelper(
					placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
			this.resolver = new PropertyPlaceholderConfigurerResolver(props);
		}

		@Override
		public String resolveStringValue(String strVal) throws BeansException {
			String resolved = this.helper.replacePlaceholders(strVal, this.resolver);
			if (trimValues) {
				resolved = resolved.trim();
			}
			return (resolved.equals(nullValue) ? null : resolved);
		}
	}


	private class PropertyPlaceholderConfigurerResolver implements PlaceholderResolver {

		private final Properties props;

		private PropertyPlaceholderConfigurerResolver(Properties props) {
			this.props = props;
		}

		@Override
		public String resolvePlaceholder(String placeholderName) {
			return PropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName, props, systemPropertiesMode);
		}
	}

}
