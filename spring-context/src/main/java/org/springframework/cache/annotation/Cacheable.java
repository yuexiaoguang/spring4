package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

import org.springframework.core.annotation.AliasFor;

/**
 * 可以缓存调用方法(或类中的所有方法)的结果的注解.
 *
 * <p>每次调用一个建议的方法时, 都会应用缓存行为, 检查是否已经为给定的参数调用了该方法.
 * 合理的默认值只是使用方法参数来计算键, 但可以通过{@link #key}属性提供SpEL表达式,
 * 或者自定义的{@link org.springframework.cache.interceptor.KeyGenerator}实现替换默认的实现 (see {@link #keyGenerator}).
 *
 * <p>如果在计算键的缓存中未找到任何值, 则将调用目标方法, 并将返回的值存储在关联的缓存中.
 * 请注意, Java8的{@code Optional}返回类型会自动处理, 并且其内容存储在缓存中.
 *
 * <p>此注解可用作<em>元注解</em>以使用属性覆盖创建自定义的 <em>组合注解</em>.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Cacheable {

	/**
	 * {@link #cacheNames}的别名.
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 存储方法调用结果的缓存的名称.
	 * <p>名称可用于确定目标缓存, 匹配特定bean定义的限定符值或bean名称.
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * 用于动态计算键的Spring Expression Language(SpEL)表达式.
	 * <p>默认是 {@code ""}, 意味着除非已配置自定义{@link #keyGenerator}, 否则所有方法参数都被视为键.
	 * <p>SpEL表达式针对提供以下元数据的专用上下文进行评估:
	 * <ul>
	 * <li>{@code #root.method}, {@code #root.target}, {@code #root.caches}
	 * 分别用于 {@link java.lang.reflect.Method method}, 目标对象, 受影响的缓存的引用.</li>
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
	 * 要使用的自定义{@link org.springframework.cache.interceptor.CacheResolver}的bean名称.
	 */
	String cacheResolver() default "";

	/**
	 * Spring Expression Language (SpEL) 表达式, 用于使方法缓存条件.
	 * <p>默认是 {@code ""}, 意味着方法结果总是被缓存.
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
	 * Spring Expression Language (SpEL) 用于否决方法缓存的表达式.
	 * <p>与{@link #condition}不同, 此表达式在调用方法后进行计算, 因此可以推断 {@code result}.
	 * <p>默认是 {@code ""}, 意味着缓存永远不会被否决.
	 * <p>SpEL表达式针对提供以下元数据的专用上下文进行评估:
	 * <ul>
	 * <li>{@code #result}用于引用方法调用的结果.
	 * 对于{@code Optional}等受支持的包装器, {@code #result} 指的是实际的对象, 而不是包装器</li>
	 * <li>{@code #root.method}, {@code #root.target}, {@code #root.caches}
	 * 分别用于{@link java.lang.reflect.Method method}, 目标对象, 受影响的缓存的引用.</li>
	 * <li>方法名称 ({@code #root.methodName}) 和目标类 ({@code #root.targetClass}) 的快捷方式也可用.
	 * <li>方法参数可以通过索引访问. 例如, 第二个参数可以通过 {@code #root.args[1]}, {@code #p1} 或{@code #a1}访问.
	 * 如果该信息可用, 也可以通过名称访问参数.</li>
	 * </ul>
	 */
	String unless() default "";

	/**
	 * 如果多个线程正在尝试加载同一个键的值, 则同步底层方法的调用.
	 * 同步导致了一些限制:
	 * <ol>
	 * <li>{@link #unless()}不受支持</li>
	 * <li>只能指定一个缓存</li>
	 * <li>不能组合其他与缓存相关的操作</li>
	 * </ol>
	 * 这实际上是一个提示, 您正在使用的实际缓存提供程序可能不会以同步方式支持它.
	 * 有关实际语义的更多详细信息, 请查看提供程序文档.
	 */
	boolean sync() default false;

}
