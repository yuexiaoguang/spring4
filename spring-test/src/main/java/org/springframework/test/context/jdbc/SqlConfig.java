package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SqlConfig}定义用于确定如何解析和执行通过{@link Sql @Sql}注解配置的SQL脚本的元数据.
 *
 * <h3>配置范围</h3>
 * <p>在集成测试类中声明为类级别注解时, {@code @SqlConfig}将作为测试类层次结构中所有SQL脚本的<strong><em>全局</em></strong>配置.
 * 当通过{@code @Sql}注解的{@link Sql#config config}属性直接声明时, {@code @SqlConfig}用作<strong><em>本地</em></strong>配置,
 * 用于在封闭的{@code @Sql}注解中声明的SQL脚本.
 *
 * <h3>默认值</h3>
 * <p>{@code @SqlConfig}中的每个属性都有一个<em>隐式</em>默认值, 该值记录在相应属性的javadoc中.
 * 由于在Java语言规范中为注解属性定义了规则, 遗憾的是无法将{@code null}的值分配给注解属性.
 * 因此, 为了支持<em>继承的</em>全局配置的覆盖, {@code @SqlConfig}属性具有<em>显式</em> {@code default}值,
 * String的{@code ""}或枚举的{@code DEFAULT}.
 * 这种方法允许{@code @SqlConfig}的本地声明通过提供除{@code ""} 或 {@code DEFAULT}以外的值,
 * 来有选择地覆盖{@code @SqlConfig}的全局声明中的各个属性.
 *
 * <h3>继承和覆盖</h3>
 * <p>只要本地{@code @SqlConfig}属性不提供{@code ""} 或 {@code DEFAULT}以外的显式值,
 * 就会<em>继承</em>全局{@code @SqlConfig}属性.
 * 因此显式本地配置<em>覆盖</em>全局配置.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlConfig {

	/**
	 * 应该对其执行脚本的{@link javax.sql.DataSource}的bean名称.
	 * <p>只有在测试的{@code ApplicationContext}中有多个{@code DataSource}类型的bean时才需要该名称.
	 * 如果只有一个这样的bean, 则不必指定bean名称.
	 * <p>默认为空字符串, 要求满足以下条件之一:
	 * <ol>
	 * <li>在{@code @SqlConfig}的全局声明中定义了显式bean名称.
	 * <li>可以使用反射从事务管理器中检索数据源, 以在事务管理器上调用名为{@code getDataSource()}的公共方法.
	 * <li>测试{@code ApplicationContext}中只有一个{@code DataSource}类型的bean.</li>
	 * <li>要使用的{@code DataSource}名为{@code "dataSource"}.</li>
	 * </ol>
	 */
	String dataSource() default "";

	/**
	 * 应该用于驱动事务的
	 * {@link org.springframework.transaction.PlatformTransactionManager PlatformTransactionManager}的bean名称.
	 * <p>只有在测试的{@code ApplicationContext}中有多个{@code PlatformTransactionManager}类型的bean时才使用该名称.
	 * 如果只有一个这样的bean, 则不必指定bean名称.
	 * <p>默认为空字符串, 要求满足以下条件之一:
	 * <ol>
	 * <li>在{@code @SqlConfig}的全局声明中定义了显式bean名称.
	 * <li>测试的{@code ApplicationContext}中只有一个{@code PlatformTransactionManager}类型的bean.</li>
	 * <li>已经实现了
	 * {@link org.springframework.transaction.annotation.TransactionManagementConfigurer TransactionManagementConfigurer}
	 * 来指定哪个{@code PlatformTransactionManager} bean 应该用于注解驱动的事务管理.</li>
	 * <li>要使用的{@code PlatformTransactionManager}名为{@code "transactionManager"}.</li>
	 * </ol>
	 */
	String transactionManager() default "";

	/**
	 * 确定是否应在事务中执行SQL脚本时使用的<em>模式</em>.
	 * <p>默认{@link TransactionMode#DEFAULT DEFAULT}.
	 * <p>可以设置为{@link TransactionMode#ISOLATED}以确保SQL脚本将在立即提交的新的隔离事务中执行.
	 */
	TransactionMode transactionMode() default TransactionMode.DEFAULT;

	/**
	 * 提供的SQL脚本的编码, 如果与平台编码不同.
	 * <p>空字符串表示应使用平台编码.
	 */
	String encoding() default "";

	/**
	 * 用于分隔SQL脚本中的各个语句的字符串.
	 * <p>隐式地默认为{@code ";"}, 如果未指定, 则作为最后的手段回退到{@code "\n"}.
	 * <p>可以设置为
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#EOF_STATEMENT_SEPARATOR}
	 * 以表示每个脚本包含没有分隔符的单个语句.
	 */
	String separator() default "";

	/**
	 * 标识SQL脚本中单行注释的前缀.
	 * <p>隐式地默认为{@code "--"}.
	 */
	String commentPrefix() default "";

	/**
	 * 开始分隔符, 用于标识SQL脚本中的块注释.
	 * <p>隐式地默认为{@code "/*"}.
	 */
	String blockCommentStartDelimiter() default "";

	/**
	 * 结束分隔符, 用于标识SQL脚本中的块注释.
	 * <p>隐式地默认为<code>"*&#47;"</code>.
	 */
	String blockCommentEndDelimiter() default "";

	/**
	 * 执行SQL语句时遇到错误时使用的<em>模式</em>.
	 * <p>默认{@link ErrorMode#DEFAULT DEFAULT}.
	 */
	ErrorMode errorMode() default ErrorMode.DEFAULT;


	/**
	 * <em>模式</em>的枚举, 指示是否应在事务中执行SQL脚本以及事务传播行为应该是什么.
	 */
	enum TransactionMode {

		/**
		 * 表示应使用的<em>默认</em>事务模式.
		 * <p><em>default</em>的含义取决于声明{@code @SqlConfig}的上下文:
		 * <ul>
		 * <li>如果{@code @SqlConfig}在本地声明, 则默认事务模式为{@link #INFERRED}.</li>
		 * <li>如果全局声明{@code @SqlConfig}, 则默认事务模式为 {@link #INFERRED}.</li>
		 * <li>如果{@code @SqlConfig}在本地<strong>和</strong>全局声明, 则本地声明的默认事务模式将从全局声明继承.</li>
		 * </ul>
		 */
		DEFAULT,

		/**
		 * 指示执行SQL脚本时要使用的事务模式, 应使用下面列出的规则<em>推断</em>.
		 * 在这些规则的上下文中, 术语"<em>available</em>"表示数据源或事务管理器的bean
		 * 可以通过{@code @SqlConfig}中的相应注解属性显式指定, 也可以通过约定发现.
		 * 有关在{@code ApplicationContext}中发现此类bean的约定的详细信息, 请参阅
		 * {@link org.springframework.test.context.transaction.TestContextTransactionUtils TestContextTransactionUtils}.
		 *
		 * <h4>推理规则</h4>
		 * <ol>
		 * <li>如果事务管理器和数据源都不可用, 则会引发异常.
		 * <li>如果事务管理器不可用, 但数据源可用, 则SQL脚本将直接针对数据源执行而无需事务.
		 * <li>如果事务管理器可用:
		 * <ul>
		 * <li>如果数据源不可用, 将尝试使用反射在事务管理器上调用名为 {@code getDataSource()}的公共方法, 从事务管理器中检索它.
		 * 如果尝试失败, 将抛出异常.
		 * <li>使用已解析的事务管理器和数据源, SQL脚本将在现有事务中执行;
		 * 否则, 脚本将在立即提交的新事务中执行.
		 * <em>现有</em>事务通常由
		 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener TransactionalTestExecutionListener}管理.
		 * </ul>
		 * </ol>
		 */
		INFERRED,

		/**
		 * 表示应始终在将立即提交的新的<em>隔离</em>事务中执行SQL脚本.
		 * <p>与{@link #INFERRED}相比, 此模式需要存在事务管理器<strong>和</strong>数据源.
		 */
		ISOLATED
	}


	/**
	 * <em>模式</em>的枚举, 指示在执行SQL语句时如何处理错误.
	 */
	enum ErrorMode {

		/**
		 * 表示应使用<em>默认</em>错误模式.
		 * <p><em>default</em>的含义取决于声明{@code @SqlConfig}的上下文:
		 * <ul>
		 * <li>如果{@code @SqlConfig}在本地声明, 则默认错误模式为{@link #FAIL_ON_ERROR}.</li>
		 * <li>如果{@code @SqlConfig}在全局声明, 则默认错误模式为{@link #FAIL_ON_ERROR}.</li>
		 * <li>如果{@code @SqlConfig}在本地<strong>和</strong>全局声明, 则本地声明的默认错误模式将从全局声明继承.</li>
		 * </ul>
		 */
		DEFAULT,

		/**
		 * 表示如果遇到错误, 脚本执行将失败.
		 * 换句话说, 不应忽略任何错误.
		 * <p>这实际上是默认错误模式, 因此如果脚本被意外执行, 如果脚本中的任何SQL语句导致错误, 它将快速失败.
		 */
		FAIL_ON_ERROR,

		/**
		 * 表示应记录SQL脚本中的所有错误, 但不会将其作为异常传播.
		 * <p>{@code CONTINUE_ON_ERROR}}是{@code FAIL_ON_ERROR}的逻辑<em>相反</em>,
		 * 和{@code IGNORE_FAILED_DROPS}的<em>超集</em>.
		 */
		CONTINUE_ON_ERROR,

		/**
		 * 表示可以忽略失败的SQL {@code DROP}语句.
		 * <p>这对于非嵌入式数据库非常有用, 该数据库的SQL方言不支持{@code DROP}语句中的{@code IF EXISTS}子句.
		 */
		IGNORE_FAILED_DROPS
	}

}
