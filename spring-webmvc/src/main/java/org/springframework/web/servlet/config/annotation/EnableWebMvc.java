package org.springframework.web.servlet.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将此注解添加到{@code @Configuration}类可从{@link WebMvcConfigurationSupport}导入Spring MVC配置, e.g.:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyWebConfiguration {
 *
 * }
 * </pre>
 *
 * <p>要自定义导入的配置, 实现接口{@link WebMvcConfigurer}或更可能扩展空方法基类 {@link WebMvcConfigurerAdapter},
 * 并覆盖单个方法, e.g.:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurerAdapter {
 *
 * 	   &#064;Override
 * 	   public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 * 	   }
 *
 * 	   &#064;Override
 * 	   public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
 *         converters.add(new MyHttpMessageConverter());
 * 	   }
 *
 *     // More overridden methods ...
 * }
 * </pre>
 *
 * <p>如果{@link WebMvcConfigurer}未公开某些需要配置的高级设置,
 * 考虑删除{@code @EnableWebMvc}注解, 并直接从{@link WebMvcConfigurationSupport}
 * 或{@link DelegatingWebMvcConfiguration}进行扩展, e.g.:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurationSupport {
 *
 * 	   &#064;Override
 *	   public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *	   }
 *
 *	   &#064;Bean
 *	   public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
 *         // Create or delegate to "super" to create and
 *         // customize properties of RequestMappingHandlerAdapter
 *	   }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
