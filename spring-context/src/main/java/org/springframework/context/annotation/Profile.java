package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 表示当一个或多个{@linkplain #value 指定的配置文件}处于活动状态时, 组件符合注册条件.
 *
 * <p><em>配置文件</em>是一个命名的逻辑分组, 可以通过{@link ConfigurableEnvironment#setActiveProfiles}以编程方式激活,
 * 也可以通过将{@link AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME spring.profiles.active}属性设置为JVM系统属性,
 * 作为环境变量, 或者作为Web应用程序的{@code web.xml}中的Servlet上下文参数, 来声明性地激活.
 * 也可以通过{@code @ActiveProfiles}注解在集成测试中以声明方式激活配置文件.
 *
 * <p>{@code @Profile}注解可以通过以下任何方式使用:
 * <ul>
 * <li>作为直接或间接使用{@code @Component}注解的任何类的类型级注解, 包括{@link Configuration @Configuration}类</li>
 * <li>作为元注解, 用于组成自定义构造型注解</li>
 * <li>作为任何{@link Bean @Bean}方法的方法级注解</li>
 * </ul>
 *
 * <p>如果{@code @Configuration}类标有{@code @Profile}, 则将绕过与该类关联的所有{@code @Bean}方法和{@link Import @Import}注解,
 * 除非一个或多个指定的配置文件处于活动状态.
 * 类似于Spring XML中的行为:
 * 如果提供了{@code beans}元素的{@code profile}属性 e.g., {@code <beans profile="p1,p2">},
 * 除非至少激活了配置文件 'p1' 或 'p2', 否则不会解析{@code beans}元素.
 * 同样, 如果{@code @Component} 或 {@code @Configuration}类标有{@code @Profile({"p1", "p2"})},
 * 除非至少激活了配置文件 'p1'或'p2', 否则不会注册或处理该类.
 *
 * <p>如果给定的配置文件以NOT运算符 ({@code !})为前缀, 则如果配置文件不处于活动状态, 则将注册带注解的组件 &mdash;
 * 例如, 给定{@code @Profile({"p1", "!p2"})}, 如果配置文件'p1' 处于活动状态或配置文件 'p2'未激活, 则会发生注册.
 *
 * <p>如果省略{@code @Profile}注解, 则无论哪个配置文件处于活动状态, 都将进行注册.
 *
 * <p><b>NOTE:</b> 在{@code @Bean}方法上使用{@code @Profile}, 可能会应用特殊方案:
 * 在重载{@code @Bean}方法的情况下使用相同的Java方法名称 (类似于构造函数重载),
 * 需要在所有重载的方法上一致地声明{@code @Profile}条件.
 * 如果条件不一致, 则重载的方法中只有第一个声明的条件才重要.
 * 因此, {@code @Profile}不能用于选择具有特定参数签名的重载方法;
 * 同一个bean的所有工厂方法之间的解析, 在创建时遵循Spring的构造函数解析算法.
 * <b>如果要定义具有不同配置文件条件的备用bean, 请使用指向相同 {@link Bean#name bean name}的不同Java方法名称</b>;
 * 请参阅{@link Configuration @Configuration}的javadoc中的{@code ProfileDatabaseConfig}.
 *
 * <p>通过XML定义Spring bean时, 可以使用{@code <beans>}元素的{@code "profile"}属性.
 * 有关详细信息, 请参阅{@code spring-beans} XSD (3.1或更高版本) 中的文档.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {

	/**
	 * 应注册的带注解的组件的配置文件.
	 */
	String[] value();

}
