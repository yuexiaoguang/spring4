package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 测试注解, 指示是否为特定测试配置文件启用或禁用测试.
 *
 * <p>在此注解的上下文中, 术语<em>profile</em>默认情况下是指Java系统属性;
 * 但是, 可以通过实现自定义{@link ProfileValueSource}来更改语义.
 * 如果配置的{@code ProfileValueSource}为声明的{@link #name}返回匹配的{@link #value}, 则将启用测试.
 * 否则, 测试将被禁用并有效<em>忽略</em>.
 *
 * <p>{@code @IfProfileValue}可以在类级别, 方法级别或两者中应用.
 * {@code @IfProfileValue}的类级别使用优先于该类或其子类中的任何方法的方法级别使用.
 * 具体而言, 如果在方法级别<em>和</em>类级别上启用了测试, 则启用测试; 没有{@code @IfProfileValue}意味着隐式启用了测试.
 * 这类似于JUnit的{@link org.junit.Ignore @Ignore}注解的语义, 除了{@code @Ignore}的存在总是禁用测试.
 *
 * <h3>Example</h3>
 * 使用{@link SystemProfileValueSource}作为{@code ProfileValueSource}实现(默认情况下配置)时,
 * 可以将测试方法配置为仅在Oracle的Java VM上运行, 如下所示:
 *
 * <pre class="code">
 * &#064;IfProfileValue(name = &quot;java.vendor&quot;, value = &quot;Oracle Corporation&quot;)
 * public void testSomething() {
 *     // ...
 * }</pre>
 *
 * <h3>'OR'语义</h3>
 * <p>也可以使用<em>OR</em>语义为多个{@link #values}配置{@code @IfProfileValue}.
 * 如果已为{@code "test-groups"}配置文件正确配置了{@code ProfileValueSource},
 * 且值为{@code unit-tests} <em>或</em> {@code integration-tests}, 则将启用以下测试.
 * 此功能类似于TestNG对测试<em>组</em> 的支持, 以及JUnit对测试<em>类别</em>的实验性支持.
 *
 * <pre class="code">
 * &#064;IfProfileValue(name = &quot;test-groups&quot;, values = { &quot;unit-tests&quot;, &quot;integration-tests&quot; })
 * public void testWhichRunsForUnitOrIntegrationTestGroups() {
 *     // ...
 * }</pre>
 *
 * <h3>{@code @IfProfileValue} vs. {@code @Profile}</h3>
 * <p>虽然{@code @IfProfileValue}和{@link org.springframework.context.annotation.Profile @Profile}注解
 * 都涉及<em>profiles</em>, 但它们并不直接相关.
 * {@code @Profile}涉及在{@link org.springframework.core.env.Environment Environment}中配置的bean定义配置文件;
 * 而{@code @IfProfileValue}用于启用或禁用测试.
 *
 * <h3>元注解支持</h3>
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface IfProfileValue {

	/**
	 * 要测试的<em>配置文件值</em>的{@code name}.
	 */
	String name();

	/**
	 * 给定{@link #name}的<em>配置文件值</em>的单个允许{@code value}.
	 * <p>Note: 给{@code #value}和{@link #values}都分配值将导致配置冲突.
	 */
	String value() default "";

	/**
	 * 给定{@link #name}的<em>配置文件值</em>的所有允许的{@code values}.
	 * <p>Note: 给{@code #value}和{@link #values}都分配值将导致配置冲突.
	 */
	String[] values() default {};

}
