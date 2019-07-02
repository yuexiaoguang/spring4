package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code TestExecutionListeners}定义了类级元数据,
 * 用于配置{@link TestExecutionListener TestExecutionListeners}应该使用{@link TestContextManager}进行注册.
 *
 * <p>通常, {@code @TestExecutionListeners}将与{@link ContextConfiguration @ContextConfiguration}一起使用.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestExecutionListeners {

	/**
	 * {@link #listeners}的别名.
	 * <p>此属性<strong>不</strong>可以与{@link #listeners}一起使用, 但可以使用它来代替{@link #listeners}.
	 */
	@AliasFor("listeners")
	Class<? extends TestExecutionListener>[] value() default {};

	/**
	 * 使用{@link TestContextManager}注册的{@link TestExecutionListener TestExecutionListeners}.
	 * <p>此属性<strong>不</strong>可以与{@link #value}一起使用, 但可以使用它来代替{@link #value}.
	 */
	@AliasFor("value")
	Class<? extends TestExecutionListener>[] listeners() default {};

	/**
	 * 是否应继承父类的{@link #listeners TestExecutionListeners}.
	 * <p>默认{@code true}, 这意味着带注解的类将<em>继承</em>由带注解的超类定义的监听器.
	 * 具体而言, 带注解的类的监听器将附加到由带注解的超类定义的监听器列表中.
	 * 因此, 子类可以选择<em>扩展</em>监听器列表.
	 * 在以下示例中, {@code AbstractBaseTest}将使用{@code DependencyInjectionTestExecutionListener}
	 * 和{@code DirtiesContextTestExecutionListener}进行配置;
	 * 然而, {@code TransactionalTest}将按顺序配置
	 * {@code DependencyInjectionTestExecutionListener},
	 * {@code DirtiesContextTestExecutionListener}, <strong>和</strong>
	 * {@code TransactionalTestExecutionListener}.
	 * <pre class="code">
	 * &#064;TestExecutionListeners({
	 *     DependencyInjectionTestExecutionListener.class,
	 *     DirtiesContextTestExecutionListener.class
	 * })
	 * public abstract class AbstractBaseTest {
	 * 	 // ...
	 * }
	 *
	 * &#064;TestExecutionListeners(TransactionalTestExecutionListener.class)
	 * public class TransactionalTest extends AbstractBaseTest {
	 * 	 // ...
	 * }</pre>
	 * <p>如果{@code inheritListeners}设置为{@code false}, 则带注解的类的监听器将 <em>shadow</em>,
	 * 并有效地替换由超类定义的任何监听器.
	 */
	boolean inheritListeners() default true;

	/**
	 * 在不从超类继承监听器的类上声明{@code @TestExecutionListeners}时使用的<em>合并模式</em>.
	 * <p>可以设置为{@link MergeMode#MERGE_WITH_DEFAULTS MERGE_WITH_DEFAULTS}以使本地声明的监听器与默认监听器<em>合并</em>.
	 * <p>如果监听器是从超类继承的, 则忽略该模式.
	 * <p>默认为{@link MergeMode#REPLACE_DEFAULTS REPLACE_DEFAULTS}以实现向后兼容.
	 */
	MergeMode mergeMode() default MergeMode.REPLACE_DEFAULTS;


	/**
	 * <em>模式</em>的枚举, 指示在未从超类继承监听器的类上声明{@code @TestExecutionListeners}时,
	 * 是否将显式声明的监听器与默认监听器合并.
	 */
	enum MergeMode {

		/**
		 * 指示本地声明的监听器应替换默认监听器.
		 */
		REPLACE_DEFAULTS,

		/**
		 * 指示本地声明的监听器应与默认监听器合并.
		 * <p>合并算法确保从列表中删除重复项, 并根据
		 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator AnnotationAwareOrderComparator}
		 * 的语义对生成的合并监听器集合进行排序.
		 * 如果监听器实现{@link org.springframework.core.Ordered Ordered}
		 * 或使用{@link org.springframework.core.annotation.Order @Order}注解,
		 * 它可以影响它与默认值合并的位置;
		 * 否则, 本地声明的监听器将在合并时简单地附加到默认监听器列表中.
		 */
		MERGE_WITH_DEFAULTS
	}

}
