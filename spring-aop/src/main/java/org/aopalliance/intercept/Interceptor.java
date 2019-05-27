package org.aopalliance.intercept;

import org.aopalliance.aop.Advice;

/**
 * 此接口表示通用拦截器.
 *
 * <p>通用拦截器可以拦截基本程序中发生的运行时事件. 这些事件由（在其中）连接点实现. 运行时连接点可以被调用, 字段访问, 异常... 
 *
 * <p>此接口不直接使用. 使用子接口拦截特定事件. 例如, 下面的类实现了一些特定的拦截器, 以实现调试器:
 *
 * <pre class=code>
 * class DebuggingInterceptor implements MethodInterceptor, 
 *     ConstructorInterceptor, FieldInterceptor {
 *
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     debug(i.getMethod(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     debug(i.getConstructor(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 * 
 *   Object get(FieldAccess fa) throws Throwable {
 *     debug(fa.getField(), fa.getThis(), null);
 *     return fa.proceed();
 *   }
 *
 *   Object set(FieldAccess fa) throws Throwable {
 *     debug(fa.getField(), fa.getThis(), fa.getValueToSet());
 *     return fa.proceed();
 *   }
 *
 *   void debug(AccessibleObject ao, Object this, Object value) {
 *     ...
 *   }
 * }
 * </pre>
 */
public interface Interceptor extends Advice {

}
