package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 为此应用程序上下文激活Spring {@link LoadTimeWeaver}, 可用作名为"loadTimeWeaver"的bean,
 * 类似于Spring XML中的 {@code <context:load-time-weaver>}元素.
 *
 * <p>在 @{@link org.springframework.context.annotation.Configuration Configuration}类中使用;
 * 最简单的例子如下:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig {
 *
 *     // application-specific &#064;Bean definitions ...
 * }</pre>
 *
 * 上面的示例等效于以下Spring XML配置:
 *
 * <pre class="code">
 * {@code
 * <beans>
 *
 *     <context:load-time-weaver/>
 *
 *     <!-- application-specific <bean> definitions -->
 *
 * </beans>
 * }</pre>
 *
 * <h2>{@code LoadTimeWeaverAware}接口</h2>
 * 实现{@link org.springframework.context.weaving.LoadTimeWeaverAware LoadTimeWeaverAware}接口
 * 的任何bean将自动接收{@code LoadTimeWeaver}引用;
 * 例如, Spring的JPA bootstrap支持.
 *
 * <h2>自定义{@code LoadTimeWeaver}</h2>
 * 默认编织器自动确定: see {@link DefaultContextLoadTimeWeaver}.
 *
 * <p>要自定义使用的weaver, 使用{@code @EnableLoadTimeWeaving}注解的{@code @Configuration}类也可以实现 {@link LoadTimeWeavingConfigurer}接口,
 * 并通过 {@code #getLoadTimeWeaver}返回自定义{@code LoadTimeWeaver}实例:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig implements LoadTimeWeavingConfigurer {
 *
 *     &#064;Override
 *     public LoadTimeWeaver getLoadTimeWeaver() {
 *         MyLoadTimeWeaver ltw = new MyLoadTimeWeaver();
 *         ltw.addClassTransformer(myClassFileTransformer);
 *         // ...
 *         return ltw;
 *     }
 * }</pre>
 *
 * <p>上面的示例可以与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * {@code
 * <beans>
 *
 *     <context:load-time-weaver weaverClass="com.acme.MyLoadTimeWeaver"/>
 *
 * </beans>
 * }</pre>
 *
 * <p>代码示例与XML示例的不同之处在于它实际上实例化了{@code MyLoadTimeWeaver}类型,
 * 意味着它也可以配置实例, e.g. 调用{@code #addClassTransformer}方法.
 * 这演示了基于代码的配置方法如何更加灵活的通过直接编程访问.
 *
 * <h2>启用基于AspectJ的织入</h2>
 * 可以使用{@link #aspectjWeaving()}属性启用AspectJ加载时织入,
 * 将导致 {@linkplain org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter AspectJ类转换器}
 * 通过{@link LoadTimeWeaver#addTransformer}进行注册.
 * 如果类路径中存在"META-INF/aop.xml"资源, 则默认情况下将激活AspectJ织入.
 * Example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving(aspectjWeaving=ENABLED)
 * public class AppConfig {
 * }</pre>
 *
 * <p>上面的示例可以与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * {@code
 * <beans>
 *
 *     <context:load-time-weaver aspectj-weaving="on"/>
 *
 * </beans>
 * }</pre>
 *
 * <p>这两个例子相当于一个重要的异常:
 * 在XML情况下, 当{@code aspectj-weaving}为"on"时, 隐式启用{@code <context:spring-configured>}的功能.
 * 使用{@code @EnableLoadTimeWeaving(aspectjWeaving=ENABLED)}时不会发生这种情况.
 * 相反, 你必须明确添加 {@code @EnableSpringConfigured} (包含在{@code spring-aspects}模块中)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoadTimeWeavingConfiguration.class)
public @interface EnableLoadTimeWeaving {

	/**
	 * 是否应启用AspectJ织入.
	 */
	AspectJWeaving aspectjWeaving() default AspectJWeaving.AUTODETECT;


	enum AspectJWeaving {

		/**
		 * 打开基于Spring的AspectJ加载时织入.
		 */
		ENABLED,

		/**
		 * 关闭基于Spring的AspectJ加载时织入 (即使类路径中存在 "META-INF/aop.xml"资源).
		 */
		DISABLED,

		/**
		 * 如果类路径中存在 "META-INF/aop.xml"资源, 打开AspectJ加载时织入.
		 * 如果没有这样的资源, 那么将关闭AspectJ加载时织入.
		 */
		AUTODETECT;
	}

}
