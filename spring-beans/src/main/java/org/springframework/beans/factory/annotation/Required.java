package org.springframework.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将方法(通常是JavaBean setter方法)标记为 'required': 即, 必须将setter方法配置为依赖注入值.
 *
 * <p>请查阅{@link RequiredAnnotationBeanPostProcessor}类的javadoc (默认情况下, 它会检查是否存在此注解).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Required {

}
