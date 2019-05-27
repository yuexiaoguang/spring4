package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * 允许从Spring上下文以及所有带{@code @ManagedResource}注解的bean中, 默认导出所有标准{@code MBean}.
 *
 * <p>生成的 {@link org.springframework.jmx.export.MBeanExporter MBeanExporter} bean 名称为"mbeanExporter".
 * 或者, 考虑明确定义自定义{@link AnnotationMBeanExporter} bean.
 *
 * <p>此注解是后来加的, 在功能上等同于Spring XML的{@code <context:mbean-export/>}元素.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MBeanExportConfiguration.class)
public @interface EnableMBeanExport {

	/**
	 * 生成JMX ObjectNames时使用的默认域.
	 */
	String defaultDomain() default "";

	/**
	 * 应将MBean导出到的MBeanServer的bean名称.
	 * 默认是使用平台的默认MBeanServer.
	 */
	String server() default "";

	/**
	 * 尝试在已存在的{@link javax.management.ObjectName}下注册MBean时使用的策略.
	 * 默认{@link RegistrationPolicy#FAIL_ON_EXISTING}.
	 */
	RegistrationPolicy registration() default RegistrationPolicy.FAIL_ON_EXISTING;
}
