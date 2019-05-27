package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法(或类上的所有方法) 触发{@link org.springframework.cache.Cache#evict(Object) 缓存逐出}操作的注解.
 *
 * <p>此注解可用作<em>元注解</em>以使用属性覆盖创建自定义的 <em>组合注解</em>.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvict {

	/**
	 * {@link #cacheNames}的别名.
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 用于缓存逐出操作的缓存的名称.
	 * <p>用于确定目标缓存的名称, 匹配特定bean定义的限定符值或bean名称.
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * 用于动态计算键的Spring Expression Language(SpEL)表达式.
	 * <p>默认是 {@code ""}, 意味着除非已配置自定义{@link #keyGenerator}, 否则所有方法参数都被视为键.
	 * <p>SpEL表达式针对提供以下元数据的专用上下文进行评估:
	 * <ul>
	 * <li>{@code #result} 用于引用方法调用的结果, 只有在{@link #beforeInvocation()}为{@code false}时才能使用.
	 * 对于{@code Optional}等受支持的包装器, {@code #result}指的是实际的对象, 而不是包装器</li>
	 * <li>{@code #root.method}, {@code #root.target}, {@code #root.caches}
	 * 分别用于{@link java.lang.reflect.Method method}, 目标对象, 受影响的缓存的引用.</li>
	 * <li>方法名称 ({@code #root.methodName}) 和目标类 ({@code #root.targetClass}) 的快捷方式也可用.
	 * <li>方法参数可以通过索引访问. 例如, 第二个参数可以通过 {@code #root.args[1]}, {@code #p1} 或{@code #a1}访问.
	 * 如果该信息可用, 也可以通过名称访问参数.</li>
	 * </ul>
	 */
	String key() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.KeyGenerator}的bean名称.
	 * <p>与{@link #key}属性互斥.
	 */
	String keyGenerator() default "";

	/**
	 * 自定义 {@link org.springframework.cache.CacheManager}的bean名称,
	 * 用于创建默认 {@link org.springframework.cache.interceptor.CacheResolver}, 如果尚未设置.
	 * <p>与{@link #cacheResolver}属性互斥.
	 */
	String cacheManager() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.CacheResolver}的bean名称.
	 */
	String cacheResolver() default "";

	/**
	 * Spring Expression Language (SpEL) 表达式, 用于使缓存逐出操作可配置条件
	 * <p>默认是 {@code ""}, 意味着始终执行缓存逐出.
	 * <p>SpEL表达式针对提供以下元数据的专用上下文进行评估:
	 * <ul>
	 * <li>{@code #root.method}, {@code #root.target}, {@code #root.caches}
	 * 分别用于{@link java.lang.reflect.Method method}, 目标对象, 受影响的缓存的引用.</li>
	 * <li>方法名称 ({@code #root.methodName}) 和目标类 ({@code #root.targetClass}) 的快捷方式也可用.
	 * <li>方法参数可以通过索引访问. 例如, 第二个参数可以通过 {@code #root.args[1]}, {@code #p1} 或{@code #a1}访问.
	 * 如果该信息可用, 也可以通过名称访问参数.</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * 是否删除了缓存中的所有条目.
	 * <p>默认情况下, 仅删除关联键下的值.
	 * <p>请注意, 不允许将此参数设置为{@code true}并指定{@link #key}.
	 */
	boolean allEntries() default false;

	/**
	 * 是否应该在调用方法之前进行驱逐.
	 * <p>设置为 {@code true}, 无论方法结果如何, 都会导致驱逐 (i.e., 是否抛出了异常).
	 * <p>默认 {@code false}, 意味着在成功调用增强方法之后将发生缓存逐出操作 (i.e., 只有在调用没有抛出异常的情况下).
	 */
	boolean beforeInvocation() default false;

}
