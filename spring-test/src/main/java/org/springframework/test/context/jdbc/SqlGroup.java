package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 容器注解, 聚合了多个{@link Sql @Sql}注解.
 *
 * <p>可以本机使用, 声明几个嵌套的{@code @Sql}注解.
 * 也可以与Java 8对可重复注解的支持结合使用, 其中{@code @Sql}可以简单地在同一个类或方法上多次声明, 隐式生成此容器注解.
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlGroup {

	Sql[] value();

}
