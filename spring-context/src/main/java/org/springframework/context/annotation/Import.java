package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示要导入的一个或多个{@link Configuration @Configuration}类.
 *
 * <p>提供与Spring XML中的{@code <import/>}元素等效的功能.
 * 允许导入{@code @Configuration}类, {@link ImportSelector}和{@link ImportBeanDefinitionRegistrar}实现,
 * 以及常规组件类 (截至4.2; 类似于{@link AnnotationConfigApplicationContext#register}).
 *
 * <p>应使用{@link org.springframework.beans.factory.annotation.Autowired @Autowired}注入,
 * 来访问在导入的{@code @Configuration}类中声明的{@code @Bean}定义.
 * bean本身可以自动装配, 或者声明bean的配置类实例可以自动装配.
 * 后一种方法允许在{@code @Configuration}类方法之间进行明确的, IDE友好的导航.
 *
 * <p>可以在类级别声明或作为元注解声明.
 *
 * <p>如果需要导入XML或其他非{@code @Configuration} bean定义资源, 请使用{@link ImportResource @ImportResource}注解.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

	/**
	 * 要导入的{@link Configuration}, {@link ImportSelector}, {@link ImportBeanDefinitionRegistrar}以及常规组件类.
	 */
	Class<?>[] value();

}
