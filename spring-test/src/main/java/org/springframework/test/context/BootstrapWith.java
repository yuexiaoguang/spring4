package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @BootstrapWith}定义了类级元数据, 用于确定如何引导<em>Spring TestContext Framework</em>.
 *
 * <p>此注解也可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BootstrapWith {

	/**
	 * 用于引导<em>Spring TestContext Framework</em>的{@link TestContextBootstrapper}.
	 */
	Class<? extends TestContextBootstrapper> value() default TestContextBootstrapper.class;

}
