package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>测试注解, 指示在启动事务<em>之前</em>, 应该执行带注解的{@code void}方法,
 * 用于配置的测试方法通过 Spring的 {@code @Transactional}注解在事务中运行.
 *
 * <p>在超类中声明的{@code @BeforeTransaction}方法或作为接口默认方法的方法将在当前测试类的方法之前执行.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 *
 * <p>从Spring Framework 4.3开始, {@code @BeforeTransaction}也可以在基于Java 8的接口默认方法上声明.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeforeTransaction {
}
