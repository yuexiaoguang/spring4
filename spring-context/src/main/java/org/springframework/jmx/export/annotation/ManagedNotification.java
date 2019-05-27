package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类型级注解, 指示bean发出的JMX通知.
 *
 * <p>从Spring Framework 4.2.4开始, 此注解被声明为可重复的.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Repeatable(ManagedNotifications.class)
public @interface ManagedNotification {

	String name();

	String description() default "";

	String[] notificationTypes();

}
