package org.springframework.core;

/**
 * 通过装饰代理实现的接口, 特别是Spring AOP代理, 但也可能是具有装饰器语义的自定义代理.
 *
 * <p>请注意, 如果装饰类不在代理类的层次结构中, 则应该实现此接口.
 * 特别是, 诸如Spring AOP CGLIB代理之类的"target-class"代理不应该实现它,
 * 因为目标类的任何查找都可以在代理类上执行.
 *
 * <p>在核心模块中定义, 以允许#{@link org.springframework.core.annotation.AnnotationAwareOrderComparator}
 * (以及没有spring-aop依赖的其他潜在候选者) 将其用于内省目的, 特别是注解查找.
 */
public interface DecoratingProxy {

	/**
	 * 返回此代理后面的(终极)装饰类.
	 * <p>对于AOP代理, 这将是最终目标类, 而不仅仅是直接目标 (在多个嵌套代理的情况下).
	 * 
	 * @return 装饰的类 (never {@code null})
	 */
	Class<?> getDecoratedClass();

}
