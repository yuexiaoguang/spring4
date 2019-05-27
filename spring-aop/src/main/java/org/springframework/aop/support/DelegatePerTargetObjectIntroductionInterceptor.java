package org.springframework.aop.support;

import java.util.Map;
import java.util.WeakHashMap;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * <p>这与{@link DelegatingIntroductionInterceptor}的不同之处在于, 此类的单个实例可用于增强多个目标对象,
 * 每个目标对象都有自己的委托 (而DelegatingIntroductionInterceptor共享同一个委托, 因此所有目标都处于同一状态).
 *
 * <p>{@code suppressInterface}方法可用于取消委托类实现的接口，但不应将其引入拥有的AOP代理.
 *
 * <p>如果代理是可序列化的，则此类的实例是可序列化的.
 *
 * <p><i>Note: 这个类与{@link DelegatingIntroductionInterceptor}之间存在一些实现相似之处, 这表明可能的重构将在未来提取一个共同的祖先类.</i>
 */
@SuppressWarnings("serial")
public class DelegatePerTargetObjectIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * 保持对键的弱引用，因为我们不想干扰垃圾回收..
	 */
	private final Map<Object, Object> delegateMap = new WeakHashMap<Object, Object>();

	private Class<?> defaultImplType;

	private Class<?> interfaceType;


	public DelegatePerTargetObjectIntroductionInterceptor(Class<?> defaultImplType, Class<?> interfaceType) {
		this.defaultImplType = defaultImplType;
		this.interfaceType = interfaceType;
		// 立即创建一个新的委托 (但不要将其存储在map中).
		// 这样做有两个原因:
		// 1) 如果在实例化代理时出现问题，则提前失败
		// 2) 仅填充一次接口map
		Object delegate = createNewDelegate();
		implementInterfacesOnObject(delegate);
		suppressInterface(IntroductionInterceptor.class);
		suppressInterface(DynamicIntroductionAdvice.class);
	}


	/**
	 * 如果子类要在around建议中执行自定义行为，则可能需要重写此类.
	 * 但是, 子类应该调用此方法, 它处理引入的接口并转发到目标.
	 */
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (isMethodOnIntroducedInterface(mi)) {
			Object delegate = getIntroductionDelegateFor(mi.getThis());

			// 使用以下方法而不是直接反射, 我们正确处理InvocationTargetException,
			// 如果引入的方法抛出异常.
			Object retVal = AopUtils.invokeJoinpointUsingReflection(delegate, mi.getMethod(), mi.getArguments());

			// 如果可能，按摩返回值: 如果委托返回自身, 我们真的想要返回代理.
			if (retVal == delegate && mi instanceof ProxyMethodInvocation) {
				retVal = ((ProxyMethodInvocation) mi).getProxy();
			}
			return retVal;
		}

		return doProceed(mi);
	}

	/**
	 * 使用提供的{@link org.aopalliance.intercept.MethodInterceptor}处理.
	 * 子类可以重写此方法以拦截目标对象上的方法调用, 这在引入需要监视引入它的对象时非常有用.
	 * 在引入的接口上永远不会为{@link MethodInvocation MethodInvocations}调用此方法.
	 */
	protected Object doProceed(MethodInvocation mi) throws Throwable {
		// 如果到了这里, 只需传递调用.
		return mi.proceed();
	}

	private Object getIntroductionDelegateFor(Object targetObject) {
		synchronized (this.delegateMap) {
			if (this.delegateMap.containsKey(targetObject)) {
				return this.delegateMap.get(targetObject);
			}
			else {
				Object delegate = createNewDelegate();
				this.delegateMap.put(targetObject, delegate);
				return delegate;
			}
		}
	}

	private Object createNewDelegate() {
		try {
			return this.defaultImplType.newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot create default implementation for '" +
					this.interfaceType.getName() + "' mixin (" + this.defaultImplType.getName() + "): " + ex);
		}
	}

}
