package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.io.support.PropertySourceFactory;

/**
 * 注解提供了一种方便的声明性的机制,
 * 用于将{@link org.springframework.core.env.PropertySource PropertySource}
 * 添加到Spring的 {@link org.springframework.core.env.Environment Environment}.
 * 与 @{@link Configuration}类一起使用.
 *
 * <h3>用法示例</h3>
 *
 * <p>给定一个包含键/值对{@code testbean.name=myTestBean}的文件{@code app.properties},
 * 以下{@code @Configuration}类使用{@code @PropertySource}将{@code app.properties}提供给{@code Environment}的{@code PropertySources}集合.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * 请注意, {@code Environment}对象是{@link org.springframework.beans.factory.annotation.Autowired @Autowired}到配置类中,
 * 然后在填充{@code TestBean}对象时使用.
 * 鉴于上面的配置, 对{@code testBean.getName()}的调用将返回 "myTestBean".
 *
 * <h3>在{@code <bean>}和{@code @Value}注解中解析${...}占位符</h3>
 *
 * 为了使用来自{@code PropertySource}的属性解析{@code <bean>}定义或{@code @Value}注解中的${...}占位符,
 * 必须注册{@code PropertySourcesPlaceholderConfigurer}.
 * 在XML中使用{@code <context:property-placeholder>}时会自动发生这种情况,
 * 但在使用{@code @Configuration}类时必须使用{@code static} {@code @Bean}方法显式注册.
 * 有关详细信息和示例，请参阅@{@link Configuration}的“使用外部化值”部分,
 * 和@{@link Bean}的javadoc中的“关于BeanFactoryPostProcessor返回@Bean方法的说明”部分.
 *
 * <h3>在{@code @PropertySource}资源位置中解析${...}占位符</h3>
 *
 * {@code @PropertySource} {@linkplain #value() 资源位置}中的任何${...}占位符将根据已针对环境注册的属性源集合进行解析.
 * 例子:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * 假设 "my.placeholder" 存在于已注册的其中一个属性源中,
 * e.g. 系统属性或环境变量, 占位符将被解析为相应的值.
 * 如果没有, 则"default/path"将用作默认值. 表示默认值 (由冒号 ":"分隔) 是可选的.
 * 如果未指定默认值且无法解析属性, 则将抛出{@code IllegalArgumentException}.
 *
 * <h3>关于用@PropertySource重写属性的说明</h3>
 *
 * 如果给定的属性键存在于多个 {@code .properties}文件中, 则处理的最后一个{@code @PropertySource}注解将 '赢'并覆盖.
 *
 * 例如, 给定两个属性文件 {@code a.properties} 和 {@code b.properties},
 * 考虑以下两个使用{@code @PropertySource}注解引用它们的配置类:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * </pre>
 *
 * 覆盖顺序取决于这些类在应用程序上下文中注册的顺序.
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * </pre>
 *
 * 在上面的场景中, {@code b.properties}中的属性将覆盖{@code a.properties}中存在的重复项,
 * 因为{@code ConfigB}最后注册.
 *
 * <p>在某些情况下, 使用{@code @ProperySource}注解时, 严格控制属性源顺序可能是不可能或不切实际的.
 * 例如, 如果上面的{@code @Configuration}类是通过组件扫描注册的, 则排序很难预测.
 * 在这种情况下 - 如果覆盖很重要 - 建议用户回退到使用编程的PropertySource API.
 * See {@link org.springframework.core.env.ConfigurableEnvironment ConfigurableEnvironment}
 * and {@link org.springframework.core.env.MutablePropertySources MutablePropertySources}
 * javadocs for details.
 *
 * <p><b>NOTE: 根据Java 8约定, 此注解是可重复的.</b>
 * 但是, 所有这些{@code @PropertySource}注解都需要在同一级别声明:
 * 直接在配置类上或作为同一自定义注解中的元注解.
 * 不建议混合直接注解和元注解, 因为直接注解将有效地覆盖元注解.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

	/**
	 * 指示此属性源的名称. 如果省略, 将根据底层资源的描述生成名称.
	 */
	String name() default "";

	/**
	 * 指示要加载的属性文件的资源位置.
	 * 例如, {@code "classpath:/com/myco/app.properties"} 或 {@code "file:/path/to/file"}.
	 * <p>不允许使用资源位置通配符 (e.g. *&#42;/*.properties);
	 * 每个位置必须只评估到一个 {@code .properties}资源.
	 * <p>${...} 占位符将针对已在{@code Environment}注册的所有属性源解析.
	 * 有关示例, 请参阅上面的{@linkplain PropertySource}.
	 * <p>每个位置都将作为自己的属性源添加到封闭的 {@code Environment}中, 并按声明的顺序添加.
	 */
	String[] value();

	/**
	 * 指示是否应忽略找不到 {@link #value() 属性资源}的错误.
	 * <p>如果属性文件是完全可选的, 则{@code true}是合适的.
	 * 默认 {@code false}.
	 */
	boolean ignoreResourceNotFound() default false;

	/**
	 * 给定资源的特定字符编码, e.g. "UTF-8".
	 */
	String encoding() default "";

	/**
	 * 指定自定义 {@link PropertySourceFactory}.
	 * <p>默认情况下, 将使用标准资源文件的默认工厂.
	 */
	Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
