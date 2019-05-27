package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将构造函数, 字段, setter方法, 配置方法标记为由Spring的依赖注入工具自动装配.
 *
 * <p>任何给定bean类只有一个构造函数（最大值）可以携带此注解, 指示构造函数在用作Spring bean时要自动装配.
 * 这样的构造函数不必是 public.
 *
 * <p>在构建bean之后立即注入字段, 在调用任何配置方法之前. 这样的配置字段不必是public的.
 *
 * <p>配置方法可以具有任意名称和任意数量的参数; 每个参数都将使用Spring容器中的匹配bean进行自动装配.
 * Bean属性setter方法实际上只是这种通用配置方法的一个特例. 这样的配置字段不必是public的.
 *
 * <p>在多参构造函数或方法的情况下, 'required' 参数适用于所有参数.
 * 单个参数可以声明为Java-8风格{@link java.util.Optional}, 覆盖基础所需的语义.
 *
 * <p>如果是{@link java.util.Collection}或{@link java.util.Map}依赖类型,
 * 容器自动装配所有与声明的值类型匹配的bean.
 * 出于这样的目的, 必须将map键声明为String类型, 键将被解析为相应的bean名称.
 * 将排序容器提供的集合, 考虑目标组件的
 * {@link org.springframework.core.Ordered}/{@link org.springframework.core.annotation.Order}值,
 * 否则在容器中注册.
 * 另外, 单个匹配的目标bean也可以是一般类型的{@code Collection}或{@code Map}本身, 如此注入.
 *
 * <p>Note: 实际的注入通过
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}执行,
 * 反过来意味着不能使用{@code @Autowired} 注入引用到
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}或
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}类型.
 * 请参考{@link AutowiredAnnotationBeanPostProcessor}类的javadoc (默认情况下, 它会检查是否存在此注解).
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	/**
	 * 声明是否需要带注解的依赖项.
	 * <p>默认 {@code true}.
	 */
	boolean required() default true;

}
