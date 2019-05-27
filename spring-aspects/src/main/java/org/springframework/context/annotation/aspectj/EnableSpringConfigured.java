package org.springframework.context.annotation.aspectj;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 发信号通知当前应用程序上下文，以将依赖项注入应用于在Spring bean工厂外部实例化的非托管类
 * (通常使用{@link org.springframework.beans.factory.annotation.Configurable @Configurable}注解的类).
 *
 * <p>与Spring的{@code <context:spring-configured>} XML元素中的功能类似.
 * 通常与{@link org.springframework.context.annotation.EnableLoadTimeWeaving @EnableLoadTimeWeaving}一起使用.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SpringConfiguredConfiguration.class)
public @interface EnableSpringConfigured {

}
