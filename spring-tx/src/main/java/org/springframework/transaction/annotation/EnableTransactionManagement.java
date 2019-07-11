package org.springframework.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用S​​pring的注解驱动的事务管理功能, 类似于Spring的{@code <tx:*>} XML命名空间中的支持.
 * 要在{@link org.springframework.context.annotation.Configuration @Configuration}类上使用, 如下所示:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 * }</pre>
 *
 * <p>作为参考, 可以将上面的示例与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * {@code
 * <beans>
 *
 *     <tx:annotation-driven/>
 *
 *     <bean id="fooRepository" class="com.foo.JdbcFooRepository">
 *         <constructor-arg ref="dataSource"/>
 *     </bean>
 *
 *     <bean id="dataSource" class="com.vendor.VendorDataSource"/>
 *
 *     <bean id="transactionManager" class="org.sfwk...DataSourceTransactionManager">
 *         <constructor-arg ref="dataSource"/>
 *     </bean>
 *
 * </beans>
 * }</pre>
 *
 * 在上述两种情况中, {@code @EnableTransactionManagement}和 {@code <tx:annotation-driven/>}
 * 负责注册为注解驱动的事务管理提供支持的必要的Spring组件, 例如TransactionInterceptor及其代理,
 * 或基于AspectJ的增强, 在调用{@code JdbcFooRepository}的{@code @Transactional}方法时将拦截器织入到调用堆栈中.
 *
 * <p>两个示例之间的细微差别在于{@code PlatformTransactionManager} bean的命名:
 * 在{@code @Bean}的情况下, 名称是<em>"txManager"</em> (根据方法的名称); 在XML情况下, 名称是<em>"transactionManager"</em>.
 * {@code <tx:annotation-driven/>}默认查找名为"transactionManager"的bean, 但{@code @EnableTransactionManagement}更灵活;
 * 它将回退到容器中任何{@code PlatformTransactionManager} bean的按类型查找.
 * 因此, 名称可以是"txManager", "transactionManager", 或"tm": 它无关紧要.
 *
 * <p>对于那些希望在{@code @EnableTransactionManagement}和要使用的确切事务管理器bean之间建立更直接关系的人,
 * 可以实现{@link TransactionManagementConfigurer}回调接口 - 注意{{@code implements}子句和带{@code @Override}注解的方法:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig implements TransactionManagementConfigurer {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 *
 *     &#064;Override
 *     public PlatformTransactionManager annotationDrivenTransactionManager() {
 *         return txManager();
 *     }
 * }</pre>
 *
 * 这种方法可能只是因为它更明确, 或者为了区分同一容器中存在的两个{@code PlatformTransactionManager} bean而可能是必要的.
 * 顾名思义, {@code annotationDrivenTransactionManager()}将用于处理{@code @Transactional}方法.
 * 有关更多详细信息, 请参阅{@link TransactionManagementConfigurer} Javadoc.
 *
 * <p>{@link #mode}属性控制如何应用增强:
 * 如果模式为{@link AdviceMode#PROXY} (默认), 则其他属性控制代理的行为.
 * 请注意, 代理模式仅允许通过代理拦截调用; 同一类中的本地调用不能以这种方式拦截.
 *
 * <p>请注意, 如果{@linkplain #mode}设置为{@link AdviceMode#ASPECTJ}, 那么{@link #proxyTargetClass}属性的值将被忽略.
 * 还要注意, 在这种情况下, {@code spring-aspects}模块JAR必须存在于类路径中, 编译时织入或加载时织入将切面应用于受影响的类.
 * 在这种情况下没有涉及代理; 本地调用也会被拦截.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {

	/**
	 * 指示是否要创建基于子类的 (CGLIB)代理 ({@code true}), 而不是基于标准Java接口的代理 ({@code false}).
	 * 默认{@code false}. <strong>仅在{@link #mode()}设置为{{@link AdviceMode#PROXY}时适用</strong>.
	 * <p>请注意, 将此属性设置为{@code true}将影响<em>所有</em> Spring管理的需要代理的bean,
	 * 而不仅仅是那些标有{@code @Transactional}的bean.
	 * 例如, 标有Spring的 {@code @Async}注解的其他bean将同时升级为子类代理.
	 * 这种方法在实践中没有负面影响, 除非有人明确期望一种类型的代理与另一种代理相比, e.g. 在测试中.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指示应如何应用事务增强.
	 * <p><b>默认{@link AdviceMode#PROXY}.</b>
	 * 请注意, 代理模式仅允许通过代理拦截调用.
	 * 同一类中的本地调用不能以这种方式拦截; 由于Spring的拦截器甚至没有为这样的运行时场景启动,
	 * 因此将忽略本地调用中此类方法的{@link Transactional}注解.
	 * 对于更高级的拦截模式, 考虑将其切换为{@link AdviceMode#ASPECTJ}.
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指示在特定连接点应用多个增强时执行事务切面的顺序.
	 * <p>默认{@link Ordered#LOWEST_PRECEDENCE}.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
