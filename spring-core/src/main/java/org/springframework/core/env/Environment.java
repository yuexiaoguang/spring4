package org.springframework.core.env;

/**
 * 表示当前应用程序正在运行的环境的接口.
 * 应用程序环境的两个方面: <em>profiles</em> 和 <em>properties</em>.
 * 与属性访问相关的方法通过{@link PropertyResolver}超级接口公开.
 *
 * <p><em>profiles</em>是仅在给定配置文件<em>active</em>时才向容器注册的bean定义的命名逻辑组.
 * 可以将Bean分配给配置文件, 无论是以XML还是通过注解定义;
 * 有关语法详细信息, 请参阅spring-beans 3.1架构或{@link org.springframework.context.annotation.Profile @Profile}注解.
 * {@code Environment}对象与配置文件相关的角色是确定当前{@linkplain #getActiveProfiles active}的配置文件,
 * 以及默认情况下应该{@linkplain #getDefaultProfiles active}的配置文件.
 *
 * <p><em>Properties</em>在几乎所有应用程序中都发挥着重要作用, 并且可能源自各种来源:
 * 属性文件, JVM 系统属性, 系统环境变量, JNDI, servlet上下文参数, ad-hoc 属性对象, Map, 等.
 * 与属性相关的环境对象的作用是为用户提供方便的服务接口, 用于配置属性源并从中解析属性.
 *
 * <p>在{@code ApplicationContext}中管理的Bean可以注册为{@link org.springframework.context.EnvironmentAware EnvironmentAware}
 * 或{@code @Inject} {@code Environment}以便查询配置文件状态或直接解析属性.
 *
 * <p>但是, 在大多数情况下, 应用程序级bean不需要直接与{@code Environment}交互,
 * 而是可能必须将{@code ${...}}属性值替换为属性占位符配置器,
 * 例如
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer PropertySourcesPlaceholderConfigurer},
 * 当使用{@code <context:property-placeholder/>}时, 它本身就是{@code EnvironmentAware}, 并且在Spring 3.1中默认注册.
 *
 * <p>必须通过{@code ConfigurableEnvironment}接口完成环境对象的配置,
 * 该接口从所有{@code AbstractApplicationContext}子类{@code getEnvironment()}方法返回.
 * 请参阅{@link ConfigurableEnvironment} Javadoc, 了解在应用程序上下文{@code refresh()}之前演示属性源操作的用法示例.
 */
public interface Environment extends PropertyResolver {

	/**
	 * 返回显式为此环境激活的配置文件集.
	 * 配置文件用于创建要有条件地注册的bean定义的逻辑分组, 例如基于部署环境.
	 * 可以通过将{@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME "spring.profiles.active"}设置为系统属性,
	 * 或通过调用{@link ConfigurableEnvironment#setActiveProfiles(String...)}来激活配置文件.
	 * <p>如果没有明确指定激活的配置文件, 那么将自动激活{@linkplain #getDefaultProfiles() 默认配置文件}.
	 */
	String[] getActiveProfiles();

	/**
	 * 如果未明确设置激活的配置文件, 则默认情况下返回要激活的配置文件集.
	 */
	String[] getDefaultProfiles();

	/**
	 * 返回一个或多个给定配置文件是否处于激活状态, 或者在没有显式激活的配置文件的情况下,
	 * 返回一个或多个给定配置文件是否包含在默认配置文件集中.
	 * 如果配置文件以 '!'开头, 逻辑被反转, i.e. 如果给定的配置文件<em>不是</em>激活的, 则该方法将返回true.
	 * 例如, <pre class="code">env.acceptsProfiles("p1", "!p2")</pre>将返回 {@code true},
	 * 如果配置文件'p1'激活, 或'p2'未激活.
	 * 
	 * @throws IllegalArgumentException 如果使用零参数调用或者profiles都是{@code null}, 则为空或只有空格
	 */
	boolean acceptsProfiles(String... profiles);

}
