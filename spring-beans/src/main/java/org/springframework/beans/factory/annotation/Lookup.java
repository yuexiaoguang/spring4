package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示'lookup'方法的注解, 要被容器覆盖, 将它们重定向回{@link org.springframework.beans.factory.BeanFactory},
 * 以进行{@code getBean}调用.
 * 这实际上是XML {@code lookup-method}属性的基于注解的版本, 导致相同的运行时安排.
 *
 * <p>目标bean的解析可以基于返回类型 ({@code getBean(Class)}) 或建议的bean名称 ({@code getBean(String)}),
 * 在两种情况下都将方法的参数传递给{@code getBean}调用, 以将它们作为目标工厂方法参数或构造函数参数应用.
 *
 * <p>这样的查找方法可以具有默认 (stub) 实现, 其将简单地被容器替换,
 * 或者它们可以被声明为抽象的 - 让容器在运行时填充它们.
 * 在这两种情况下, 容器将通过CGLIB生成方法包含类的运行时子类,
 * 这就是为什么这样的查找方法只能处理由容器通过常规构造函数实例化的bean:
 * i.e. 查询方法无法替换从工厂方法返回的bean, 我们无法动态地为它们提供子类.
 *
 * <p><b>典型Spring配置方案中的具体限制:</b>
 * 与组件扫描或任何其他过滤掉抽象bean的机制一起使用时, 提供查找方法的stub实现, 以便能够将它们声明为具体类.
 * 请记住, 查找方法不适用于从配置类中的{@code @Bean}方法返回的bean;
 * 将不得不求助于 {@code @Inject Provider&lt;TargetBean&gt;} 等.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {

	/**
	 * 此注释属性可以建议要查找的目标bean名称.
	 * 如果未指定, 将根据被注释的方法的返回类型声明来解析目标bean.
	 */
	String value() default "";

}
