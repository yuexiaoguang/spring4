package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示是否要延迟初始化bean.
 *
 * <p>可以在直接或间接使用{@link org.springframework.stereotype.Component @Component}注解的类或使用{@link Bean @Bean}注解的方法上使用.
 *
 * <p>如果{@code @Component}或{@code @Bean}定义中没有此注解, 则会实时的初始化.
 * 如果存在并设置为{@code true}, 则@code @Bean}或{@code @Component}将不会被初始化,
 * 直到被另一个bean引用或从封闭的{@link org.springframework.beans.factory.BeanFactory BeanFactory}中显式检索.
 * 如果存在并设置为 {@code false}, bean将在启动时由bean工厂实例化, 这些工厂执行单例的实时初始化.
 *
 * <p>如果{@link Configuration @Configuration}类中存在Lazy, 则表示{@code @Configuration}中的所有{@code @Bean}方法都应该被延迟地初始化.
 * 如果在带{@code @Lazy}注解的{@code @Configuration}类中的{@code @Bean}方法中存在{@code @Lazy}且为false,
 * 这表明覆盖了'默认延迟'行为, 并且应该实时地初始化bean.
 *
 * <p>除了组件初始化的作用外, 此注解还可以放在标有
 * {@link org.springframework.beans.factory.annotation.Autowired}或{@link javax.inject.Inject}的注入点上:
 * 在该上下文中, 它导致为所有受影响的依赖项创建一个延迟解析代理, 
 * 作为使用{@link org.springframework.beans.factory.ObjectFactory}或{@link javax.inject.Provider}的替代方法.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lazy {

	/**
	 * 是否延迟初始化.
	 */
	boolean value() default true;

}
