/**
 * SPI包允许Spring AOP框架处理任意类型增强.
 *
 * <p>只想使用Spring AOP框架而不是扩展其功能的用户, 不需要关心这个包.
 *
 * <p>您可能希望使用这些适配器来包装特定于Spring的增强, 例如 MethodBeforeAdvice,
 * 在MethodInterceptor中, 允许在另一个支持AOP联盟接口的AOP框架中使用它们.
 *
 * <p>这些适配器不依赖于任何其他Spring框架类来允许此类使用.
 */
package org.springframework.aop.framework.adapter;
