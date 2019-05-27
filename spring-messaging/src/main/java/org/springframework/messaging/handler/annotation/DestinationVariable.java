package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示方法参数应绑定到目标模板字符串中的模板变量.
 * 支持消息处理方法, 例如{@link MessageMapping @MessageMapping}.
 * <p>
 * 始终需要{@code @DestinationVariable}模板变量.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DestinationVariable {

	/**
	 * 要绑定的目标模板变量的名称.
	 */
	String value() default "";

}
