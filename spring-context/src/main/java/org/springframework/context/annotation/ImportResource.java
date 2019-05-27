package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.annotation.AliasFor;

/**
 * 指示包含要导入的bean定义的一个或多个资源.
 *
 * <p>与{@link Import @Import}一样, 此注解提供的功能类似于Spring XML中的{@code <import/>}元素.
 * 它通常在设计{@link Configuration @Configuration}类以由{@link AnnotationConfigApplicationContext}引导时使用,
 * 但是仍然需要一些XML功能, 例如命名空间.
 *
 * <p>默认情况下, 如果以{@code ".groovy"}结尾, 将使用
 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader}
 * 处理{@link #value}属性的参数;
 * 否则, 将使用{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader}
 * 解析Spring {@code <beans/>} XML文件.
 * 可选, 可以声明{@link #reader}属性, 允许用户选择自定义的{@link BeanDefinitionReader}实现.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ImportResource {

	/**
	 * {@link #locations}的别名.
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 要导入的资源位置.
	 * <p>支持资源加载前缀, 如{@code classpath:}, {@code file:}, 等.
	 * <p>有关如何处理资源的详细信息, 请参阅{@link #reader}的Javadoc.
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * 处理通过{@link #value}属性指定的资源时使用的{@link BeanDefinitionReader}实现.
	 * <p>默认情况下, 读取器将适应指定的资源路径:
	 * {@code ".groovy"}文件将使用
	 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader}进行处理;
	 * 而所有其他资源都将使用
	 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader}进行处理.
	 */
	Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
