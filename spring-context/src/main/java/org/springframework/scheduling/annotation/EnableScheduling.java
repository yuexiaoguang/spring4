package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 启用S​​pring的计划任务执行功能, 类似于Spring的{@code <task:*>} XML名称空间中的功能.
 * 要在@{@link Configuration}类上使用, 如下所示:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     // various &#064;Bean definitions
 * }</pre>
 *
 * 这样可以检测容器中任何Spring管理的bean上的 @{@link Scheduled}注解.
 * 例如, 给定一个类{@code MyTask}
 *
 * <pre class="code">
 * package com.myco.tasks;
 *
 * public class MyTask {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }</pre>
 *
 * 以下配置将确保每1000毫秒调用一次{@code MyTask.work()}:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyTask task() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * 或者, 如果{@code MyTask}使用{@code @Component}注解, 则以下配置将确保以所需的时间间隔调用其{@code @Scheduled}方法:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * &#064;ComponentScan(basePackages="com.myco.tasks")
 * public class AppConfig {
 * }</pre>
 *
 * 使用{@code @Scheduled}注解的方法甚至可以直接在{@code @Configuration}类中声明:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }</pre>
 *
 * <p>默认情况下, 将搜索关联的调度器定义:
 * 上下文中的唯一{@link org.springframework.scheduling.TaskScheduler} bean, 或者名为"taskScheduler"的{@code TaskScheduler} bean;
 * 同样的查找也将针对{@link java.util.concurrent.ScheduledExecutorService} bean执行.
 * 如果两者都不可解析, 则将在注册器中创建并使用本地单线程默认调度器.
 *
 * <p>当需要更多控制时, {@code @Configuration}类可以实现{@link SchedulingConfigurer}.
 * 这允许访问底层的{@link ScheduledTaskRegistrar}实例.
 * 例如, 以下示例演示如何自定义用于执行计划任务的{@link Executor}:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskExecutor());
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskExecutor() {
 *         return Executors.newScheduledThreadPool(100);
 *     }
 * }</pre>
 *
 * <p>请注意上面的示例中使用的{@code @Bean(destroyMethod="shutdown")}.
 * 这可确保在Spring应用程序上下文本身关闭时正确关闭任务执行器.
 *
 * <p>实现{@code SchedulingConfigurer}还允许通过 {@code ScheduledTaskRegistrar}对任务注册进行细粒度控制.
 * 例如, 以下配置根据自定义{@code Trigger}实现执行特定bean方法:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskScheduler());
 *         taskRegistrar.addTriggerTask(
 *             new Runnable() {
 *                 public void run() {
 *                     myTask().work();
 *                 }
 *             },
 *             new CustomTrigger()
 *         );
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskScheduler() {
 *         return Executors.newScheduledThreadPool(42);
 *     }
 *
 *     &#064;Bean
 *     public MyTask myTask() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * <p>作为参考, 可以将上面的示例与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * {@code
 * <beans>
 *
 *     <task:annotation-driven scheduler="taskScheduler"/>
 *
 *     <task:scheduler id="taskScheduler" pool-size="42"/>
 *
 *     <task:scheduled-tasks scheduler="taskScheduler">
 *         <task:scheduled ref="myTask" method="work" fixed-rate="1000"/>
 *     </task:scheduled-tasks>
 *
 *     <bean id="myTask" class="com.foo.MyTask"/>
 *
 * </beans>
 * }</pre>
 *
 * 这些示例是等效的, 除了在XML中使用<em>fixed-rate</em>周期, 而不是自定义<em>{@code Trigger}</em>实现;
 * 这是因为{@code task:}命名空间 {@code scheduled}不能轻易暴露这样的支持.
 * 这只是一个演示, 即基于代码的方法如何通过直接访问实际的组件来实现最大的可配置性.<p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingConfiguration.class)
@Documented
public @interface EnableScheduling {

}
