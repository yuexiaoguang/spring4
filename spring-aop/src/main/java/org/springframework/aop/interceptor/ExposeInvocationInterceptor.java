package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.PriorityOrdered;

/**
 * 将当前{@link org.aopalliance.intercept.MethodInvocation}公开为thread-local 对象的拦截器.
 * 我们偶尔需要这样做; 例如, 当切点（例如，AspectJ表达式切点）需要知道完整的调用上下文时.
 *
 * <p>除非确实有必要, 否则不要使用此拦截器. 目标对象通常不应该知道Spring AOP, 因为这会产生对Spring API的依赖.
 * 目标对象应尽可能是普通的POJO.
 *
 * <p>如果使用，这个拦截器通常是拦截链中的第一个.
 */
@SuppressWarnings("serial")
public class ExposeInvocationInterceptor implements MethodInterceptor, PriorityOrdered, Serializable {

	/** 这个类的Singleton实例 */
	public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

	/**
	 * 这个类的单例切面. 使用Spring AOP时优先使用INSTANCE, 因为它可以防止创建一个新的Advisor来包装实例.
	 */
	public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
		@Override
		public String toString() {
			return ExposeInvocationInterceptor.class.getName() +".ADVISOR";
		}
	};

	private static final ThreadLocal<MethodInvocation> invocation =
			new NamedThreadLocal<MethodInvocation>("Current AOP method invocation");


	/**
	 * 返回与当前调用关联的AOP Alliance MethodInvocation对象.
	 * 
	 * @return 与当前调用关联的调用对象
	 * @throws IllegalStateException 如果没有正在进行的AOP调用, 或者如果没有将ExposeInvocationInterceptor添加到此拦截器链中
	 */
	public static MethodInvocation currentInvocation() throws IllegalStateException {
		MethodInvocation mi = invocation.get();
		if (mi == null)
			throw new IllegalStateException(
					"No MethodInvocation found: Check that an AOP invocation is in progress, and that the " +
					"ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that " +
					"advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor!");
		return mi;
	}


	/**
	 * 确保只能创建规范实例.
	 */
	private ExposeInvocationInterceptor() {
	}

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		MethodInvocation oldInvocation = invocation.get();
		invocation.set(mi);
		try {
			return mi.proceed();
		}
		finally {
			invocation.set(oldInvocation);
		}
	}

	@Override
	public int getOrder() {
		return PriorityOrdered.HIGHEST_PRECEDENCE + 1;
	}

	/**
	 * 需要支持序列化. 在反序列化时替换规范实例, 保护单例模式.
	 * <p>替代覆盖{@code equals}方法.
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
