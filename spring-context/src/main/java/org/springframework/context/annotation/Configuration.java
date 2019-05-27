package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 表示一个类声明了一个或多个{@link Bean @Bean}方法, 并且可以由Spring容器处理, 以便在运行时为这些bean生成bean定义和服务请求,
 * 例如:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate, configure and return bean ...
 *     }
 * }</pre>
 *
 * <h2>Bootstrapping {@code @Configuration} classes</h2>
 *
 * <h3>通过{@code AnnotationConfigApplicationContext}</h3>
 *
 * {@code @Configuration}类通常使用{@link AnnotationConfigApplicationContext}或其支持Web的变体进行引导,
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext}.
 * 以下是前者的简单示例:
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(AppConfig.class);
 * ctx.refresh();
 * MyBean myBean = ctx.getBean(MyBean.class);
 * // use myBean ...
 * </pre>
 *
 * 有关更多详细信息, 请参阅{@link AnnotationConfigApplicationContext} Javadoc,
 * 有关{@code web.xml}配置说明, 请参阅
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext}.
 *
 * <h3>通过Spring {@code <beans>} XML</h3>
 *
 * <p>作为直接针对{@code AnnotationConfigApplicationContext}注册{@code @Configuration}类的替代方法,
 * {@code @Configuration}类可以在Spring XML文件中声明为普通的{@code <bean>}定义:
 * <pre class="code">
 * {@code
 * <beans>
 *    <context:annotation-config/>
 *    <bean class="com.acme.AppConfig"/>
 * </beans>}</pre>
 *
 * 在上面的示例中, 需要{@code <context:annotation-config/>} 以启用 {@link ConfigurationClassPostProcessor}和其他与注解相关的后处理器,
 * 以便于处理{@code @Configuration}类.
 *
 * <h3>通过组件扫描</h3>
 *
 * <p>{@code @Configuration}使用{@link Component @Component}元注解,
 * 因此{@code @Configuration}类是组件扫描的候选者 (通常使用Spring XML的 {@code <context:component-scan/>}元素)
 * 因此也可以像任何常规{@code @Component}一样利用 {@link Autowired @Autowired}/{@link javax.inject.Inject @Inject}.
 * 特别是, 如果存在单个构造函数, 则将以透明方式应用自动装配语义:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *     private final SomeBean someBean;
 *
 *     public AppConfig(SomeBean someBean) {
 *         this.someBean = someBean;
 *     }
 *
 *     // &#064;Bean definition using "SomeBean"
 *
 * }</pre>
 *
 * <p>{@code @Configuration}类不仅可以使用组件扫描进行自我引导,
 * 还可以使用{@link ComponentScan @ComponentScan}注解自己<em>配置</em>组件扫描:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.acme.app.services")
 * public class AppConfig {
 *     // various &#064;Bean definitions ...
 * }</pre>
 *
 * 有关详细信息, 请参阅{@link ComponentScan @ComponentScan} javadoc.
 *
 * <h2>使用外部值</h2>
 *
 * <h3>使用{@code Environment} API</h3>
 *
 * 可以通过将Spring {@link org.springframework.core.env.Environment}注入到{@code @Configuration}类来查找外部值
 * (e.g. 使用{@code @Autowired}注解):
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064Autowired Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         MyBean myBean = new MyBean();
 *         myBean.setName(env.getProperty("bean.name"));
 *         return myBean;
 *     }
 * }</pre>
 *
 * 通过{@code Environment}解析的属性驻留在一个或多个 "属性源"对象中,
 * 而且{@code @Configuration}类可以使用
 * {@link org.springframework.core.env.PropertySources @PropertySources}注解向{@code Environment}对象提供属性源:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064Inject Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(env.getProperty("bean.name"));
 *     }
 * }</pre>
 *
 * 有关详细信息, 请参阅{@link org.springframework.core.env.Environment Environment}
 * 和{@link PropertySource @PropertySource} Javadoc.
 *
 * <h3>使用{@code @Value}注解</h3>
 *
 * 外部值可以使用{@link Value @Value}注解'连接到' {@code @Configuration}类:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064Value("${bean.name}") String beanName;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(beanName);
 *     }
 * }</pre>
 *
 * 使用Spring的
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer PropertySourcesPlaceholderConfigurer}时,
 * 此方法最有用, 通常使用{@code <context:property-placeholder/>}通过XML启用.
 * 请参阅以下部分, 使用{@code @ImportResource}引入Spring XML组成{@code @Configuration}类,
 * 请参阅{@link Value @Value} Javadoc, {@link Bean @Bean} Javadoc, 了解有关使用{@code BeanFactoryPostProcessor}类型的详细信息,
 * 例如{@code PropertySourcesPlaceholderConfigurer}.
 *
 * <h2>组成{@code @Configuration}类</h2>
 *
 * <h3>使用{@code @Import}注解</h3>
 *
 * <p>可以使用{@link Import @Import}注解来组成{@code @Configuration}类, 这与{@code <import>}在Spring XML中的工作方式不同.
 * 因为{@code @Configuration}对象在容器中作为Spring bean进行管理, 所以可以按照常规方式注入导入的配置 (e.g. 通过构造函数注入):
 *
 * <pre class="code">
 * &#064;Configuration
 * public class DatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return DataSource
 *     }
 * }
 *
 * &#064;Configuration
 * &#064;Import(DatabaseConfig.class)
 * public class AppConfig {
 *
 *     private final DatabaseConfig dataConfig;
 *
 *     public AppConfig(DatabaseConfig dataConfig) {
 *         this.dataConfig = dataConfig;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // reference the dataSource() bean method
 *         return new MyBean(dataConfig.dataSource());
 *     }
 * }</pre>
 *
 * 现在{@code AppConfig}和导入的{@code DatabaseConfig}都可以通过仅针对Spring上下文注册 {@code AppConfig}来引导:
 *
 * <pre class="code">
 * new AnnotationConfigApplicationContext(AppConfig.class);</pre>
 *
 * <h3>使用 {@code @Profile}注解</h3>
 *
 * {@code @Configuration}类可以使用{@link Profile @Profile}注解进行标记,
 * 以指示只有在给定的 profile或 profiles是<em>有效</em>时才应处理这些内容:
 *
 * <pre class="code">
 * &#064;Profile("development")
 * &#064;Configuration
 * public class EmbeddedDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return embedded DataSource
 *     }
 * }
 *
 * &#064;Profile("production")
 * &#064;Configuration
 * public class ProductionDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return production DataSource
 *     }
 * }</pre>
 *
 * 或者也可以在{@code @Bean}方法级别声明profile条件, e.g. 对于同一配置类中的替代bean变体:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class ProfileDatabaseConfig {
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("development")
 *     public DataSource embeddedDatabase() { ... }
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("production")
 *     public DataSource productionDatabase() { ... }
 * }</pre>
 *
 * 有关详细信息, 请参阅{@link Profile @Profile}和{@link org.springframework.core.env.Environment} javadocs.
 *
 * <h3>使用{@code @ImportResource}注解导入Spring XML</h3>
 *
 * 如上所述, {@code @Configuration}类可以在Spring XML文件中声明为常规Spring {@code <bean>}定义.
 * 也可以使用{@link ImportResource @ImportResource}注解将Spring XML配置文件导入{@code @Configuration}类.
 * 从XML导入的Bean定义可以用普通的方式注入 (e.g. 使用{@code Inject} 注解):
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ImportResource("classpath:/com/acme/database-config.xml")
 * public class AppConfig {
 *
 *     &#064Inject DataSource dataSource; // from XML
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // inject the XML-defined dataSource bean
 *         return new MyBean(this.dataSource);
 *     }
 * }</pre>
 *
 * <h3>使用嵌套的{@code @Configuration}类</h3>
 *
 * 类可以如下嵌套在另一个类中:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(dataSource);
 *     }
 *
 *     &#064;Configuration
 *     static class DatabaseConfig {
 *         &#064;Bean
 *         DataSource dataSource() {
 *             return new EmbeddedDatabaseBuilder().build();
 *         }
 *     }
 * }</pre>
 *
 * 在引导这样安排时, 只需要在应用程序上下文中注册{@code AppConfig}.
 * 由于是一个嵌套的{@code @Configuration}类, {@code DatabaseConfig} <em>将自动注册</em>.
 * 当{@code AppConfig} {@code DatabaseConfig}之间的关系已经隐含清楚时, 这就避免了使用{@code @Import}注解的必要.
 *
 * <p>另请注意, 嵌套的{@code @Configuration}类可用于{@code @Profile}注解,
 * 以便为封闭的{@code @Configuration}类提供相同bean的两个选项.
 *
 * <h2>配置延迟初始化</h2>
 *
 * <p>默认情况下, {@code @Bean}方法将在容器引导时 <em>实时实例化</em>.
 * 为了避免这种情况, {@code @Configuration}可以与{@link Lazy @Lazy}注解一起使用,
 * 以指示在类中声明的所有{@code @Bean}方法默认延迟初始化.
 * 请注意, {@code @Lazy}也可用于单个{@code @Bean}方法.
 *
 * <h2>测试对{@code @Configuration}类的支持</h2>
 *
 * {@code spring-test}模块中提供的Spring <em>TestContext框架</em>提供了 {@code @ContextConfiguration}注解,
 * 从Spring 3.1开始, 它可以接受{@code @Configuration} {@code Class}对象的数组:
 *
 * <pre class="code">
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * &#064;ContextConfiguration(classes={AppConfig.class, DatabaseConfig.class})
 * public class MyTests {
 *
 *     &#064;Autowired MyBean myBean;
 *
 *     &#064;Autowired DataSource dataSource;
 *
 *     &#064;Test
 *     public void test() {
 *         // assertions against myBean ...
 *     }
 * }</pre>
 *
 * 有关详细信息, 请参阅TestContext框架参考文档.
 *
 * <h2>使用{@code @Enable}注解启用内置Spring功能</h2>
 *
 * 可以使用各自的"{@code @Enable}"注解从{@code @Configuration}类启用和配置Spring
 * 异步方法执行, 定时任务执行, 注解驱动事务管理甚至Spring MVC等Spring功能.
 * See
 * {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync},
 * {@link org.springframework.scheduling.annotation.EnableScheduling @EnableScheduling},
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement},
 * {@link org.springframework.context.annotation.EnableAspectJAutoProxy @EnableAspectJAutoProxy},
 * and {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}
 * for details.
 *
 * <h2>创建{@code @Configuration}类时的约束</h2>
 *
 * <ul>
 * <li>必须以类的形式提供配置类 (i.e. 不是从工厂方法返回的实例), 允许通过生成的子类进行运行时增强.
 * <li>配置类必须是非 final 的.
 * <li>配置类必须是非本地的 (i.e. 可能无法在方法中声明).
 * <li>必须将任何嵌套的配置类声明为{@code static}.
 * <li>{@code @Bean}方法可能不会反过来创建更多配置类 (任何此类实例都将被视为常规bean, 其配置注解仍未被检测到).
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

	/**
	 * 显式指定与此Configuration类关联的Spring bean定义的名称.
	 * 如果未指定 (常见情况), 将自动生成bean名称.
	 * <p>仅当通过组件扫描获取Configuration类或直接提供给{@link AnnotationConfigApplicationContext}时, 自定义名称才适用.
	 * 如果将Configuration类注册为传统的XML bean定义, 则bean元素的 name/id 优先.
	 * 
	 * @return 建议的组件名称 (或空字符串)
	 */
	String value() default "";

}
