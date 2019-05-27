package org.springframework.core.env;

import java.util.Map;

/**
 * 由大多数(不是全部){@link Environment}类型实现的配置接口.
 * 提供用于设置活动的和默认的配置文件以及操作底层属性源的工具.
 * 允许客户端通过{@link ConfigurablePropertyResolver}超接口设置和验证所需的属性, 自定义转换服务等.
 *
 * <h2>操作属性源</h2>
 * <p>可以删除, 重新排序或替换属性源;
 * 可以使用从{@link #getPropertySources()}返回的{@link MutablePropertySources}实例添加其他属性源.
 * 以下示例针对{@code ConfigurableEnvironment}的{@link StandardEnvironment}实现,
 * 但通常适用于任何实现, 但特定的默认属性源可能不同.
 *
 * <h4>Example: 添加具有最高搜索优先级的新属性源</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map<String, String> myMap = new HashMap<String, String>();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>Example: 删除默认的系统属性属性源</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>Example: 模拟系统环境以进行测试</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 *
 * {@code ApplicationContext}正在使用{@link Environment}时,
 * 重要的是要执行任何此类{@code PropertySource}操作
 * 在调用上下文的
 * {@link org.springframework.context.support.AbstractApplicationContext#refresh() refresh()}<em>之前</em>.
 * 这可确保在容器引导过程中所有属性源都可用,
 * 包括{@linkplain org.springframework.context.support.PropertySourcesPlaceholderConfigurer 属性占位符配置器}的使用.
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * 指定为此{@code Environment}激活的配置文件集.
	 * 在容器引导期间评估配置文件, 以确定是否应该在容器中注册bean定义.
	 * <p>任何现有的活动配置文件都将替换为给定的参数;
	 * 使用零参数调用以清除当前的活动配置文件集.
	 * 使用{@link #addActiveProfile}添加配置文件, 同时保留现有集合.
	 * 
	 * @throws IllegalArgumentException 如果有配置文件为null, 空或仅空格
	 */
	void setActiveProfiles(String... profiles);

	/**
	 * 将配置文件添加到当前活动的配置文件集中.
	 * 
	 * @throws IllegalArgumentException 如果有配置文件为null, 空或仅空格
	 */
	void addActiveProfile(String profile);

	/**
	 * 如果通过{@link #setActiveProfiles}没有显式激活其他配置文件, 则指定默认情况下要激活的配置文件集.
	 * 
	 * @throws IllegalArgumentException 如果有配置文件为null, 空或仅空格
	 */
	void setDefaultProfiles(String... profiles);

	/**
	 * 以可变形式返回此{@code Environment}的{@link PropertySources},
	 * 允许操作在解析此{@code Environment}对象的属性时搜索{@link PropertySource}对象集.
	 * 各种{@link MutablePropertySources}方法, 如
	 * {@link MutablePropertySources#addFirst addFirst},
	 * {@link MutablePropertySources#addLast addLast},
	 * {@link MutablePropertySources#addBefore addBefore}和
	 * {@link MutablePropertySources#addAfter addAfter}允许对属性源排序进行细粒度控制.
	 * 例如, 这有助于确保某些用户定义的属性源具有优先于默认属性源的搜索优先级, 例如系统属性集或系统环境变量集.
	 */
	MutablePropertySources getPropertySources();

	/**
	 * 如果当前{@link SecurityManager}允许, 则返回{@link System#getenv()}的值,
	 * 否则返回一个Map实现, 该实现将尝试使用对{@link System#getenv(String)}的调用来访问各个键.
	 * <p>请注意, 大多数{@link Environment}实现都将此系统环境Map作为要搜索的默认{@link PropertySource}.
	 * 因此, 建议不要直接使用此方法, 除非明确地绕过其他属性源.
	 * <p>在返回的Map上调用{@link Map#get(Object)}, 将永远不会抛出{@link IllegalAccessException};
	 * 如果SecurityManager禁止访问属性, 将返回{@code null}并发出INFO级别的日志消息, 指出异常.
	 */
	Map<String, Object> getSystemEnvironment();

	/**
	 * 如果当前{@link SecurityManager}允许, 则返回{@link System#getProperties()}的值,
	 * 否则返回一个Map实现, 该实现将尝试使用对{@link System#getProperty(String)}的调用来访问各个键.
	 * <p>请注意, 大多数{@code Environment}实现将此系统属性Map作为要搜索的默认{@link PropertySource}.
	 * 因此, 建议不要直接使用此方法, 除非明确地绕过其他属性源.
	 * <p>在返回的Map上调用{@link Map#get(Object)}将永远不会抛出{@link IllegalAccessException};
	 * 如果SecurityManager禁止访问属性, 将返回{@code null}并发出INFO级别的日志消息, 指出异常.
	 */
	Map<String, Object> getSystemProperties();

	/**
	 * 将给定父环境的活动配置文件, 默认配置文件和属性源附加到此 (子) 环境的每个相应的集合.
	 * <p>对于父和子中同时存在的任何具有相同名称的{@code PropertySource}实例, 将保留子实例并丢弃父实例.
	 * 这具有允许子级覆盖属性源, 以及避免通过公共属性源类型的冗余搜索的效果, e.g. 系统环境和系统属性.
	 * <p>还会对活动的和默认的配置文件名称进行重复过滤, 以避免混淆和冗余存储.
	 * <p>在任何情况下, 父环境都保持不变.
	 * 请注意, 在调用{@code merge}之后发生的对父环境的任何更改都不会反映在子级中.
	 * 因此, 在调用{@code merge}之前, 应注意配置父属性源和配置文件信息.
	 * 
	 * @param parent 要合并的环境
	 */
	void merge(ConfigurableEnvironment parent);

}
