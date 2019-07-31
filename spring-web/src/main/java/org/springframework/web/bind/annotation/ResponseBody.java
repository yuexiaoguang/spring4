package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示方法返回值应绑定到Web响应主体的注解.
 * 支持Servlet环境中带注解的处理器方法.
 *
 * <p>从版本4.0开始, 此注解也可以在类型级别上添加, 在这种情况下它是继承的, 不需要再在方法级别添加.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseBody {

}
