package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 测试注解, 表明与测试关联的{@link org.springframework.context.ApplicationContext ApplicationContext}是<em>dirty</em>,
 * 因此应该关闭并从上下文缓存中删除.
 *
 * <p>如果测试修改了上下文, 请使用此注解&mdash; 例如, 通过修改单例bean的状态, 修改嵌入式数据库的状态等.
 * 请求相同上下文的后续测试将提供新的上下文.
 *
 * <p>{@code @DirtiesContext}可以用作同一类或类层次结构中的类级别和方法级别注解.
 * 在这种情况下, {@code ApplicationContext}将被标记为<em>dirty</em>, 在带此注解的方法之前或之后,
 * 以及当前测试类之前或之后, 具体取决于配置的{@link #methodMode} 和 {@link #classMode}.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 *
 * <h3>支持的测试阶段</h3>
 * <ul>
 * <li><strong>在当前的测试类之前</strong>: 当在类级别声明, 且类模式设置为{@link ClassMode#BEFORE_CLASS BEFORE_CLASS}</li>
 * <li><strong>在当前测试类中的每个测试方法之前</strong>:
 * 当在类级别声明, 且类模式设置为{@link ClassMode#BEFORE_EACH_TEST_METHOD BEFORE_EACH_TEST_METHOD}</li>
 * <li><strong>在当前测试方法之前</strong>: 在方法级别声明, 且方法模式设置为{@link MethodMode#BEFORE_METHOD BEFORE_METHOD}</li>
 * <li><strong>在当前测试方法之后</strong>: 当在方法级别声明, 且方法模式设置为{@link MethodMode#AFTER_METHOD AFTER_METHOD}</li>
 * <li><strong>在当前测试类中的每个测试方法之后</strong>:
 * 当在类级别声明, 且类模式设置为{@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}</li>
 * <li><strong>在当前的测试类之后</strong>: 当在类级别声明, 且类模式设置为{@link ClassMode#AFTER_CLASS AFTER_CLASS}</li>
 * </ul>
 *
 * <p>
 * {@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener DirtiesContextBeforeModesTestExecutionListener}
 * 支持{@code BEFORE_*}模式;
 * {@link org.springframework.test.context.support.DirtiesContextTestExecutionListener DirtiesContextTestExecutionListener}
 * 支持{@code AFTER_*}模式.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DirtiesContext {

	/**
	 * 测试方法使用{@code @DirtiesContext}注解时, 使用的<i>模式</i>.
	 * <p>默认{@link MethodMode#AFTER_METHOD AFTER_METHOD}.
	 * <p>在带注解的测试类上设置方法模式没有意义.
	 * 对于类级别控制, 请改用{@link #classMode}.
	 */
	MethodMode methodMode() default MethodMode.AFTER_METHOD;

	/**
	 * 测试类使用{@code @DirtiesContext}注解时, 使用的<i>模式</i>.
	 * <p>默认{@link ClassMode#AFTER_CLASS AFTER_CLASS}.
	 * <p>在带注解的测试方法上设置类模式没有意义.
	 * 对于方法级控制, 请改用{@link #methodMode}.
	 */
	ClassMode classMode() default ClassMode.AFTER_CLASS;

	/**
	 * 通过{@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}
	 * 将上下文配置为层次结构的一部分时, 使用的上下文缓存清除<em>模式</em>.
	 * <p>默认{@link HierarchyMode#EXHAUSTIVE EXHAUSTIVE}.
	 */
	HierarchyMode hierarchyMode() default HierarchyMode.EXHAUSTIVE;


	/**
	 * 定义<i>模式</i>, 用于确定在注解测试方法时如何解释{@code @DirtiesContext}.
	 */
	enum MethodMode {

		/**
		 * 在相应的测试方法之前, 相关的{@code ApplicationContext}将被标记为<em>dirty</em>.
		 */
		BEFORE_METHOD,

		/**
		 * 在相应的测试方法之后, 关联的{@code ApplicationContext}将被标记为<em>dirty</em>.
		 */
		AFTER_METHOD;
	}


	/**
	 * 定义<i>模式</i>, 用于确定在注解测试类时如何解释{@code @DirtiesContext}.
	 */
	enum ClassMode {

		/**
		 * 在测试类之前, 关联的{@code ApplicationContext}将被标记为<em>dirty</em>.
		 */
		BEFORE_CLASS,

		/**
		 * 在类中的每个测试方法之前, 关联的{@code ApplicationContext}将被标记为<em>dirty</em>.
		 */
		BEFORE_EACH_TEST_METHOD,

		/**
		 * 在类中的每个测试方法之后, 关联的{@code ApplicationContext}将被标记为<em>dirty</em>.
		 */
		AFTER_EACH_TEST_METHOD,

		/**
		 * 在测试类之后, 关联的{@code ApplicationContext}将标记为<em>dirty</em>.
		 */
		AFTER_CLASS;
	}


	/**
	 * 定义<i>模式</i>, 用于确定在通过{@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}
	 * 将上下文配置为层次结构的一部分的测试中使用{@code @DirtiesContext}时, 如何清除上下文缓存.
	 */
	enum HierarchyMode {

		/**
		 * 将使用<em>穷举</em>算法清除上下文缓存, 该算法不仅包括{@linkplain HierarchyMode#CURRENT_LEVEL 当前级别},
		 * 还包括共享当前测试共有的祖先上下文的所有其他上下文层次结构.
		 *
		 * <p>驻留在公共祖先上下文的子层次结构中的所有{@code ApplicationContexts}将从上下文缓存中删除并关闭.
		 */
		EXHAUSTIVE,

		/**
		 * 上下文层次结构中<em>当前级别</em>的{@code ApplicationContext}和当前级别子层次结构中的所有上下文将从上下文缓存中删除并关闭.
		 *
		 * <p><em>当前级别</em>指的是从当前测试中可见的上下文层次结构中最低级别的{@code ApplicationContext}.
		 */
		CURRENT_LEVEL;
	}

}
