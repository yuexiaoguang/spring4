package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @Sql}用于注解测试类或测试方法,
 * 以配置在集成测试期间针对给定数据库执行的SQL {@link #scripts}和{@link #statements}.
 *
 * <p>方法级声明覆盖类级声明.
 *
 * <p>脚本执行由{@link SqlScriptsTestExecutionListener}执行, 默认情况下已启用.
 *
 * <p>此注解提供的配置选项和{@link SqlConfig @SqlConfig}等同于
 * {@link org.springframework.jdbc.datasource.init.ScriptUtils ScriptUtils}和
 * {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator ResourceDatabasePopulator}
 * 支持的配置选项, 但它们是{@code <jdbc:initialize-database/>} XML 命名空间元素提供的超集.
 * 有关详细信息, 请参阅此注解和{@link SqlConfig @SqlConfig}中的各个属性的javadoc.
 *
 * <p>从Java 8开始, {@code @Sql}可以用作<em>{@linkplain Repeatable 可重复}</em>注解.
 * 否则, {@link SqlGroup @SqlGroup}可以用作声明{@code @Sql}的多个实例的显式容器.
 *
 * <p>此注释可用作<em>元注解</em>以使用属性覆盖创建自定义<em>组合注解</em>.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(SqlGroup.class)
public @interface Sql {

	/**
	 * {@link #scripts}的别名.
	 * <p>此属性<strong>不</strong>可以与{@link #scripts}一起使用, 但可以使用它来代替{@link #scripts}.
	 */
	@AliasFor("scripts")
	String[] value() default {};

	/**
	 * 要执行的SQL脚本的路径.
	 * <p>此属性可以<strong>不</strong>与{@link #value}一起使用, 但可以使用它来代替{@link #value}.
	 * 同样, 此属性可与{@link #statements}结合使用或代替它.
	 * <h3>路径资源语义</h3>
	 * <p>每个路径都将被解释为Spring {@link org.springframework.core.io.Resource Resource}.
	 * 一个普通路径 &mdash; 例如, {@code "schema.sql"} &mdash; 将被视为<em>相对于</em>定义测试类的包的的类路径资源.
	 * 以斜杠开头的路径将被视为<em>绝对</em>类路径资源, 例如:
	 * {@code "/org/example/schema.sql"}.
	 * 将使用指定的资源协议加载引用URL的路径(e.g., 以
	 * {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:},
	 * {@code http:}等为前缀的路径).
	 * 
	 * <h3>默认脚本检测</h3>
	 * <p>如果未指定SQL脚本或{@link #statements}, 则将尝试检测<em>默认</em>脚本, 具体取决于此注解的声明位置.
	 * 如果无法检测到默认值, 将抛出{@link IllegalStateException}.
	 * <ul>
	 * <li><strong>类级声明</strong>: 如果带注解的测试类是{@code com.example.MyTest},
	 * 则相应的默认脚本是{@code "classpath:com/example/MyTest.sql"}.</li>
	 * <li><strong>方法级声明</strong>: 如果带注解的测试方法名为{@code testMethod()}并且在类{@code com.example.MyTest}中定义,
	 * 则相应的默认脚本为{@code "classpath:com/example/MyTest.testMethod.sql"}.</li>
	 * </ul>
	 */
	@AliasFor("value")
	String[] scripts() default {};

	/**
	 * 要执行的<em>内联SQL语句</em>.
	 * <p>此属性可与{@link #scripts}结合使用或代替它.
	 * <h3>顺序</h3>
	 * <p>通过此属性声明的语句将在从资源{@link #scripts}加载的语句之后执行.
	 * 如果希望在脚本之前执行内联语句, 只需在同一个类或方法上声明{@code @Sql}的多个实例.
	 */
	String[] statements() default {};

	/**
	 * 何时应执行SQL脚本和语句.
	 * <p>默认{@link ExecutionPhase#BEFORE_TEST_METHOD BEFORE_TEST_METHOD}.
	 */
	ExecutionPhase executionPhase() default ExecutionPhase.BEFORE_TEST_METHOD;

	/**
	 * 在此{@code @Sql}注解中声明的SQL脚本和语句的本地配置.
	 * <p>有关本地与全局配置, 继承, 覆盖等的说明, 请参阅{@link SqlConfig}的类级别javadoc.
	 * <p>默认为空{@link SqlConfig @SqlConfig}实例.
	 */
	SqlConfig config() default @SqlConfig();


	/**
	 * <em>阶段</em>的枚举, 指示何时执行SQL脚本.
	 */
	enum ExecutionPhase {

		/**
		 * 配置的SQL脚本和语句将在相应的测试方法<em>之前</em>执行.
		 */
		BEFORE_TEST_METHOD,

		/**
		 * 配置的SQL脚本和语句将在相应的测试方法<em>之后</em>执行.
		 */
		AFTER_TEST_METHOD
	}

}
