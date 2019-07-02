package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @TestPropertySource}是一个类级注解, 用于配置属性文件的{@link #locations},
 * 并将内联的{@link #properties}添加到{@code Environment}的{@code PropertySources}集合中,
 * 用于集成测试的{@link org.springframework.context.ApplicationContext ApplicationContext}.
 *
 * <h3>优先权</h3>
 * <p>测试属性源的优先级高于从操作系统的环境, 或Java系统属性加载的属性源, 以及应用程序通过
 * {@link org.springframework.context.annotation.PropertySource @PropertySource}
 * 以编程方式声明性地添加的属性源
 * (e.g., 通过
 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}
 * 或其他方式).
 * 因此, 测试属性源可用于有选择地覆盖系统和应用程序属性源中定义的属性.
 * 此外, 内联{@link #properties}的优先级高于从资源{@link #locations}加载的属性.
 *
 * <h3>默认属性文件检测</h3>
 * <p>如果{@code @TestPropertySource}被声明为<em>空</em>注解
 * (i.e., 没有{@link #locations} 或 {@link #properties}的显式值),
 * 将尝试检测相对于声明注解的类的<em>默认</em>属性文件.
 * 例如, 如果带注解的测试类是{@code com.example.MyTest}, 则相应的默认属性文件是{@code "classpath:com/example/MyTest.properties"}.
 * 如果无法检测到默认值, 将抛出{@link IllegalStateException}.
 *
 * <h3>启用 &#064;TestPropertySource</h3>
 * <p>如果配置的{@linkplain ContextConfiguration#loader 上下文加载器}符合它, 则启用{@code @TestPropertySource}.
 * 每个{@code SmartContextLoader}都是
 * {@link org.springframework.test.context.support.AbstractGenericContextLoader AbstractGenericContextLoader}
 * 或{@link org.springframework.test.context.web.AbstractGenericWebContextLoader AbstractGenericWebContextLoader}的子类,
 * 它为{@code @TestPropertySource}提供自动支持, 这包括Spring TestContext Framework提供的每个{@code SmartContextLoader}.
 *
 * <h3>其它</h3>
 * <ul>
 * <li>通常, {@code @TestPropertySource}将与{@link ContextConfiguration @ContextConfiguration}一起使用.</li>
 * <li>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>;
 * 但是, 如果此注释和{@code @ContextConfiguration}组合在一个组合注释上, 则应该小心,
 * 因为两个注释的{@code locations}和{@code inheritLocations}属性在属性解析过程中可能导致歧义.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestPropertySource {

	/**
	 * {@link #locations}的别名.
	 * <p>此属性<strong>不</strong>可以与{@link #locations}一起使用, 但可以<em>代替</em> {@link #locations}.
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 要加载到{@code Environment}的{@code PropertySources}集合的属性文件的资源位置.
	 * 每个位置将按照声明的顺序添加到封闭的{@code Environment}作为其自己的属性源.
	 * <h3>支持的文件格式</h3>
	 * <p>支持传统和基于XML的属性文件格式 &mdash;
	 * 例如, {@code "classpath:/com/example/test.properties"}或{@code "file:/path/to/file.xml"}.
	 * <h3>路径资源语义</h3>
	 * <p>每个路径都将被解释为Spring {@link org.springframework.core.io.Resource Resource}.
	 * 普通路径&mdash; 例如, {@code "test.properties"} &mdash; 将被视为<em>相对</em>于定义测试类的包的类路径资源.
	 * 以斜杠开头的路径将被视为<em>绝对</em>类路径资源, 例如: {@code "/org/example/test.xml"}.
	 * 将使用指定的资源协议加载引用URL的路径(e.g., 以
	 * {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:},
	 * {@code http:}为前缀的路径.).
	 * 不允许使用资源位置通配符 (e.g. <code>*&#42;/*.properties</code>):
	 * 每个位置必须只评估一个{@code .properties}或{@code .xml}资源.
	 * 路径中的属性占位符 (i.e., <code>${...}</code>)将针对{@code Environment}
	 * {@linkplain org.springframework.core.env.Environment#resolveRequiredPlaceholders(String) 解析}.
	 * <h3>默认属性文件检测</h3>
	 * <p>有关检测默认值的讨论, 请参阅类级Javadoc.
	 * <h3>优先级</h3>
	 * <p>从资源位置加载的属性优先于内联{@link #properties}.
	 * <p>此属性<strong>不</strong>可以与{@link #value}一起使用, 但可以<em>代替</em> {@link #value}.
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * 是否应<em>继承</em>来自超类的测试属性源{@link #locations}.
	 * <p>默认{@code true}, 这意味着测试类将<em>继承</em>由超类定义的属性源位置.
	 * 具体而言, 测试类的属性源位置将附加到由超类定义的属性源位置列表中.
	 * 因此, 子类可以选择<em>扩展</em>测试属性源位置列表.
	 * <p>如果{@code inheritLocations}设置为{@code false}, 则测试类的属性源位置将<em>shadow</em>,
	 * 并有效替换由超类定义的任何属性源位置.
	 * <p>在以下示例中, {@code BaseTest}的{@code ApplicationContext}将仅使用{@code "base.properties"}文件作为测试属性源加载.
	 * 相反, {@code ExtendedTest}的{@code ApplicationContext}将使用{@code "base.properties"}
	 * <strong>和</strong> {@code "extended.properties"}文件作为测试属性源位置加载.
	 * <pre class="code">
	 * &#064;TestPropertySource(&quot;base.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *   // ...
	 * }
	 *
	 * &#064;TestPropertySource(&quot;extended.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *   // ...
	 * }
	 * </pre>
	 */
	boolean inheritLocations() default true;

	/**
	 * <em>内联属性</em>以<em>键值对</em>的形式出现, 应该在加载{@code ApplicationContext}之前,
	 * 添加到Spring {@link org.springframework.core.env.Environment Environment}.
	 * 所有键值对将作为具有最高优先级的单个测试{@code PropertySource}添加到封闭的{@code Environment}中.
	 * <h3>支持的语法</h3>
	 * <p>键值对支持的语法与为Java
	 * {@linkplain java.util.Properties#load(java.io.Reader) 属性文件}中的条目定义的语法相同:
	 * <ul>
	 * <li>{@code "key=value"}</li>
	 * <li>{@code "key:value"}</li>
	 * <li>{@code "key value"}</li>
	 * </ul>
	 * <h3>优先级</h3>
	 * <p>通过此属性声明的属性具有比从资源{@link #locations}加载的属性更高的优先级.
	 * <p>此属性可与{@link #value} <em>或</em> {@link #locations}结合使用.
	 */
	String[] properties() default {};

	/**
	 * 是否应<em>继承</em>来自超类的内联测试{@link #properties}.
	 * <p>默认{@code true}, 这意味着测试类将<em>继承</em>由超类定义的内联属性.
	 * 具体来说, 测试类的内联属性将附加到超类定义的内联属性列表中.
	 * 因此, 子类可以选择<em>扩展</em>内联测试属性列表.
	 * <p>如果{@code inheritProperties}设置为{@code false}, 则测试类的内联属性将<em>shadow</em>,
	 * 并有效替换由超类定义的任何内联属性.
	 * <p>在以下示例中, 将仅使用内联的{@code key1}属性加载{@code BaseTest}的{@code ApplicationContext}.
	 * 相反, {@code ExtendedTest}的{@code ApplicationContext}将使用内联的{@code key1} <strong>和</strong> {@code key2}属性加载.
	 * <pre class="code">
	 * &#064;TestPropertySource(properties = &quot;key1 = value1&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *   // ...
	 * }
	 * &#064;TestPropertySource(properties = &quot;key2 = value2&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *   // ...
	 * }
	 * </pre>
	 */
	boolean inheritProperties() default true;

}
