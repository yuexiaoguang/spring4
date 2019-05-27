package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用Spring的注解驱动的缓存管理功能, 类似于Spring的 {@code <cache:*>} XML命名空间中的支持.
 * 与 @{@link org.springframework.context.annotation.Configuration Configuration}类一起使用如下:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 * }</pre>
 *
 * <p>作为参考, 可以将上面的示例与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * {@code
 * <beans>
 *
 *     <cache:annotation-driven/>
 *
 *     <bean id="myService" class="com.foo.MyService"/>
 *
 *     <bean id="cacheManager" class="org.springframework.cache.support.SimpleCacheManager">
 *         <property name="caches">
 *             <set>
 *                 <bean class="org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean">
 *                     <property name="name" value="default"/>
 *                 </bean>
 *             </set>
 *         </property>
 *     </bean>
 *
 * </beans>
 * }</pre>
 *
 * 在上面的两个场景中, {@code @EnableCaching}和{@code <cache:annotation-driven/>}负责注册为注解驱动的缓存管理提供支持的必要Spring组件,
 * 例如{@link org.springframework.cache.interceptor.CacheInterceptor CacheInterceptor} 以及基于代理或基于AspectJ的切面,
 * 在调用 {@link org.springframework.cache.annotation.Cacheable @Cacheable}方法时将拦截器编织到调用堆栈中.
 *
 * <p>如果存在JSR-107 API和Spring的JCache实现, 则还会注册管理标准缓存注解的必要组件.
 * 这将创建基于代理或基于AspectJ的切面, 当调用使用 {@code CacheResult}, {@code CachePut}, 
 * {@code CacheRemove}, {@code CacheRemoveAll}注解的方法时, 它会将拦截器编织到调用堆栈中.
 *
 * <p><strong>必须注册 {@link org.springframework.cache.CacheManager CacheManager}类型的bean</strong>,
 * 因为框架没有合理的默认值可以作为约定使用.
 * 而 {@code <cache:annotation-driven>} 元素假设bean<em>名为</em> "cacheManager",
 * {@code @EnableCaching}<em>按类型</em>搜索缓存管理器bean.
 * 因此, 缓存管理器bean方法的命名并不重要.
 *
 * <p>对于那些希望在 {@code @EnableCaching}和要使用的确切缓存管理器bean之间建立更直接关系的人,
 * 可以实现{@link CachingConfigurer}回调接口.
 * 请注意下面{@code @Override}注解的方法:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig extends CachingConfigurerSupport {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public KeyGenerator keyGenerator() {
 *         // configure and return an implementation of Spring's KeyGenerator SPI
 *         return new MyKeyGenerator();
 *     }
 * }</pre>
 *
 * 这种方法可能只是因为它更明确, 或者为了区分同一容器中存在的两个{@code CacheManager} bean, 而可能是必要的.
 *
 * <p>另请注意上面示例中的 {@code keyGenerator}方法.
 * 根据Spring的 {@link org.springframework.cache.interceptor.KeyGenerator KeyGenerator} SPI, 允许自定义缓存键生成策略.
 * 通常, {@code @EnableCaching} 将为此目的配置Spring的 {@link org.springframework.cache.interceptor.SimpleKeyGenerator SimpleKeyGenerator},
 * 但是在实现 {@code CachingConfigurer}时, 必须明确提供键生成器.
 * 如果不需要自定义, 请从此方法返回{@code null}或{@code new SimpleKeyGenerator()}.
 *
 * <p>{@link CachingConfigurer}提供了额外的自定义选项:
 * 建议从{@link org.springframework.cache.annotation.CachingConfigurerSupport CachingConfigurerSupport}扩展,
 * 为所有方法提供默认实现, 如果您不需要自定义所有方法.
 * 有关更多详细信息, 请参阅{@link CachingConfigurer} Javadoc.
 *
 * <p>{@link #mode}属性控制如何应用增强:
 * 如果模式为 {@link AdviceMode#PROXY} (默认值), 则其他属性控制代理的行为.
 * 请注意, 代理模式仅允许通过代理拦截调用; 同一类中的本地调用不能以这种方式拦截.
 *
 * <p>请注意, 如果 {@linkplain #mode}设置为 {@link AdviceMode#ASPECTJ}, 那么{@link #proxyTargetClass}属性的值将被忽略.
 * 还要注意, 在这种情况下, {@code spring-aspects}模块JAR必须存在于类路径中, 编译时编织或加载时编织将切面应用于受影响的类.
 * 在这种情况下没有涉及代理; 本地调用也会被拦截.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {

	/**
	 * 指示是否要创建基于子类的(CGLIB)代理, 而不是基于标准Java接口的代理.
	 * 默认是 {@code false}.
	 * <strong>仅在{@link #mode()}设置为 {@link AdviceMode#PROXY}时适用</strong>.
	 * <p>请注意, 将此属性设置为{@code true}将影响 <em>所有</em> Spring管理的需要代理的bean, 而不仅仅是那些标有 {@code @Cacheable}的bean.
	 * 例如, 标有Spring的 {@code @Transactional}注解的其他bean将同时升级为子类代理.
	 * 这种方法在实践中没有负面影响, 除非有人明确期望一种代理 vs 另一种代理, e.g. 在测试中.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指出应如何应用缓存增强.
	 * <p><b>默认是 {@link AdviceMode#PROXY}.</b>
	 * 请注意, 代理模式仅允许通过代理拦截调用.
	 * 同一类中的本地调用不能以这种方式拦截; 由于Spring的拦截器甚至没有为这样的运行时场景启动, 因此将忽略本地调用中此类方法的缓存注解.
	 * 对于更高级的拦截模式, 请考虑将其切换为 {@link AdviceMode#ASPECTJ}.
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指示在特定连接点应用多个增强时, 执行缓存切面的顺序.
	 * <p>默认是 {@link Ordered#LOWEST_PRECEDENCE}.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
