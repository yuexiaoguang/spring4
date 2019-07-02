package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ContextHierarchy}是一个类级注解, 用于为集成测试定义
 * {@link org.springframework.context.ApplicationContext ApplicationContexts}的层次结构.
 *
 * <h3>Examples</h3>
 * <p>以下基于JUnit的示例演示了需要使用上下文层次结构的集成测试的常见配置方案.
 *
 * <h4>具有上下文层次结构的单个测试类</h4>
 * <p>{@code ControllerIntegrationTests}通过声明由两个级别组成的上下文层次结构表示Spring MVC Web应用程序的典型集成测试场景,
 * 一个用于<em>root</em> {@code WebApplicationContext} (使用{@code TestAppConfig}),
 * 一个用于<em>dispatcher servlet</em> {@code WebApplicationContext} (带有{@code WebConfig}).
 * <em>自动装配</em>到测试实例中的{@code WebApplicationContext}是子上下文的一个 (i.e., 层次结构中最低的上下文).
 *
 * <pre class="code">
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * &#064;WebAppConfiguration
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(classes = TestAppConfig.class),
 *     &#064;ContextConfiguration(classes = WebConfig.class)
 * })
 * public class ControllerIntegrationTests {
 *
 *     &#064;Autowired
 *     private WebApplicationContext wac;
 *
 *     // ...
 * }</pre>
 *
 * <h4>具有隐式父上下文的类层次结构</h4>
 * <p>以下测试类定义测试类层次结构中的上下文层次结构.
 * {@code AbstractWebTests}在Spring驱动的Web应用程序中声明根{@code WebApplicationContext}的配置.
 * 但请注意, {@code AbstractWebTests}未声明{@code @ContextHierarchy};
 * 因此, {@code AbstractWebTests}的子类可以选择参与上下文层次结构或遵循{@code @ContextConfiguration}的标准语义.
 * {@code SoapWebServiceTests}和{@code RestWebServiceTests}都扩展{@code AbstractWebTests},
 * 并通过{@code @ContextHierarchy}定义上下文层次结构.
 * 结果是将加载三个应用程序上下文 (一个用于{@code @ContextConfiguration}的每个声明,
 * 并且基于{@code AbstractWebTests}中的配置加载的应用程序上下文将被设置为每个加载的具体子类的上下文的父上下文.
 *
 * <pre class="code">
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * &#064;WebAppConfiguration
 * &#064;ContextConfiguration("file:src/main/webapp/WEB-INF/applicationContext.xml")
 * public abstract class AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/spring/soap-ws-config.xml")
 * public class SoapWebServiceTests extends AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/spring/rest-ws-config.xml")
 * public class RestWebServiceTests extends AbstractWebTests {}</pre>
 *
 * <h4>具有合并上下文层次结构配置的类层次结构</h4>
 * <p>以下类演示如何使用<em>命名的</em>层次结构级别来<em>合并</em>上下文层次结构中特定级别的配置.
 * {@code BaseTests}在层次结构中定义了两个级别, {@code parent} 和 {@code child}.
 * {@code ExtendedTests}扩展{@code BaseTests}并指示Spring TestContext Framework合并{@code child}层次结构级别的上下文配置,
 * 只需确保通过{@link ContextConfiguration#name}声明的名称都是 {@code "child"}.
 * 结果是将加载三个应用程序上下文:
 * 一个用于{@code "/app-config.xml"}, 一个用于{@code "/user-config.xml"}, 一个用于<code>{"/user-config.xml", "/order-config.xml"}</code>.
 * 与前面的示例一样, 从{@code "/app-config.xml"}加载的应用程序上下文
 * 将被设置为从{@code "/user-config.xml"} 和 <code>{"/user-config.xml", "/order-config.xml"}</code>加载的上下文的父上下文.
 *
 * <pre class="code">
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 *     &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 *     &#064;ContextConfiguration(name = "child",  locations = "/order-config.xml")
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <h4>具有重写上下文层次结构配置的类层次结构</h4>
 * <p>与前面的示例相比, 此示例演示了如何通过将{@link ContextConfiguration#inheritLocations}标志设置为{@code false}
 * 来<em>覆盖</em>上下文层次结构中给定命名级别的配置.
 * 因此, {@code ExtendedTests}的应用程序上下文将仅从{@code "/test-user-config.xml"}加载,
 * 并将其父级设置为从{@code "/app-config.xml"}加载上下文.
 *
 * <pre class="code">
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 *     &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 *     &#064;ContextConfiguration(name = "child",  locations = "/test-user-config.xml", inheritLocations=false)
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextHierarchy {

	/**
	 * {@link ContextConfiguration @ContextConfiguration}实例列表, 其中每个实例定义上下文层次结构中的级别.
	 * <p>如果需要合并或覆盖测试类层次结构中给定级别的上下文层次结构的配置,
	 * 必须通过在类层次结构中的每个级别为{@code @ContextConfiguration}中的
	 * {@link ContextConfiguration#name name}属性提供相同的值来明确命名该级别.
	 * 有关示例, 请参阅类级Javadoc.
	 */
	ContextConfiguration[] value();

}
