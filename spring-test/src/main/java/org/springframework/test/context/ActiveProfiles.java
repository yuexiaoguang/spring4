package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code ActiveProfiles}是一个类级注解, 用于声明在为测试类加载
 * {@link org.springframework.context.ApplicationContext ApplicationContext}时,
 * 应使用哪些<em>活动的bean定义配置文件</em>.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ActiveProfiles {

	/**
	 * {@link #profiles}的别名.
	 * <p>此属性<strong>不</strong>可以与{@link #profiles}一起使用, 但可以<em>代替</em>{@link #profiles}.
	 */
	@AliasFor("profiles")
	String[] value() default {};

	/**
	 * 要激活的bean定义配置文件.
	 * <p>此属性<strong>不</strong>可以与{@link #value}一起使用, 但可以<em>代替</em> {@link #value}.
	 */
	@AliasFor("value")
	String[] profiles() default {};

	/**
	 * 用于以编程方式解析活动Bean定义配置文件的{@link ActiveProfilesResolver}的类型.
	 */
	Class<? extends ActiveProfilesResolver> resolver() default ActiveProfilesResolver.class;

	/**
	 * 是否<em>继承</em>超类的bean定义配置文件.
	 * <p>默认{@code true}, 这意味着测试类将<em>>继承</em>由测试超类定义的 bean定义配置文件.
	 * 具体来说, 测试类的bean定义配置文件将附加到测试超类定义的bean定义配置文件列表中.
	 * 因此, 子类可以选择<em>扩展</em> bean定义配置文件列表.
	 * <p>如果{@code inheritProfiles}设置为{@code false}, 则测试类的bean定义配置文件将<em>shadow</em>,
	 * 并有效替换由超类定义的任何bean定义配置文件.
	 * <p>在以下示例中, {@code BaseTest}的{@code ApplicationContext}将仅使用 &quot;base&quot; bean定义配置文件加载;
	 * 在&quot;extended&quot;配置文件中定义的bean不会加载.
	 * 相反, {@code ExtendedTest}的{@code ApplicationContext}将使用&quot;base&quot; 
	 * <strong>和</strong> &quot;extended&quot; bean定义配置文件.
	 * <pre class="code">
	 * &#064;ActiveProfiles(&quot;base&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ActiveProfiles(&quot;extended&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * <p>Note: 从基于路径的资源位置或带注释的类加载{@code ApplicationContext}时, 可以使用{@code @ActiveProfiles}.
	 */
	boolean inheritProfiles() default true;

}
