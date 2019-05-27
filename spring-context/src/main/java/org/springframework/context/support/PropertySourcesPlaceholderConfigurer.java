package org.springframework.context.support;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

/**
 * {@link PlaceholderConfigurerSupport}的专业化,
 * 解析当前Spring {@link Environment}及其{@link PropertySources}的bean定义属性值和{@code @Value}注解中的 ${...}占位符.
 *
 * <p>此类被设计为Spring 3.1中引入的{@code PropertyPlaceholderConfigurer}的替代.
 * 默认情况下, 它用于支持{@code property-placeholder}元素在spring-context-3.1或更高版本的XSD中可用,
 * 而spring-context 版本 &lt;= 3.0 默认为 {@code PropertyPlaceholderConfigurer}以确保向后兼容性.
 * 有关完整的详细信息, 请参阅spring-context XSD文档.
 *
 * <p>任何本地属性 (e.g. 通过{@link #setProperties}, {@link #setLocations}等添加的属性) 都将添加为{@code PropertySource}.
 * 本地属性的搜索优先级基于 {@link #setLocalOverride localOverride}属性的值, 默认情况下为{@code false},
 * 表示在所有环境属性源之后最后搜索本地属性.
 *
 * <p>有关操作环境属性源的详细信息, 请参阅{@link org.springframework.core.env.ConfigurableEnvironment}和相关的javadoc.
 */
public class PropertySourcesPlaceholderConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

	/**
	 * {@value}是提供给此配置程序的{@linkplain #mergeProperties() 合并属性}的{@link PropertySource}的名称.
	 */
	public static final String LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME = "localProperties";

	/**
	 * {@value} 是{@link PropertySource}的名称, 它包装了提供给此配置程序的{@linkplain #setEnvironment environment}.
	 */
	public static final String ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME = "environmentProperties";


	private MutablePropertySources propertySources;

	private PropertySources appliedPropertySources;

	private Environment environment;


	/**
	 * 自定义此配置器使用的{@link PropertySources}.
	 * <p>设置此属性表示应忽略环境属性源和本地属性.
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = new MutablePropertySources(propertySources);
	}

	/**
	 * 在替换${...}占位符时, 将搜索来自给定{@link Environment}的{@code PropertySources}.
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}


	/**
	 * 通过根据此配置器的{@link PropertySources}集合解析每个占位符, 替换bean定义中${...}占位符, 其中包括:
	 * <ul>
	 * <li>所有的{@linkplain org.springframework.core.env.ConfigurableEnvironment#getPropertySources 环境属性源},
	 * 如果{@code Environment} {@linkplain #setEnvironment} 存在
	 * <li>{@linkplain #mergeProperties 合并的本地属性}, 如果已经指定了{@linkplain #setLocation}
	 * {@linkplain #setLocations} {@linkplain #setProperties} {@linkplain #setPropertiesArray}
	 * <li>通过调用{@link #setPropertySources}设置的任何属性源
	 * </ul>
	 * <p>如果调用了{@link #setPropertySources}, <strong>环境和本地属性将被忽略</strong>.
	 * 此方法旨在为用户提供对属性源的细粒度控制, 一旦设置, 配置器不会对添加其他源进行任何假设.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertySources == null) {
			this.propertySources = new MutablePropertySources();
			if (this.environment != null) {
				this.propertySources.addLast(
					new PropertySource<Environment>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
						@Override
						public String getProperty(String key) {
							return this.source.getProperty(key);
						}
					}
				);
			}
			try {
				PropertySource<?> localPropertySource =
						new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, mergeProperties());
				if (this.localOverride) {
					this.propertySources.addFirst(localPropertySource);
				}
				else {
					this.propertySources.addLast(localPropertySource);
				}
			}
			catch (IOException ex) {
				throw new BeanInitializationException("Could not load properties", ex);
			}
		}

		processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
		this.appliedPropertySources = this.propertySources;
	}

	/**
	 * 访问给定bean工厂中的每个bean定义, 并尝试使用给定属性中的值替换 ${...}属性占位符.
	 */
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			final ConfigurablePropertyResolver propertyResolver) throws BeansException {

		propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
		propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
		propertyResolver.setValueSeparator(this.valueSeparator);

		StringValueResolver valueResolver = new StringValueResolver() {
			@Override
			public String resolveStringValue(String strVal) {
				String resolved = (ignoreUnresolvablePlaceholders ?
						propertyResolver.resolvePlaceholders(strVal) :
						propertyResolver.resolveRequiredPlaceholders(strVal));
				if (trimValues) {
					resolved = resolved.trim();
				}
				return (resolved.equals(nullValue) ? null : resolved);
			}
		};

		doProcessProperties(beanFactoryToProcess, valueResolver);
	}

	/**
	 * 已实现与{@link org.springframework.beans.factory.config.PlaceholderConfigurerSupport}的兼容性.
	 * 
	 * @throws UnsupportedOperationException in this implementation
	 * @deprecated in favor of
	 * {@link #processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver)}
	 */
	@Override
	@Deprecated
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
		throw new UnsupportedOperationException(
				"Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
	}

	/**
	 * 返回在{@link #postProcessBeanFactory(ConfigurableListableBeanFactory) post-processing}后处理期间实际应用的属性源.
	 * 
	 * @return 应用的属性源
	 * @throws IllegalStateException 如果尚未应用属性源
	 */
	public PropertySources getAppliedPropertySources() throws IllegalStateException {
		Assert.state(this.appliedPropertySources != null, "PropertySources have not yet been applied");
		return this.appliedPropertySources;
	}

}
