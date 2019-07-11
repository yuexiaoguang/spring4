package org.springframework.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.TransactionDefinition;

/**
 * 描述方法或类的事务属性.
 *
 * <p>这个注解类型通常可以直接与Spring的
 * {@link org.springframework.transaction.interceptor.RuleBasedTransactionAttribute}类进行比较,
 * 实际上{@link AnnotationTransactionAttributeSource}会直接将数据转换为后一个类,
 * 因此Spring的事务支持代码不必知道注解.
 * 如果没有规则与异常相关, 则将其视为
 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
 * (在{@link RuntimeException}和{@link Error}上回滚, 但不在受检异常上回滚).
 *
 * <p>有关此注解的属性的语义的特定信息,
 * 请参阅{@link org.springframework.transaction.TransactionDefinition}
 * 和{@link org.springframework.transaction.interceptor.TransactionAttribute}的 javadoc.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {

	/**
	 * {@link #transactionManager}的别名.
	 */
	@AliasFor("transactionManager")
	String value() default "";

	/**
	 * 指定事务的<em>限定符</em>值.
	 * <p>可用于确定目标事务管理器,
	 * 匹配特定{@link org.springframework.transaction.PlatformTransactionManager} bean定义的限定符值(或bean名称).
	 */
	@AliasFor("value")
	String transactionManager() default "";

	/**
	 * 事务传播类型.
	 * <p>默认{@link Propagation#REQUIRED}.
	 */
	Propagation propagation() default Propagation.REQUIRED;

	/**
	 * 事务隔离级别.
	 * <p>默认{@link Isolation#DEFAULT}.
	 * <p>专门设计用于{@link Propagation#REQUIRED}或{@link Propagation#REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务.
	 * 如果希望隔离级别声明在参与具有不同隔离级别的现有事务时被拒绝,
	 * 在事务管理器上将"validateExistingTransactions"标志切换为"true".
	 */
	Isolation isolation() default Isolation.DEFAULT;

	/**
	 * 此事务的超时.
	 * <p>默认为底层事务系统的默认超时.
	 * <p>专门设计用于{@link Propagation#REQUIRED}或{@link Propagation#REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务.
	 */
	int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

	/**
	 * 如果事务是有效只读的, 则可以设置为{@code true}, 允许在运行时进行相应的优化.
	 * <p>默认{@code false}.
	 * <p>这仅仅是实际事务子系统的提示; 它<i>不一定</i>导致写访问尝试失败.
	 * 一个事务管理器无法解释只读提示, 当被要求进行只读事务时, <i>不会</i>抛出异常, 而是默默地忽略提示.
	 */
	boolean readOnly() default false;

	/**
	 * 定义零(0) 或更多异常{@link Class classes}, 它必须是{@link Throwable}的子类, 指示哪些异常类型必须导致事务回滚.
	 * <p>默认情况下, 事务将在{@link RuntimeException}和{@link Error}上回滚, 但不会在受检异常(业务异常)上回滚.
	 * 有关详细说明, 参见
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)}.
	 * <p>这是构造回滚规则的首选方法 (与{@link #rollbackForClassName}对比), 匹配异常类及其子类.
	 * <p>类似于{@link org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(Class clazz)}.
	 */
	Class<? extends Throwable>[] rollbackFor() default {};

	/**
	 * 定义零 (0) 或更多异常名称 (必须是{@link Throwable}的子类的异常), 指示哪些异常类型必须导致事务回滚.
	 * <p>这可以是完全限定类名的子字符串, 目前没有通配符支持.
	 * 例如, {@code "ServletException"}的值将匹配{@code javax.servlet.ServletException}及其子类.
	 * <p><b>NB:</b> 仔细考虑模式的具体程度以及是否包含包信息 (这不是强制性的).
	 * 例如, {@code "Exception"}几乎可以匹配任何内容, 并且可能会隐藏其他规则.
	 * 如果{@code "Exception"}用于为所有受检异常定义规则, 则{@code "java.lang.Exception"}将是正确的.
	 * 使用更多不寻常的{@link Exception}名称, 例如{@code "BaseBusinessException"}, 不需要使用FQN.
	 * <p>类似于{@link org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(String exceptionName)}.
	 */
	String[] rollbackForClassName() default {};

	/**
	 * 定义零(0) 或更多异常{@link Class Classes}, 必须是{@link Throwable}的子类,
	 * 指示哪些异常类型 <b>不能</b>导致事务回滚.
	 * <p>这是构造回滚规则的首选方法 (与{@link #noRollbackForClassName}相比), 匹配异常类及其子类.
	 * <p>类似于{@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(Class clazz)}.
	 */
	Class<? extends Throwable>[] noRollbackFor() default {};

	/**
	 * 定义零(0) 或更多异常名称(必须是{@link Throwable}的子类的异常),
	 * 指示哪些异常类型<b>不能</b>导致事务回滚.
	 * <p>有关如何处理指定名称的详细信息, 请参阅{@link #rollbackForClassName}的说明.
	 * <p>类似于{@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(String exceptionName)}.
	 */
	String[] noRollbackForClassName() default {};

}
