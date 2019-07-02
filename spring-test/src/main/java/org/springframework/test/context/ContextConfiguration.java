package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AliasFor;

/**
 * {@code @ContextConfiguration}定义了类级元数据,
 * 用于确定如何加载和配置{@link org.springframework.context.ApplicationContext ApplicationContext}以进行集成测试.
 *
 * <h3>支持的资源类型</h3>
 *
 * <p>
 * 在Spring 3.1之前, 仅支持基于路径的资源位置 (通常是XML配置文件).
 * 从Spring 3.1开始, {@linkplain #loader context loaders}可能会选择支持基于路径的<em>或</em>基于类的资源.
 * 从Spring 4.0.4开始, {@linkplain #loader context loaders}可以选择同时支持基于路径的<em>和</em>基于类的资源.
 * 因此, {@code @ContextConfiguration}可用于声明基于路径的资源位置 (通过{@link #locations} 或{@link #value}属性)
 * <em>或</em>带注解的类 (通过{@link #classes}属性).
 * 但请注意, {@link SmartContextLoader}的大多数实现仅支持单一资源类型.
 * 从Spring 4.1开始, 基于路径的资源位置可以是XML配置文件或Groovy脚本 (如果Groovy在类路径上).
 * 当然, 第三方框架可以选择支持其他类型的基于路径的资源.
 *
 * <h3>带注解的类</h3>
 *
 * <p>
 * 术语<em>带注解的类</em>可以指以下任何一种.
 *
 * <ul>
 * <li>具有{@link org.springframework.context.annotation.Configuration @Configuration}注解的类</li>
 * <li>组件 (i.e., 具有
 * {@link org.springframework.stereotype.Component @Component},
 * {@link org.springframework.stereotype.Service @Service},
 * {@link org.springframework.stereotype.Repository @Repository}等注解的类.)</li>
 * <li>具有{@code javax.inject}注解符合JSR-330的类</li>
 * <li>包含{@link org.springframework.context.annotation.Bean @Bean}方法的任何其他类</li>
 * </ul>
 *
 * <p>
 * 有关<em>带注解的类</em>的配置和语义的更多信息, 请参阅
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * 和{@link org.springframework.context.annotation.Bean @Bean} Javadoc.
 *
 * <p>
 * 从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextConfiguration {

	/**
	 * {@link #locations}的别名.
	 * <p>此属性<strong>不</strong>可以与{@link #locations}一起使用, 但可以使用它来代替{@link #locations}.
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 用于加载
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}的资源位置.
	 * <p>查看用于
	 * {@link org.springframework.test.context.support.AbstractContextLoader#modifyLocations
	 * AbstractContextLoader.modifyLocations()}的Javadoc,
	 * 了解有关如何在运行时解释位置的详细信息, 特别是在相对路径的情况下.
	 * 另外, 请查看有关
	 * {@link org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations
	 * AbstractContextLoader.generateDefaultLocations()}的文档,
	 * 以获取有关在未指定任何内容时将使用的默认位置的详细信息.
	 * <p>请注意, 上述默认规则仅适用于标准的
	 * {@link org.springframework.test.context.support.AbstractContextLoader AbstractContextLoader}子类,
	 * 例如
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader}
	 * 或{@link org.springframework.test.context.support.GenericGroovyXmlContextLoader GenericGroovyXmlContextLoader},
	 * 如果配置了{@code locations}, 它们是运行时使用的有效默认实现.
	 * 有关默认加载器的更多详细信息, 请参阅{@link #loader}的文档.
	 * <p>此属性可以<strong>不</strong>与{@link #value}一起使用, 但可以使用它来代替{@link #value}.
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * 用于加载
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}的<em>带注解的类</em>.
	 * <p>查看用于
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader#detectDefaultConfigurationClasses
	 * AnnotationConfigContextLoader.detectDefaultConfigurationClasses()}的javadoc,
	 * 了解如果未指定<em>带注解的类</em>, 将如何检测默认配置类的详细信息.
	 * 有关默认加载器的更多详细信息, 请参阅{@link #loader}的文档.
	 */
	Class<?>[] classes() default {};

	/**
	 * 用于初始化{@link ConfigurableApplicationContext}的应用程序上下文<em>初始化器类</em>.
	 * <p>每个声明的初始器支持的具体{@code ConfigurableApplicationContext}类型
	 * 必须与正在使用的{@link SmartContextLoader}创建的{@code ApplicationContext}类型兼容.
	 * <p>{@code SmartContextLoader}实现通常会检测是否已实现Spring的{@link org.springframework.core.Ordered Ordered}接口,
	 * 或者是否存在 @{@link org.springframework.core.annotation.Order Order}注解,
	 * 并在调用它们之前相应地对实例进行排序.
	 */
	Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers() default {};

	/**
	 * 是否应<em>继承</em>来自测试超类的{@link #locations 资源位置}或<em>带注解的类</em>.
	 * <p>默认{@code true}.
	 * 这意味着带注解的类将<em>继承</em>由测试超类定义的资源位置或带注解的类.
	 * 具体而言, 给定测试类的资源位置或带注解的类, 将附加到由测试超类定义的资源位置或带注解的类的列表中.
	 * 因此, 子类可以选择<em>扩展</em>资源位置列表或带注解的类.
	 * <p>如果{@code inheritLocations}设置为{@code false}, 则带注解的类的资源位置或带注解的类将<em>shadow</em>,
	 * 并有效地替换由超类定义的任何资源位置或带注解的类.
	 * <p>在以下使用基于路径的资源位置的示例中, {@code ExtendedTest}的
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * 将从{@code "base-context.xml"} <strong>和</strong> {@code "extended-context.xml"}中加载, 按此顺序.
	 * 因此, {@code "extended-context.xml"}中定义的Bean可能会覆盖{@code "base-context.xml"}中定义的Bean.
	 * <pre class="code">
	 * &#064;ContextConfiguration("base-context.xml")
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration("extended-context.xml")
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * <p>同样, 在以下使用带注解的类的示例中, {@code ExtendedTest}的
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * 将从{@code BaseConfig} <strong>和</strong> {@code ExtendedConfig}配置类中按顺序加载.
	 * 因此, {@code ExtendedConfig}中定义的Bean可能会覆盖{@code BaseConfig}中定义的Bean.
	 * <pre class="code">
	 * &#064;ContextConfiguration(classes=BaseConfig.class)
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration(classes=ExtendedConfig.class)
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 */
	boolean inheritLocations() default true;

	/**
	 * 是否应该<em>继承</em>来自测试超类的{@linkplain #initializers 上下文初始化器}.
	 * <p>默认{@code true}.
	 * 这意味着带注解的类将<em>继承</em>由测试超类定义的应用程序上下文初始器.
	 * 具体而言, 给定测试类的初始化器将添加到由测试超类定义的初始化器中.
	 * 因此, 子类可以选择<em>扩展</em>初始化器集合.
	 * <p>如果{@code inheritInitializers}设置为{@code false}, 则带注解的类的初始化器将<em>shadow</em>,
	 * 并有效替换超类定义的任何初始化器.
	 * <p>在以下示例中, {@code ExtendedTest}的{@link org.springframework.context.ApplicationContext ApplicationContext}
	 *  将使用{@code BaseInitializer} <strong>和</strong> {@code ExtendedInitializer}进行初始化.
	 * 但请注意, 调用初始化器的顺序取决于它们是否实现了{@link org.springframework.core.Ordered Ordered}
	 * 或使用了{@link org.springframework.core.annotation.Order &#064;Order}注解.
	 * <pre class="code">
	 * &#064;ContextConfiguration(initializers = BaseInitializer.class)
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration(initializers = ExtendedInitializer.class)
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * @since 3.2
	 */
	boolean inheritInitializers() default true;

	/**
	 * 用于加载{@link org.springframework.context.ApplicationContext ApplicationContext}
	 * 的{@link SmartContextLoader} (或{@link ContextLoader})的类型.
	 * <p>如果未指定, 则加载器将从使用{@code @ContextConfiguration}注解的第一个超类继承, 并指定显式加载器.
	 * 如果层次结构中没有类指定显式加载器, 则将使用默认加载器.
	 * <p>在运行时选择的默认具体实现将是
	 * {@link org.springframework.test.context.support.DelegatingSmartContextLoader DelegatingSmartContextLoader}或
	 * {@link org.springframework.test.context.web.WebDelegatingSmartContextLoader WebDelegatingSmartContextLoader}
	 * 具体取决于是否存在
	 * {@link org.springframework.test.context.web.WebAppConfiguration &#064;WebAppConfiguration}.
	 * 有关各种具体{@code SmartContextLoaders}的默认行为的更多详细信息, 请查看用于
	 * {@link org.springframework.test.context.support.AbstractContextLoader AbstractContextLoader},
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader},
	 * {@link org.springframework.test.context.support.GenericGroovyXmlContextLoader GenericGroovyXmlContextLoader},
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader AnnotationConfigContextLoader},
	 * {@link org.springframework.test.context.web.GenericXmlWebContextLoader GenericXmlWebContextLoader},
	 * {@link org.springframework.test.context.web.GenericGroovyXmlWebContextLoader GenericGroovyXmlWebContextLoader},和
	 * {@link org.springframework.test.context.web.AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader}
	 * 的Javadoc.
	 */
	Class<? extends ContextLoader> loader() default ContextLoader.class;

	/**
	 * 此配置表示的上下文层次结构级别的名称.
	 * <p>如果未指定, 将根据层次结构中所有已声明的上下文中的数字级别推断名称.
	 * <p>此属性仅在使用{@code @ContextHierarchy}配置的测试类层次结构中使用时才适用,
	 * 在这种情况下, 该名称可用于<em>合并</em>或<em>覆盖</em>在超类中定义的层次结构级别中相同名称的配置.
	 * 有关详细信息, 请参阅{@link ContextHierarchy @ContextHierarchy}的Javadoc.
	 */
	String name() default "";

}
