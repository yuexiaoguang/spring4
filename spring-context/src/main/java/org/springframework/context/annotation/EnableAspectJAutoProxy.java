package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用支持处理标有AspectJ的{@code @Aspect}注解的组件,
 * 类似于Spring的 {@code <aop:aspectj-autoproxy>} XML元素中的功能.
 * 要在 @{@link Configuration}类上使用, 如下所示:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService();
 *     }
 *
 *     &#064;Bean
 *     public MyAspect myAspect() {
 *         return new MyAspect();
 *     }
 * }</pre>
 *
 * 其中{@code FooService}是典型的POJO组件, {@code MyAspect}是{@code @Aspect}风格的切面:
 *
 * <pre class="code">
 * public class FooService {
 *
 *     // various methods
 * }</pre>
 *
 * <pre class="code">
 * &#064;Aspect
 * public class MyAspect {
 *
 *     &#064;Before("execution(* FooService+.*(..))")
 *     public void advice() {
 *         // advise FooService methods as appropriate
 *     }
 * }</pre>
 *
 * 在上面的场景中, {@code @EnableAspectJAutoProxy} 确保 {@code MyAspect}将被正确处理, {@code FooService}将被代理混合在它提供的增强中.
 *
 * <p>用户可以使用 {@link #proxyTargetClass()}属性控制为{@code FooService}创建的代理类型.
 * 以下是启用CGLIB样式的“子类”代理, 而不是基于默认接口的JDK代理方法.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
 * public class AppConfig {
 *     // ...
 * }</pre>
 *
 * <p>请注意, {@code @Aspect} bean可以像任何其他bean一样进行组件扫描.
 * 只需使用{@code @Aspect}和{@code @Component}标记切面:
 *
 * <pre class="code">
 * package com.foo;
 *
 * &#064;Component
 * public class FooService { ... }
 *
 * &#064;Aspect
 * &#064;Component
 * public class MyAspect { ... }</pre>
 *
 * 然后使用 @{@link ComponentScan}注解来选择它们:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.foo")
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     // no explicit &#064Bean definitions required
 * }</pre>
 *
 * <b>Note: 仅适用于其本地应用程序上下文, 允许在不同级别选择性地代理bean.</b>
 * 请在每个单独的上下文中重新声明 {@code @EnableAspectJAutoProxy},
 * e.g. 公共根Web应用程序上下文和任何单独的{@code DispatcherServlet}应用程序上下文, 如果您需要在多个级别应用其行为.
 *
 * <p>此功能要求在类路径中存在{@code aspectjweaver}.
 * 虽然{@code spring-aop}的依赖性是可选的, 但{@code @EnableAspectJAutoProxy}及其底层功能需要它.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * 指示是否要创建基于子类的 (CGLIB) 代理, 而不是基于标准Java接口的代理.
	 * 默认{@code false}.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指示代理应由AOP框架公开为{@code ThreadLocal}, 以便通过{@link org.springframework.aop.framework.AopContext}类进行检索.
	 * 默认关闭, 即不保证{@code AopContext}访问将起作用.
	 */
	boolean exposeProxy() default false;

}
