package org.aopalliance.intercept;

import java.lang.reflect.Constructor;

/**
 * 对构造方法的调用的描述, 给构造方法调用上的拦截器.
 *
 * <p>构造方法调用是一个连接点，可以由构造方法拦截器拦截.
 */
public interface ConstructorInvocation extends Invocation {

    /**
     * 获取被调用的构造方法.
     * <p>这个方法是{@link Joinpoint#getStaticPart()}方法的友好实现 (相同的结果).
     * 
     * @return 被调用的构造方法
     */
    Constructor<?> getConstructor();

}
