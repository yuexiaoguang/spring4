package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.Assert;

/**
 * <p>子类只需要扩展这个类并实现自己引入的接口. 在这种情况下，委托是子类实例本身.
 * 或者单独的委托可以实现该接口, 并通过委托bean属性设置.
 *
 * <p>委托或子类可以实现任意数量的接口. 默认情况下, 除了IntroductionInterceptor之外的所有接口都从子类或委托中获取.
 *
 * <p>{@code suppressInterface}方法可用于取消委托实现的接口，但不应将其引入拥有的AOP代理.
 *
 * <p>如果委托是可序列化的，则此类的实例是可序列化的.
 */
@SuppressWarnings("serial")
public class DelegatingIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * 实际实现接口的对象.
	 * 可能是"this", 如果子类实现引入的接口.
	 */
	private Object delegate;


	/**
	 * 提供实现要引入的接口的委托.
	 * 
	 * @param delegate 实现引入接口的委托
	 */
	public DelegatingIntroductionInterceptor(Object delegate) {
		init(delegate);
	}

	/**
	 * 委托将是子类, 必须实现其他接口.
	 */
	protected DelegatingIntroductionInterceptor() {
		init(this);
	}


	/**
	 * 两个构造函数都使用此init方法, 因为不可能将“this”引用从一个构造函数传递给另一个构造函数.
	 * 
	 * @param delegate 委托对象
	 */
	private void init(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		implementInterfacesOnObject(delegate);

		// We don't want to expose the control interface
		suppressInterface(IntroductionInterceptor.class);
		suppressInterface(DynamicIntroductionAdvice.class);
	}


	/**
	 * 如果子类要在around增强中执行自定义行为，则可能需要重写此类.
	 * 但是, 子类应该调用此方法, 它处理引入的接口并转发到目标.
	 */
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (isMethodOnIntroducedInterface(mi)) {
			// 使用以下方法而不是直接反射, 正确处理InvocationTargetException,
			// 如果引入的方法抛出异常.
			Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());

			// 如果可能，按摩返回值: 如果委托返回自身, 我们真的想要返回代理.
			if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {
				Object proxy = ((ProxyMethodInvocation) mi).getProxy();
				if (mi.getMethod().getReturnType().isInstance(proxy)) {
					retVal = proxy;
				}
			}
			return retVal;
		}

		return doProceed(mi);
	}

	/**
	 * 使用提供的{@link org.aopalliance.intercept.MethodInterceptor}处理.
	 * 子类可以重写此方法以拦截目标对象上的方法调用，这在引入需要监视引入它的对象时非常有用.
	 * 在引入的接口上永远不会为{@link MethodInvocation MethodInvocations}调用此方法.
	 */
	protected Object doProceed(MethodInvocation mi) throws Throwable {
		// 如果到了这里, 只需传递调用.
		return mi.proceed();
	}

}
