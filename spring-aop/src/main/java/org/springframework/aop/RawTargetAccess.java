package org.springframework.aop;

/**
 * AOP代理接口的标记 (特别是: 介绍接口), 明确打算返回原始目标对象 (从方法调用返回时通常会被代理对象替换).
 *
 * <p>请注意，这是 {@link java.io.Serializable}风格的标记接口, 语义上应用于声明的接口, 而不是具体对象的完整类.
 * 换句话说, 此标记仅适用于特定接口 (通常是不作为AOP代理的主要接口的介绍接口), 因此不会影响具体AOP代理可能实现的其他接口.
 */
public interface RawTargetAccess {

}
