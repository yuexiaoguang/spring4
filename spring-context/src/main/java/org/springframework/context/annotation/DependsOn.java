package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当前bean所依赖的bean.
 * 指定的任何bean都保证在此bean之前由容器创建.
 * 在bean没有通过属性或构造函数参数显式依赖于另一个bean的情况下很少使用,
 * 但是有依赖于另一个bean的初始化的副作用.
 *
 * <p>可以在直接或间接使用{@link org.springframework.stereotype.Component}注解的类或使用{@link Bean}注解的方法上使用.
 *
 * <p>除非正在使用组件扫描, 否则在类级别使用{@link DependsOn}无效.
 * 如果通过XML声明{@link DependsOn}注解的类, 则忽略{@link DependsOn}注解元数据, 并且遵守{@code <bean depends-on="..."/>}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOn {

	String[] value() default {};

}
