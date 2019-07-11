package org.springframework.transaction.annotation;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * 使用@{@link EnableTransactionManagement}注解的
 * @{@link org.springframework.context.annotation.Configuration Configuration}类实现的接口,
 * 希望或需要显式指定用于注解驱动的事务管理的默认{@link PlatformTransactionManager} bean,
 * 而不是通过按类型查找的默认方法.
 * 这可能是必要的一个原因是容器中是否存在两个{@code PlatformTransactionManager} bean.
 *
 * <p>有关一般示例和上下文, 请参阅@{@link EnableTransactionManagement};
 * 有关详细说明, 请参阅{@link #annotationDrivenTransactionManager()}.
 *
 * <p>请注意, 在按类型查找消除歧义的情况下, 实现此接口的另一种方法是将一个有问题的
 * {@code PlatformTransactionManager} {@code @Bean}方法标记为{@link org.springframework.context.annotation.Primary @Primary}.
 * 这通常是首选, 因为它不会导致{@code PlatformTransactionManager} bean的实时初始化.
 */
public interface TransactionManagementConfigurer {

	/**
	 * 返回默认事务管理器bean, 以用于注解驱动的数据库事务管理, i.e. 处理{@code @Transactional}方法时.
	 * <p>实现此方法有两种基本方法:
	 * <h3>1. 实现该方法并使用{@code @Bean}注解</h3>
	 * 在这种情况下, 实现{@code @Configuration}类, 实现此方法, 使用{@code @Bean}标记它,
	 * 并直接在方法体内配置并返回事务管理器:
	 * <pre class="code">
	 * &#064;Bean
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }</pre>
	 * <h3>2. 在没有{@code @Bean}的情况下实现该方法, 并委托给另一个现有的{@code @Bean}方法</h3>
	 * <pre class="code">
	 * &#064;Bean
	 * public PlatformTransactionManager txManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }
	 *
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return txManager(); // reference the existing {@code @Bean} method above
	 * }</pre>
	 * 如果采用方法 #2, 确保<em>只有一个</em>方法标有{@code @Bean}!
	 * <p>在方案 #1 或 #2中, 重要的是{@code PlatformTransactionManager}实例作为容器内的Spring bean进行管理,
	 * 因为所有{@code PlatformTransactionManager}实现都利用了Spring生命周期回调,
	 * 例如{@code InitializingBean}和{@code BeanFactoryAware}.
	 */
	PlatformTransactionManager annotationDrivenTransactionManager();

}
