package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用S​​pring的异步方法执行功能, 类似于Spring的{@code <task:*>} XML命名空间中的功能.
 *
 * <p>要与@{@link Configuration Configuration}类一起使用, 如下所示,
 * 为整个Spring应用程序上下文启用注解驱动的异步处理:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig {
 *
 * }</pre>
 *
 * {@code MyAsyncBean}是一个用户定义的类型, 其中一个或多个方法使用Spring的{@code @Async}注解,
 * EJB 3.1 {@code @javax.ejb.Asynchronous}注解, 或通过{@link #annotation}属性指定的自定义注解.
 * 为任何已注册的bean透明地添加切面, 例如通过此配置:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AnotherAppConfig {
 *
 *     &#064;Bean
 *     public MyAsyncBean asyncBean() {
 *         return new MyAsyncBean();
 *     }
 * }</pre>
 *
 * <p>默认情况下, Spring将搜索关联的线程池定义:
 * 上下文中的唯一{@link org.springframework.core.task.TaskExecutor} bean,
 * 或者名为"taskExecutor"的{@link java.util.concurrent.Executor} bean.
 * 如果两者都不可解析, 则将使用{@link org.springframework.core.task.SimpleAsyncTaskExecutor}来处理异步方法调用.
 * 此外, 具有{@code void}返回类型的带注解的方法不能将任何异常传回给调用者.
 * 默认情况下, 仅记录此类未捕获的异常.
 *
 * <p>要自定义所有这些, 请实现{@link AsyncConfigurer}并提供:
 * <ul>
 * <li>{@link java.util.concurrent.Executor Executor}, 通过{@link AsyncConfigurer#getAsyncExecutor getAsyncExecutor()}方法</li>
 * <li>{@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler AsyncUncaughtExceptionHandler},
 * 通过{@link AsyncConfigurer#getAsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler()}方法.</li>
 * </ul>
 *
 * <p><b>NOTE: {@link AsyncConfigurer}配置类在应用程序上下文引导程序的早期初始化.
 * 如果你需要依赖其他bean, 请确保尽可能地声明它们'lazy', 以便让它们通过其他后处理器.</b>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig implements AsyncConfigurer {
 *
 *     &#064;Override
 *     public Executor getAsyncExecutor() {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setCorePoolSize(5);
 *         executor.setMaxPoolSize(10);
 *         executor.setQueueCapacity(25);
 *         executor.setThreadNamePrefix("MyExecutor-");
 *         executor.initialize();
 *         return executor;
 *     }
 *
 *     &#064;Override
 *     public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
 *         return MyAsyncUncaughtExceptionHandler();
 *     }
 * }</pre>
 *
 * <p>如果只需要自定义一个项目, 则可以返回{@code null}以保留默认设置.
 * 考虑尽可能从{@link AsyncConfigurerSupport}扩展.
 *
 * <p>Note: 在上面的例子中, {@code ThreadPoolTask​​Executor}不是一个完全管理的Spring bean.
 * 如果您想要一个完全托管的bean, 请将{@code @Bean}注解添加到{@code getAsyncExecutor()}方法.
 * 在这种情况下, 不再需要手动调用{@code executor.initialize()}方法, 因为这将在bean初始化时自动调用.
 *
 * <p>作为参考, 可以将上面的示例与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * {@code
 * <beans>
 *
 *     <task:annotation-driven executor="myExecutor" exception-handler="exceptionHandler"/>
 *
 *     <task:executor id="myExecutor" pool-size="5-10" queue-capacity="25"/>
 *
 *     <bean id="asyncBean" class="com.foo.MyAsyncBean"/>
 *
 *     <bean id="exceptionHandler" class="com.foo.MyAsyncUncaughtExceptionHandler"/>
 *
 * </beans>
 * }</pre>
 *
 * 除了设置{@code Executor}的<em>线程名称前缀</em>之外, 上述基于XML和基于JavaConfig的示例是等效的;
 * 这是因为{@code <task:executor>}元素不公开这样的属性.
 * 这演示了基于JavaConfig的方法, 如何通过直接访问实际的组件, 来实现最大的可配置性.
 *
 * <p>{@link #mode}属性控制如何应用增强:
 * 如果模式为{@link AdviceMode#PROXY} (默认), 则其他属性控制代理的行为.
 * 请注意, 代理模式仅允许通过代理拦截呼叫; 同一类中的本地调用不能以这种方式截获.
 *
 * <p>请注意, 如果{@linkplain #mode}设置为{@link AdviceMode#ASPECTJ}, 那么{@link #proxyTargetClass}属性的值将被忽略.
 * 还要注意, 在这种情况下, {@code spring-aspects}模块JAR必须存在于类路径中, 编译时织入或加载时织入将切面应用于受影响的类.
 * 在这种情况下没有涉及代理; 本地调用也会被截获.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AsyncConfigurationSelector.class)
public @interface EnableAsync {

	/**
	 * 指示要在类或方法级别检测的'async'注解类型.
	 * <p>默认情况下, 将检测到Spring的 @{@link Async}注解和EJB 3.1 {@code @javax.ejb.Asynchronous}注解.
	 * <p>此属性存在, 以便开发人员可以提供自己的自定义注释类型, 以指示应异步调用方法 (或给定类的所有方法).
	 */
	Class<? extends Annotation> annotation() default Annotation.class;

	/**
	 * 指示是否要创建基于子类的(CGLIB)代理而不是基于标准Java接口的代理.
	 * <p><strong>仅在{@link #mode}设置为{@link AdviceMode#PROXY}时适用</strong>.
	 * <p>默认{@code false}.
	 * <p>请注意, 将此属性设置为{@code true}将影响<em>所有</em> Spring管理的需要代理的bean, 而不仅仅是那些标有{@code @Async}的bean.
	 * 例如, 标有Spring的{@code @Transactional}注解的其他bean将同时升级为子类代理.
	 * 这种方法在实践中没有任何负作用, 除非有人明确期望一种代理与另一种代理对比 &mdash; 例如, 在测试中.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 应如何应用异步增强.
	 * <p><b>默认是{@link AdviceMode#PROXY}.</b>
	 * 请注意, 代理模式仅允许通过代理拦截调用.
	 * 同一类中的本地调用不能以这种方式拦截; 由于Spring的拦截器甚至没有为这样的运行时场景启动, 因此将忽略本地调用中此类方法的{@link Async}注解.
	 * 对于更高级的拦截模式, 请考虑将其切换为 {@link AdviceMode#ASPECTJ}.
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指示应该应用{@link AsyncAnnotationBeanPostProcessor}的顺序.
	 * <p>默认为{@link Ordered#LOWEST_PRECEDENCE}, 以便在所有其他后处理器之后运行, 以便它可以向现有代理添加切面而不是双代理.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
