package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.annotation.AliasFor;

/**
 * 表示方法将生成由Spring容器管理的bean.
 *
 * <h3>Overview</h3>
 *
 * <p>此注解的属性的名称和语义类似于Spring XML模式中的 {@code <bean/>}元素的名称和语义.
 * For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // 实例化和配置MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Bean Names</h3>
 *
 * <p>虽然{@link #name}属性可用, 但确定bean名称的默认策略是使用 {@code @Bean}方法的名称.
 * 这很方便直观, 但如果需要显式命名, 可以使用{@code name}属性 (或其别名 {@code value}).
 * 另请注意, {@code name} 接受一个字符串数组, 允许单个bean使用多个名称 (即主bean名称加上一个或多个别名).
 *
 * <pre class="code">
 *     &#064;Bean({"b1", "b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Profile, Scope, Lazy, DependsOn, Primary, Order</h3>
 *
 * <p>请注意, {@code @Bean}注解不提供 profile, scope, lazy, depends-on, primary属性.
 * 相反, 它应该与 {@link Scope @Scope}, {@link Lazy @Lazy}, {@link DependsOn @DependsOn},
 * {@link Primary @Primary}注解一起使用来声明这些语义.
 * For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Profile("production")
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * 上述注解的语义与它们在组件类级别的使用相匹配:
 * {@code Profile} 允许选择性地包含某些bean.
 * {@code @Scope} 将bean的范围从singleton更改为指定的范围.
 * {@code @Lazy} 只有在默认单例范围的情况下才有实际作用.
 * {@code @DependsOn} 在创建此bean之前强制创建特定的其他bean,
 * 除了bean通过直接引用表达的依赖项之外, 这些依赖项通常对单例启动有帮助.
 * {@code @Primary}是一种解决注入点级别歧义的机制, 如果需要注入单个目标组件, 但是有多个bean按类型匹配.
 *
 * <p>此外, {@code @Bean} 方法还可以声明限定符注解和 {@link org.springframework.core.annotation.Order @Order} 值,
 * 在注入点解析期间要考虑, 就像对应的组件类上的相应注解一样, 但可能是每个bean定义非常独立
 * (在具有相同bean类的多个定义的情况下).
 * 限定符在初始类型匹配后缩小候选者范围;
 * 在收集注入点的情况下, 顺序值确定已解析元素的顺序 (有多个目标bean按类型和限定符匹配).
 *
 * <p><b>NOTE:</b> {@code @Order}值可能会影响注入点的优先级,
 * 但请注意它们不会影响单例启动顺序, 启动顺序是由依赖关系和{@code @DependsOn}声明确定的, 如上所述.
 * 此外, {@link javax.annotation.Priority} 在此级别不可用, 因为它无法在方法上声明;
 * 它的语义可以通过{@code @Order}值和{@code @Primary}在每个类型的单例bean上建模.
 *
 * <h3>{@code @Configuration}类中的{@code @Bean}方法</h3>
 *
 * <p>通常, {@code @Bean} 方法在{@code @Configuration}类中声明.
 * 在这种情况下, bean方法可以通过直接调用它们来引用同一类中的其他{@code @Bean}方法.
 * 这可确保bean之间的引用是强类型和可导航的.
 * 这种所谓的<em>'inter-bean references'</em>保证尊重范围和AOP语义, 就像{@code getBean()}查找一样.
 * 这些是原始的'Spring JavaConfig'项目中已知的语义, 它需要在运行时对每个这样的配置类进行CGLIB子类化.
 * 因此, {@code @Configuration}类及其工厂方法在此模式下不得标记为final或private.
 * For example:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>{@code @Bean} <em>Lite</em> Mode</h3>
 *
 * <p>{@code @Bean}方法也可以在未使用{@code @Configuration}注解的类中声明.
 * 例如, bean方法可以在{@code @Component}类中声明, 甚至可以在<em>普通旧类</em>中声明.
 * 在这种情况下, {@code @Bean}方法将在所谓的<em>'lite'</em>模式下处理.
 *
 * <p><em>lite</em>模式中的Bean方法将被容器视为普通的<em>工厂方法</em> (类似于XML中的 {@code factory-method}声明),
 * 并且正确应用范围和生命周期回调.
 * 在这种情况下, 包含类保持不变, 并且包含类或工厂方法没有不同寻常的约束.
 *
 * <p>与{@code @Configuration}类中的bean方法的语义相反, <em>lite</em>模式不支持<em>'inter-bean references'</em>.
 * 相反, 当一个{@code @Bean}方法在<em>lite</em>模式中调用另一个 {@code @Bean}方法时, 调用是标准的Java方法调用;
 * Spring不会通过CGLIB代理拦截调用.
 * 这类似于代理模式下的 inter-{@code @Transactional}方法调用, Spring不拦截调用 &mdash;
 * Spring仅在AspectJ模式下执行此操作.
 *
 * <p>For example:
 *
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <h3>Bootstrapping</h3>
 *
 * <p>有关更多详细信息, 请参阅 @{@link Configuration} javadoc,
 * 包括如何使用 {@link AnnotationConfigApplicationContext}和朋友引导容器.
 *
 * <h3>{@code BeanFactoryPostProcessor}-returning {@code @Bean} methods</h3>
 *
 * <p>必须特别考虑返回
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor} ({@code BFPP})类型的{@code @Bean}方法.
 * 因为{@code BFPP}对象必须在容器生命周期的早期实例化,
 * 它们可能会干扰{@code @Configuration}类中{@code @Autowired}, {@code @Value}, {@code @PostConstruct}注解的处理.
 * 要避免这些生命周期问题, 请将{@code BFPP}-returning {@code @Bean}方法标记为 {@code static}.
 * For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertySourcesPlaceholderConfigurer pspc() {
 *         // instantiate, configure and return pspc ...
 *     }
 * </pre>
 *
 * 通过将此方法标记为 {@code static}, 可以调用它而不会导致其声明 {@code @Configuration}类的实例化, 从而避免上述生命周期冲突.
 * 请注意, {@code static} {@code @Bean}方法不会针对作用域和AOP语义进行增强.
 * 这在{@code BFPP}情况下有效, 因为它们通常不被其他 {@code @Bean}方法引用.
 * 提醒一下, 将为任何具有可分配给{@code BeanFactoryPostProcessor}的返回类型的非静态{@code @Bean}方法发出WARN级别的日志消息.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

	/**
	 * {@link #name}的别名.
	 * <p>在不需要其他属性时使用, 例如: {@code @Bean("customBeanName")}.
	 */
	@AliasFor("name")
	String[] value() default {};

	/**
	 * 此bean的名称, 或多个名称, 主bean名称加别名.
	 * <p>如果未指定, 则bean的名称是被注解的方法的名称.
	 * 如果指定, 则忽略方法名称.
	 * <p>如果没有声明其他属性, 也可以通过{@link #value}属性配置bean名称和别名.
	 */
	@AliasFor("value")
	String[] name() default {};

	/**
	 * 是否通过名称或类型通过基于约定的自动装配注入依赖项?
	 * <p>请注意, 此autowire模式只是外部驱动的基于bean属性setter方法的自动装配, 类似于XML bean定义.
	 * <p>默认模式允许注解驱动的自动装配.
	 * "no" 仅指外部驱动的自动装配, 不影响bean类本身通过注解表达的自动装配要求.
	 */
	Autowire autowire() default Autowire.NO;

	/**
	 * 初始化期间在bean实例上调用的方法的可选名称.
	 * 不常用, 因为可以在被注解的Bean方法的主体内直接以编程方式调用该方法.
	 * <p>默认值为 {@code ""}, 表示没有要调用的init方法.
	 */
	String initMethod() default "";

	/**
	 * 关闭应用程序上下文时, 在bean实例上调用的方法的可选名称,
	 * 例如JDBC {@code DataSource}实现上的{@code close()}方法, 或Hibernate {@code SessionFactory}对象.
	 * 该方法必须没有参数, 但可能抛出任何异常.
	 * <p>为了方便用户, 容器将尝试针对从{@code @Bean}方法返回的对象推断出destroy方法.
	 * 例如, 给定的{@code @Bean}方法返回 Apache Commons DBCP {@code BasicDataSource},
	 * 容器会注意到该对象上可用的{@code close()}方法, 并自动将其注册为 {@code destroyMethod}.
	 * 这种'destroy 方法推断'目前仅限于检测名为'close' 或 'shutdown'的 public, 非arg方法.
	 * 该方法可以在继承层次结构的任何级别声明, 并且无论 {@code @Bean}方法的返回类型如何都将被检测到
	 * (i.e., 检测在创建时反映出bean实例本身).
	 * <p>要禁用特定{@code @Bean}的destroy方法推断, 指定一个空字符串作为值, e.g. {@code @Bean(destroyMethod="")}.
	 * 请注意, 仍会检测到{@link org.springframework.beans.factory.DisposableBean}回调接口, 并调用相应的destroy方法:
	 * 换句话说, {@code destroyMethod=""} 仅影响自定义 close/shutdown 方法和 {@link java.io.Closeable}/{@link java.lang.AutoCloseable}声明的close方法.
	 * <p>Note: 仅在生命周期完全由工厂控制的bean上调用, 对于单例来说总是如此, 但对于其他范围则不能保证.
	 */
	String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

}
