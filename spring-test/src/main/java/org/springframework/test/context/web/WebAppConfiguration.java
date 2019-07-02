package org.springframework.test.context.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @WebAppConfiguration}是一个类级注解, 用于声明为集成测试加载的{@code ApplicationContext}
 * 应该是 {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}.
 *
 * <p>测试类上存在{@code @WebAppConfiguration}表示应使用Web应用程序根路径的默认值为测试加载{@code WebApplicationContext}.
 * 要覆盖默认值, 通过 {@link #value}属性指定显式资源路径.
 *
 * <p>请注意, {@code @WebAppConfiguration}必须与
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}一起使用,
 * 可以在单个测试类中, 也可以在测试类层次结构中使用.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface WebAppConfiguration {

	/**
	 * Web应用程序根目录的资源路径.
	 * <p>不包含Spring资源前缀的路径 (e.g., {@code classpath:}, {@code file:}, etc.)
	 * 将被解释为文件系统资源, 并且路径不应以斜杠结尾.
	 * <p>默认{@code "src/main/webapp"}作为文件系统资源.
	 * 请注意, 这是项目中Web应用程序根目录的标准目录, 该目录遵循WAR的标准Maven项目布局.
	 */
	String value() default "src/main/webapp";

}
