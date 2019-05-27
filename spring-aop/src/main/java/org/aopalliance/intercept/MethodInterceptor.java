package org.aopalliance.intercept;

/**
 * 在到达目标的路上拦截对接口的调用. 它们嵌套在目标的“顶部”.
 *
 * <p>用户应该实现{@link #invoke(MethodInvocation)} 方法修改原始行为.
 * E.g. 以下类实现了一个跟踪拦截器 (跟踪拦截方法上的所有调用):
 *
 * <pre class=code>
 * class TracingInterceptor implements MethodInterceptor {
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     System.out.println("method "+i.getMethod()+" is called on "+
 *                        i.getThis()+" with args "+i.getArguments());
 *     Object ret=i.proceed();
 *     System.out.println("method "+i.getMethod()+" returns "+ret);
 *     return ret;
 *   }
 * }
 * </pre>
 */
public interface MethodInterceptor extends Interceptor {
	
	/**
	 * 实现此方法以在调用之前和之后执行额外的处理.
	 * 礼貌的实现肯定会调用 {@link Joinpoint#proceed()}.
	 * 
	 * @param invocation 方法调用连接点
	 * 
	 * @return 调用 {@link Joinpoint#proceed()}的结果; 可能会被拦截器拦截
	 * @throws Throwable 如果拦截器或目标对象抛出异常
	 */
	Object invoke(MethodInvocation invocation) throws Throwable;

}
