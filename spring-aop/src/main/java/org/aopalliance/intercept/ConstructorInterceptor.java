package org.aopalliance.intercept;

/**
 * 拦截新对象的构建.
 *
 * <p>用户应该实现 {@link #construct(ConstructorInvocation)}方法修改原始行为.
 * E.g. 下面的类实现了一个单例拦截器 (仅允许拦截类的一个唯一实例):
 *
 * <pre class=code>
 * class DebuggingInterceptor implements ConstructorInterceptor {
 *   Object instance=null;
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     if(instance==null) {
 *       return instance=i.proceed();
 *     } else {
 *       throw new Exception("singleton does not allow multiple instance");
 *     }
 *   }
 * }
 * </pre>
 */
public interface ConstructorInterceptor extends Interceptor  {

	/**
	 * 实现此方法以在构造新对象之前和之后执行额外处理.
	 * 礼貌的实现肯定会调用 {@link Joinpoint#proceed()}.
	 * 
	 * @param invocation 构造方法连接点
	 * 
	 * @return 新创建的对象, 也是调用 {@link Joinpoint#proceed()}的结果; 可能被拦截器取代
	 * @throws Throwable 如果拦截器或目标对象抛出异常
	 */
	Object construct(ConstructorInvocation invocation) throws Throwable;

}
