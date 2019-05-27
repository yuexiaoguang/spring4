package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.filter.TypeFilter;

/**
 * 配置组件扫描指令, 以与 @{@link Configuration}类一起使用.
 * 提供与Spring XML {@code <context:component-scan>}元素并行的支持.
 *
 * <p>可以指定{@link #basePackageClasses}或{@link #basePackages} (或其别名{@link #value})来定义要扫描的特定包.
 * 如果未定义特定包, 则将从声明此注解的类的包进行扫描.
 *
 * <p>请注意, {@code <context:component-scan>}元素具有{@code annotation-config}属性;
 * 但是, 这个注解没有.
 * 这是因为在使用{@code @ComponentScan}的几乎所有情况下, 都假设默认注解配置处理 (e.g. 处理 {@code @Autowired}和朋友).
 * 此外, 使用{@link AnnotationConfigApplicationContext}时, 注解配置处理器始终注册,
 * 这意味着将忽略在{@code @ComponentScan}级别禁用它们的任何尝试.
 *
 * <p>有关用法示例, 请参阅{@link Configuration @Configuration}的Javadoc.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(ComponentScans.class)
public @interface ComponentScan {

	/**
	 * {@link #basePackages}的别名.
	 * <p>如果不需要其他属性, 则允许更简洁的注解声明 &mdash;
	 * 例如, {@code @ComponentScan("org.my.pkg")}, 而不是{@code @ComponentScan(basePackages = "org.my.pkg")}.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * 用于扫描带注解的组件的基础包.
	 * <p>{@link #value}是此属性的别名 (并且与其互斥).
	 * <p>使用{@link #basePackageClasses}作为基于字符串的包名称的类型安全的替代.
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * {@link #basePackages}的类型安全的替代方法, 用于指定要扫描的带注解的组件的包.
	 * 将扫描指定的每个类的包.
	 * <p>考虑在每个包中创建一个特殊的无操作标记类或接口, 除了被该属性引用之外没有其它用途.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * {@link BeanNameGenerator}类, 用于命名Spring容器中检测到的组件.
	 * <p>{@link BeanNameGenerator}接口的默认值表示用于处理此{@code @ComponentScan}注解的扫描程序应使用其继承的bean名称生成器,
	 * e.g. 在引导时提供给应用程序上下文的默认的{@link AnnotationBeanNameGenerator}或任何自定义实例.
	 */
	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

	/**
	 * 用于解析检测到的组件的范围的{@link ScopeMetadataResolver}.
	 */
	Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;

	/**
	 * 指示是否应为检测到的组件生成代理, 这在以代理方式使用范围时可能是必需的.
	 * <p>默认遵循用于执行实际扫描的组件扫描程序的默认行为.
	 * <p>请注意, 设置此属性会覆盖为{@link #scopeResolver}设置的任何值.
	 */
	ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

	/**
	 * 控制符合组件检测条件的类文件.
	 * <p>考虑使用 {@link #includeFilters}和{@link #excludeFilters}来获得更灵活的方法.
	 */
	String resourcePattern() default ClassPathScanningCandidateComponentProvider.DEFAULT_RESOURCE_PATTERN;

	/**
	 * 指示是否应启用使用带{@code @Component} {@code @Repository}, {@code @Service}, {@code @Controller}注解的类的自动检测.
	 */
	boolean useDefaultFilters() default true;

	/**
	 * 指定哪些类型符合组件扫描的条件.
	 * <p>进一步将候选组件集从{@link #basePackages}中的所有内容, 缩小到与给定过滤器匹配的基本包中的所有内容.
	 * <p>请注意, 除了默认过滤器, 还将应用这些过滤器.
	 * 将包含与给定过滤器匹配的指定基础包下的所有类型, 即使它与默认过滤器不匹配 (i.e. 未使用{@code @Component}注解).
	 */
	Filter[] includeFilters() default {};

	/**
	 * 指定哪些类型不符合组件扫描的条件.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * 指定是否应注册扫描的bean, 以进行延迟初始化.
	 * <p>默认是 {@code false}; 需要时切换为{@code true}.
	 */
	boolean lazyInit() default false;


	/**
	 * 要用作 {@linkplain ComponentScan#includeFilters 包含过滤器}或{@linkplain ComponentScan#excludeFilters 排除过滤器}的类型过滤器.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {

		/**
		 * 要使用的过滤器类型.
		 * <p>默认是 {@link FilterType#ANNOTATION}.
		 */
		FilterType type() default FilterType.ANNOTATION;

		/**
		 * {@link #classes}的别名.
		 */
		@AliasFor("classes")
		Class<?>[] value() default {};

		/**
		 * 要用作过滤器的类.
		 * <p>下表说明了如何根据{@link #type}属性的配置值解释类.
		 * <table border="1">
		 * <tr><th>{@code FilterType}</th><th>类解释为</th></tr>
		 * <tr><td>{@link FilterType#ANNOTATION ANNOTATION}</td>
		 * <td>注解本身</td></tr>
		 * <tr><td>{@link FilterType#ASSIGNABLE_TYPE ASSIGNABLE_TYPE}</td>
		 * <td>检测到的组件可以分配给的类型</td></tr>
		 * <tr><td>{@link FilterType#CUSTOM CUSTOM}</td>
		 * <td>{@link TypeFilter}的实现</td></tr>
		 * </table>
		 * <p>指定多个类时, 将应用<em>OR</em>逻辑 &mdash; 例如, "包括用{@code @Foo} OR {@code @Bar}注解的类型".
		 * <p>自定义{@link TypeFilter TypeFilters}可以选择实现以下任何{@link org.springframework.beans.factory.Aware Aware}接口,
		 * 并在{@link TypeFilter#match match}之前调用它们各自的方法:
		 * <ul>
		 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
		 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
		 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
		 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
		 * </ul>
		 * <p>允许指定零类, 但不会影响组件扫描.
		 */
		@AliasFor("value")
		Class<?>[] classes() default {};

		/**
		 * 用于过滤器的模式, 作为指定类{@link #value}的替代方法.
		 * <p>如果{@link #type}设置为{@link FilterType#ASPECTJ ASPECTJ}, 这是一个AspectJ类型模式表达式.
		 * 如果{@link #type}设置为{@link FilterType#REGEX REGEX}, 这是要匹配的完全限定类名的正则表达式模式.
		 */
		String[] pattern() default {};

	}
}
