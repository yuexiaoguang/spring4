package org.aopalliance.intercept;

import java.lang.reflect.Method;

/**
 * 调用方法的描述, 给方法调用上的拦截器.
 *
 * <p>方法调用是一个连接点，可以被方法拦截器截获.
 */
public interface MethodInvocation extends Invocation {

	/**
	 * 获取被调用的方法.
	 * <p>此方法是{@link Joinpoint#getStaticPart()}方法的友好实现 (相同结果).
	 * 
	 * @return 被调用的方法
	 */
	Method getMethod();

}
