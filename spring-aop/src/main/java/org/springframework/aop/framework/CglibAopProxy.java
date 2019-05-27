package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Dispatcher;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.cglib.transform.impl.UndeclaredThrowableStrategy;
import org.springframework.core.SmartClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 基于CGLIB的{@link AopProxy} Spring AOP框架实现.
 *
 * <p>应通过代理工厂获取此类对象, 由{@link AdvisedSupport}对象配置. 
 * 该类是Spring的AOP框架内部的，不需要由客户端代码直接使用.
 *
 * <p>如有必要, {@link DefaultAopProxyFactory}将自动创建基于CGLIB的代理, 例如, 在代理目标类的情况下
 * (see the {@link DefaultAopProxyFactory attendant javadoc} for details).
 *
 * <p>如果底层（目标）类是线程安全的, 则使用此类创建的代理是线程安全的.
 */
@SuppressWarnings("serial")
class CglibAopProxy implements AopProxy, Serializable {

	// CGLIB回调数组索引的常量
	private static final int AOP_PROXY = 0;
	private static final int INVOKE_TARGET = 1;
	private static final int NO_OVERRIDE = 2;
	private static final int DISPATCH_TARGET = 3;
	private static final int DISPATCH_ADVISED = 4;
	private static final int INVOKE_EQUALS = 5;
	private static final int INVOKE_HASHCODE = 6;


	/** static来优化序列化 */
	protected static final Log logger = LogFactory.getLog(CglibAopProxy.class);

	/** 跟踪为final方法验证的Classes */
	private static final Map<Class<?>, Boolean> validatedClasses = new WeakHashMap<Class<?>, Boolean>();


	/** 用于配置此代理的配置 */
	protected final AdvisedSupport advised;

	protected Object[] constructorArgs;

	protected Class<?>[] constructorArgTypes;

	/** 用于增强的方法的Dispatcher */
	private final transient AdvisedDispatcher advisedDispatcher;

	private transient Map<String, Integer> fixedInterceptorMap;

	private transient int fixedInterceptorOffset;


	/**
	 * @param config AOP配置
	 * 
	 * @throws AopConfigException 如果配置无效. 在这种情况下, 尝试抛出一个信息异常,而不是让一个神秘的失败发生.
	 */
	public CglibAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;
		this.advisedDispatcher = new AdvisedDispatcher(this.advised);
	}

	/**
	 * 设置用于创建代理的构造函数参数.
	 * 
	 * @param constructorArgs 构造函数参数值
	 * @param constructorArgTypes 构造函数参数类型
	 */
	public void setConstructorArguments(Object[] constructorArgs, Class<?>[] constructorArgTypes) {
		if (constructorArgs == null || constructorArgTypes == null) {
			throw new IllegalArgumentException("Both 'constructorArgs' and 'constructorArgTypes' need to be specified");
		}
		if (constructorArgs.length != constructorArgTypes.length) {
			throw new IllegalArgumentException("Number of 'constructorArgs' (" + constructorArgs.length +
					") must match number of 'constructorArgTypes' (" + constructorArgTypes.length + ")");
		}
		this.constructorArgs = constructorArgs;
		this.constructorArgTypes = constructorArgTypes;
	}


	@Override
	public Object getProxy() {
		return getProxy(null);
	}

	@Override
	public Object getProxy(ClassLoader classLoader) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating CGLIB proxy: target source is " + this.advised.getTargetSource());
		}

		try {
			Class<?> rootClass = this.advised.getTargetClass();
			Assert.state(rootClass != null, "Target class must be available for creating a CGLIB proxy");

			Class<?> proxySuperClass = rootClass;
			if (ClassUtils.isCglibProxyClass(rootClass)) {
				proxySuperClass = rootClass.getSuperclass();
				Class<?>[] additionalInterfaces = rootClass.getInterfaces();
				for (Class<?> additionalInterface : additionalInterfaces) {
					this.advised.addInterface(additionalInterface);
				}
			}

			// 验证类, 写入日志消息.
			validateClassIfNecessary(proxySuperClass, classLoader);

			// 配置CGLIB Enhancer...
			Enhancer enhancer = createEnhancer();
			if (classLoader != null) {
				enhancer.setClassLoader(classLoader);
				if (classLoader instanceof SmartClassLoader &&
						((SmartClassLoader) classLoader).isClassReloadable(proxySuperClass)) {
					enhancer.setUseCache(false);
				}
			}
			enhancer.setSuperclass(proxySuperClass);
			enhancer.setInterfaces(AopProxyUtils.completeProxiedInterfaces(this.advised));
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			enhancer.setStrategy(new ClassLoaderAwareUndeclaredThrowableStrategy(classLoader));

			Callback[] callbacks = getCallbacks(rootClass);
			Class<?>[] types = new Class<?>[callbacks.length];
			for (int x = 0; x < types.length; x++) {
				types[x] = callbacks[x].getClass();
			}
			// fixedInterceptorMap仅在此时填充, 在上面getCallbacks调用之后
			enhancer.setCallbackFilter(new ProxyCallbackFilter(
					this.advised.getConfigurationOnlyCopy(), this.fixedInterceptorMap, this.fixedInterceptorOffset));
			enhancer.setCallbackTypes(types);

			// 生成代理类并创建代理实例.
			return createProxyClassAndInstance(enhancer, callbacks);
		}
		catch (CodeGenerationException ex) {
			throw new AopConfigException("Could not generate CGLIB subclass of " + this.advised.getTargetClass() +
					": Common causes of this problem include using a final class or a non-visible class",
					ex);
		}
		catch (IllegalArgumentException ex) {
			throw new AopConfigException("Could not generate CGLIB subclass of " + this.advised.getTargetClass() +
					": Common causes of this problem include using a final class or a non-visible class",
					ex);
		}
		catch (Throwable ex) {
			// TargetSource.getTarget() failed
			throw new AopConfigException("Unexpected AOP exception", ex);
		}
	}

	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
		enhancer.setInterceptDuringConstruction(false);
		enhancer.setCallbacks(callbacks);
		return (this.constructorArgs != null ?
				enhancer.create(this.constructorArgTypes, this.constructorArgs) :
				enhancer.create());
	}

	/**
	 * 创建CGLIB {@link Enhancer}. 子类可能希望重写它以返回自定义{@link Enhancer}实现.
	 */
	protected Enhancer createEnhancer() {
		return new Enhancer();
	}

	/**
	 * 检查提供的{@code Class}是否已经过验证，如果没有，则验证它.
	 */
	private void validateClassIfNecessary(Class<?> proxySuperClass, ClassLoader proxyClassLoader) {
		if (logger.isWarnEnabled()) {
			synchronized (validatedClasses) {
				if (!validatedClasses.containsKey(proxySuperClass)) {
					doValidateClass(proxySuperClass, proxyClassLoader,
							ClassUtils.getAllInterfacesForClassAsSet(proxySuperClass));
					validatedClasses.put(proxySuperClass, Boolean.TRUE);
				}
			}
		}
	}

	/**
	 * 检查给定{@code Class}的final方法, 以及ClassLoaders中的包可见的方法, 并为每个找到的写入警告到日志.
	 */
	private void doValidateClass(Class<?> proxySuperClass, ClassLoader proxyClassLoader, Set<Class<?>> ifcs) {
		if (proxySuperClass != Object.class) {
			Method[] methods = proxySuperClass.getDeclaredMethods();
			for (Method method : methods) {
				int mod = method.getModifiers();
				if (!Modifier.isStatic(mod) && !Modifier.isPrivate(mod)) {
					if (Modifier.isFinal(mod)) {
						if (implementsInterface(method, ifcs)) {
							logger.warn("Unable to proxy interface-implementing method [" + method + "] because " +
									"it is marked as final: Consider using interface-based JDK proxies instead!");
						}
						logger.info("Final method [" + method + "] cannot get proxied via CGLIB: " +
								"Calls to this method will NOT be routed to the target instance and " +
								"might lead to NPEs against uninitialized fields in the proxy instance.");
					}
					else if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod) &&
							proxyClassLoader != null && proxySuperClass.getClassLoader() != proxyClassLoader) {
						logger.info("Method [" + method + "] is package-visible across different ClassLoaders " +
								"and cannot get proxied via CGLIB: Declare this method as public or protected " +
								"if you need to support invocations through the proxy.");
					}
				}
			}
			doValidateClass(proxySuperClass.getSuperclass(), proxyClassLoader, ifcs);
		}
	}

	private Callback[] getCallbacks(Class<?> rootClass) throws Exception {
		// 用于优化选择的参数...
		boolean exposeProxy = this.advised.isExposeProxy();
		boolean isFrozen = this.advised.isFrozen();
		boolean isStatic = this.advised.getTargetSource().isStatic();

		// 选择一个"aop" 拦截器 (用于AOP 调用).
		Callback aopInterceptor = new DynamicAdvisedInterceptor(this.advised);

		// 选择一个"straight to target"拦截器. (用于未经增强的调用, 但可以返回此 this). 可能需要公开代理.
		Callback targetInterceptor;
		if (exposeProxy) {
			targetInterceptor = (isStatic ?
					new StaticUnadvisedExposedInterceptor(this.advised.getTargetSource().getTarget()) :
					new DynamicUnadvisedExposedInterceptor(this.advised.getTargetSource()));
		}
		else {
			targetInterceptor = (isStatic ?
					new StaticUnadvisedInterceptor(this.advised.getTargetSource().getTarget()) :
					new DynamicUnadvisedInterceptor(this.advised.getTargetSource()));
		}

		// 选择一个"direct to target" 调度器 (用于无法返回this的静态目标的未经增强的调用).
		Callback targetDispatcher = (isStatic ?
				new StaticDispatcher(this.advised.getTargetSource().getTarget()) : new SerializableNoOp());

		Callback[] mainCallbacks = new Callback[] {
				aopInterceptor,  // 正常的增强
				targetInterceptor,  // 调用目标而不考虑增强, 如果优化
				new SerializableNoOp(),  // 没有覆盖映射到this的方法
				targetDispatcher, this.advisedDispatcher,
				new EqualsInterceptor(this.advised),
				new HashCodeInterceptor(this.advised)
		};

		Callback[] callbacks;

		// 如果目标是静态目标，而且增强链被冻结, 然后我们可以通过使用该方法的固定链将AOP调用直接发送到目标来进行一些优化.
		if (isStatic && isFrozen) {
			Method[] methods = rootClass.getMethods();
			Callback[] fixedCallbacks = new Callback[methods.length];
			this.fixedInterceptorMap = new HashMap<String, Integer>(methods.length);

			// TODO: 这里有小内存优化 (可以跳过没有增强的方法的创建)
			for (int x = 0; x < methods.length; x++) {
				List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(methods[x], rootClass);
				fixedCallbacks[x] = new FixedChainStaticTargetInterceptor(
						chain, this.advised.getTargetSource().getTarget(), this.advised.getTargetClass());
				this.fixedInterceptorMap.put(methods[x].toString(), x);
			}

			// 现在将mainCallbacks和fixedCallbacks的回调复制到回调数组中.
			callbacks = new Callback[mainCallbacks.length + fixedCallbacks.length];
			System.arraycopy(mainCallbacks, 0, callbacks, 0, mainCallbacks.length);
			System.arraycopy(fixedCallbacks, 0, callbacks, mainCallbacks.length, fixedCallbacks.length);
			this.fixedInterceptorOffset = mainCallbacks.length;
		}
		else {
			callbacks = mainCallbacks;
		}
		return callbacks;
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof CglibAopProxy &&
				AopProxyUtils.equalsInProxy(this.advised, ((CglibAopProxy) other).advised)));
	}

	@Override
	public int hashCode() {
		return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}


	/**
	 * 检查是否在任何给定接口上声明了给定方法.
	 */
	private static boolean implementsInterface(Method method, Set<Class<?>> ifcs) {
		for (Class<?> ifc : ifcs) {
			if (ClassUtils.hasMethod(ifc, method.getName(), method.getParameterTypes())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 处理返回值. 包装{@code this}的返回到{@code proxy}, 并验证没有返回{@code null}.
	 */
	private static Object processReturnType(Object proxy, Object target, Method method, Object retVal) {
		// Massage return value if necessary
		if (retVal != null && retVal == target &&
				!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
			// Special case: 它返回 "this". 请注意，如果目标在另一个返回的对象中设置对自身的引用，我们将无法帮助您.
			retVal = proxy;
		}
		Class<?> returnType = method.getReturnType();
		if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
			throw new AopInvocationException(
					"Null return value from advice does not match primitive return type for: " + method);
		}
		return retVal;
	}


	/**
	 * 可序列化替换CGLIB的NoOp接口.
	 * Public 以允许在框架中的其他地方使用.
	 */
	public static class SerializableNoOp implements NoOp, Serializable {
	}


	/**
	 * 方法拦截器用于没有增强链的静态目标. 该调用直接传递回目标.
	 * 在需要公开代理时使用，并且无法确定该方法不会返回{@code this}.
	 */
	private static class StaticUnadvisedInterceptor implements MethodInterceptor, Serializable {

		private final Object target;

		public StaticUnadvisedInterceptor(Object target) {
			this.target = target;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			Object retVal = methodProxy.invoke(this.target, args);
			return processReturnType(proxy, this.target, method, retVal);
		}
	}


	/**
	 * 方法拦截器用于没有增强链的静态目标, 当代理被公开的时候.
	 */
	private static class StaticUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {

		private final Object target;

		public StaticUnadvisedExposedInterceptor(Object target) {
			this.target = target;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			Object oldProxy = null;
			try {
				oldProxy = AopContext.setCurrentProxy(proxy);
				Object retVal = methodProxy.invoke(this.target, args);
				return processReturnType(proxy, this.target, method, retVal);
			}
			finally {
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * 拦截器用于在不创建方法调用或评估增强链的情况下调用动态目标. (我们知道这种方法没有增强.)
	 */
	private static class DynamicUnadvisedInterceptor implements MethodInterceptor, Serializable {

		private final TargetSource targetSource;

		public DynamicUnadvisedInterceptor(TargetSource targetSource) {
			this.targetSource = targetSource;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			Object target = this.targetSource.getTarget();
			try {
				Object retVal = methodProxy.invoke(target, args);
				return processReturnType(proxy, target, method, retVal);
			}
			finally {
				this.targetSource.releaseTarget(target);
			}
		}
	}


	/**
	 * 当代理需要公开时，用于未增强的动态目标的拦截器.
	 */
	private static class DynamicUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {

		private final TargetSource targetSource;

		public DynamicUnadvisedExposedInterceptor(TargetSource targetSource) {
			this.targetSource = targetSource;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			Object oldProxy = null;
			Object target = this.targetSource.getTarget();
			try {
				oldProxy = AopContext.setCurrentProxy(proxy);
				Object retVal = methodProxy.invoke(target, args);
				return processReturnType(proxy, target, method, retVal);
			}
			finally {
				AopContext.setCurrentProxy(oldProxy);
				this.targetSource.releaseTarget(target);
			}
		}
	}


	/**
	 * 静态目标的调度器. 调度器比拦截器快得多. 只要可以确定方法肯定不返回 "this"，就会使用它.
	 */
	private static class StaticDispatcher implements Dispatcher, Serializable {

		private Object target;

		public StaticDispatcher(Object target) {
			this.target = target;
		}

		@Override
		public Object loadObject() {
			return this.target;
		}
	}


	/**
	 * 在Advised类上声明的任何方法的Dispatcher.
	 */
	private static class AdvisedDispatcher implements Dispatcher, Serializable {

		private final AdvisedSupport advised;

		public AdvisedDispatcher(AdvisedSupport advised) {
			this.advised = advised;
		}

		@Override
		public Object loadObject() throws Exception {
			return this.advised;
		}
	}


	/**
	 * {@code equals}方法的调度器. 确保方法调用始终由此类处理.
	 */
	private static class EqualsInterceptor implements MethodInterceptor, Serializable {

		private final AdvisedSupport advised;

		public EqualsInterceptor(AdvisedSupport advised) {
			this.advised = advised;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
			Object other = args[0];
			if (proxy == other) {
				return true;
			}
			if (other instanceof Factory) {
				Callback callback = ((Factory) other).getCallback(INVOKE_EQUALS);
				if (!(callback instanceof EqualsInterceptor)) {
					return false;
				}
				AdvisedSupport otherAdvised = ((EqualsInterceptor) callback).advised;
				return AopProxyUtils.equalsInProxy(this.advised, otherAdvised);
			}
			else {
				return false;
			}
		}
	}


	/**
	 * {@code hashCode}方法的调度器. 确保方法调用始终由此类处理.
	 */
	private static class HashCodeInterceptor implements MethodInterceptor, Serializable {

		private final AdvisedSupport advised;

		public HashCodeInterceptor(AdvisedSupport advised) {
			this.advised = advised;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
			return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
		}
	}


	/**
	 * 专门用于冻结的静态代理上的增强方法的拦截器.
	 */
	private static class FixedChainStaticTargetInterceptor implements MethodInterceptor, Serializable {

		private final List<Object> adviceChain;

		private final Object target;

		private final Class<?> targetClass;

		public FixedChainStaticTargetInterceptor(List<Object> adviceChain, Object target, Class<?> targetClass) {
			this.adviceChain = adviceChain;
			this.target = target;
			this.targetClass = targetClass;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			MethodInvocation invocation = new CglibMethodInvocation(proxy, this.target, method, args,
					this.targetClass, this.adviceChain, methodProxy);
			// 如果到了这里, 需要创建一个 MethodInvocation.
			Object retVal = invocation.proceed();
			retVal = processReturnType(proxy, this.target, method, retVal);
			return retVal;
		}
	}


	/**
	 * 通用AOP回调. 在目标是动态的或代理未冻结时使用.
	 */
	private static class DynamicAdvisedInterceptor implements MethodInterceptor, Serializable {

		private final AdvisedSupport advised;

		public DynamicAdvisedInterceptor(AdvisedSupport advised) {
			this.advised = advised;
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			Object oldProxy = null;
			boolean setProxyContext = false;
			Class<?> targetClass = null;
			Object target = null;
			try {
				if (this.advised.exposeProxy) {
					// 如有必要，让调用可用.
					oldProxy = AopContext.setCurrentProxy(proxy);
					setProxyContext = true;
				}
				// May be null. 尽可能缩短“拥有”目标的时间, 如果它来自池...
				target = getTarget();
				if (target != null) {
					targetClass = target.getClass();
				}
				List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
				Object retVal;
				// 检查是否只有一个InvokerInterceptor: 即, 没有真正的增强, 但只是反射性地调用目标.
				if (chain.isEmpty() && Modifier.isPublic(method.getModifiers())) {
					// 可以跳过创建一个MethodInvocation: 只是直接调用目标.
					// 请注意, 最终调用者必须是InvokerInterceptor, 所以知道它只对目标进行反射操作, 没有热交换或花哨的代理.
					Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
					retVal = methodProxy.invoke(target, argsToUse);
				}
				else {
					// 需要创建一个方法调用...
					retVal = new CglibMethodInvocation(proxy, target, method, args, targetClass, chain, methodProxy).proceed();
				}
				retVal = processReturnType(proxy, target, method, retVal);
				return retVal;
			}
			finally {
				if (target != null) {
					releaseTarget(target);
				}
				if (setProxyContext) {
					// 恢复旧代理.
					AopContext.setCurrentProxy(oldProxy);
				}
			}
		}

		@Override
		public boolean equals(Object other) {
			return (this == other ||
					(other instanceof DynamicAdvisedInterceptor &&
							this.advised.equals(((DynamicAdvisedInterceptor) other).advised)));
		}

		/**
		 * CGLIB使用它来驱动代理创建.
		 */
		@Override
		public int hashCode() {
			return this.advised.hashCode();
		}

		protected Object getTarget() throws Exception {
			return this.advised.getTargetSource().getTarget();
		}

		protected void releaseTarget(Object target) throws Exception {
			this.advised.getTargetSource().releaseTarget(target);
		}
	}


	/**
	 * 此AOP代理使用的AOP Alliance MethodInvocation的实现.
	 */
	private static class CglibMethodInvocation extends ReflectiveMethodInvocation {

		private final MethodProxy methodProxy;

		private final boolean publicMethod;

		public CglibMethodInvocation(Object proxy, Object target, Method method, Object[] arguments,
				Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers, MethodProxy methodProxy) {

			super(proxy, target, method, arguments, targetClass, interceptorsAndDynamicMethodMatchers);
			this.methodProxy = methodProxy;
			this.publicMethod = Modifier.isPublic(method.getModifiers());
		}

		/**
		 * 与调用公共方法时使用反射调用目标相比，性能略有提高.
		 */
		@Override
		protected Object invokeJoinpoint() throws Throwable {
			if (this.publicMethod) {
				return this.methodProxy.invoke(this.target, this.arguments);
			}
			else {
				return super.invokeJoinpoint();
			}
		}
	}


	/**
	 * CallbackFilter将Callbacks分配给方法.
	 */
	private static class ProxyCallbackFilter implements CallbackFilter {

		private final AdvisedSupport advised;

		private final Map<String, Integer> fixedInterceptorMap;

		private final int fixedInterceptorOffset;

		public ProxyCallbackFilter(AdvisedSupport advised, Map<String, Integer> fixedInterceptorMap, int fixedInterceptorOffset) {
			this.advised = advised;
			this.fixedInterceptorMap = fixedInterceptorMap;
			this.fixedInterceptorOffset = fixedInterceptorOffset;
		}

		/**
		 * 返回我们需要的回调索引的CallbackFilter.accept()实现.
		 * <p>每个代理的回调都是由一组用于一般用途的固定回调构成的，然后是一组特定于具有固定增强链的静态目标上使用的方法的回调.
		 * <p>因此确定使用的回调:
		 * <dl>
		 * <dt>对于公开的代理</dt>
		 * <dd>公开代理需要在方法/链调用之前和之后执行代码.
		 * 这意味着必须使用DynamicAdvisedInterceptor, 因为所有其他拦截器都可以避免使用 try/catch块</dd>
		 * <dt>对于 Object.finalize():</dt>
		 * <dd>不需要覆盖此方法.</dd>
		 * <dt>对于 equals():</dt>
		 * <dd>EqualsInterceptor用于将equals()调用重定向到此代理的特殊处理程序.</dd>
		 * <dt>对于Advised类中的方法:</dt>
		 * <dd>AdvisedDispatcher用于将调用直接分派给目标</dd>
		 * <dt>对于增强的方法:</dt>
		 * <dd>如果目标是静态的, 并且增强链被冻结, 那么特定于该方法的FixedChainStaticTargetInterceptor用于调用建议链.
		 * 否则使用DyanmicAdvisedInterceptor.</dd>
		 * <dt>对于未增强的方法:</dt>
		 * <dd>在哪里可以确定该方法不会返回{@code this}或者{@code ProxyFactory.getExposeProxy()}返回{@code false},
		 * 那么将使用Dispatcher. 对于静态目标, 将使用StaticDispatcher; 对于动态目标, 将使用DynamicUnadvisedInterceptor.
		 * 如果方法返回{@code this}, 那么StaticUnadvisedInterceptor将用于静态目标 - DynamicUnadvisedInterceptor已经考虑过这一点.</dd>
		 * </dl>
		 */
		@Override
		public int accept(Method method) {
			if (AopUtils.isFinalizeMethod(method)) {
				logger.debug("Found finalize() method - using NO_OVERRIDE");
				return NO_OVERRIDE;
			}
			if (!this.advised.isOpaque() && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Method is declared on Advised interface: " + method);
				}
				return DISPATCH_ADVISED;
			}
			// 必须始终代理 equals, 直接调用 this.
			if (AopUtils.isEqualsMethod(method)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Found 'equals' method: " + method);
				}
				return INVOKE_EQUALS;
			}
			// 必须始终根据代理计算hashCode.
			if (AopUtils.isHashCodeMethod(method)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Found 'hashCode' method: " + method);
				}
				return INVOKE_HASHCODE;
			}
			Class<?> targetClass = this.advised.getTargetClass();
			// 代理尚不可用, 但这不重要.
			List<?> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
			boolean haveAdvice = !chain.isEmpty();
			boolean exposeProxy = this.advised.isExposeProxy();
			boolean isStatic = this.advised.getTargetSource().isStatic();
			boolean isFrozen = this.advised.isFrozen();
			if (haveAdvice || !isFrozen) {
				// 如果公开代理, 那么必须使用AOP_PROXY.
				if (exposeProxy) {
					if (logger.isDebugEnabled()) {
						logger.debug("Must expose proxy on advised method: " + method);
					}
					return AOP_PROXY;
				}
				String key = method.toString();
				// 检查我们是否有固定拦截器来处理此方法.
				// 否则使用 AOP_PROXY.
				if (isStatic && isFrozen && this.fixedInterceptorMap.containsKey(key)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Method has advice and optimizations are enabled: " + method);
					}
					// 知道我们正在优化，所以我们可以使用FixedStaticChainInterceptors.
					int index = this.fixedInterceptorMap.get(key);
					return (index + this.fixedInterceptorOffset);
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Unable to apply any optimizations to advised method: " + method);
					}
					return AOP_PROXY;
				}
			}
			else {
				// 查看方法的返回类型是否在目标类型的类层次结构之外.
				// 如果是这样，我们知道它永远不需要返回类型消息，可以使用调度器.
				// 如果代理被公开, 那么必须使用已经配置正确的拦截器.
				// 如果目标不是静态的, 那么我们不能使用调度器, 因为目标需要在调用后显式释放.
				if (exposeProxy || !isStatic) {
					return INVOKE_TARGET;
				}
				Class<?> returnType = method.getReturnType();
				if (returnType.isAssignableFrom(targetClass)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Method return type is assignable from target type and " +
								"may therefore return 'this' - using INVOKE_TARGET: " + method);
					}
					return INVOKE_TARGET;
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Method return type ensures 'this' cannot be returned - " +
								"using DISPATCH_TARGET: " + method);
					}
					return DISPATCH_TARGET;
				}
			}
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ProxyCallbackFilter)) {
				return false;
			}
			ProxyCallbackFilter otherCallbackFilter = (ProxyCallbackFilter) other;
			AdvisedSupport otherAdvised = otherCallbackFilter.advised;
			if (this.advised == null || otherAdvised == null) {
				return false;
			}
			if (this.advised.isFrozen() != otherAdvised.isFrozen()) {
				return false;
			}
			if (this.advised.isExposeProxy() != otherAdvised.isExposeProxy()) {
				return false;
			}
			if (this.advised.getTargetSource().isStatic() != otherAdvised.getTargetSource().isStatic()) {
				return false;
			}
			if (!AopProxyUtils.equalsProxiedInterfaces(this.advised, otherAdvised)) {
				return false;
			}
			// 增强实例标识对代理类不重要: 重要的是类型和排序.
			Advisor[] thisAdvisors = this.advised.getAdvisors();
			Advisor[] thatAdvisors = otherAdvised.getAdvisors();
			if (thisAdvisors.length != thatAdvisors.length) {
				return false;
			}
			for (int i = 0; i < thisAdvisors.length; i++) {
				Advisor thisAdvisor = thisAdvisors[i];
				Advisor thatAdvisor = thatAdvisors[i];
				if (!equalsAdviceClasses(thisAdvisor, thatAdvisor)) {
					return false;
				}
				if (!equalsPointcuts(thisAdvisor, thatAdvisor)) {
					return false;
				}
			}
			return true;
		}

		private boolean equalsAdviceClasses(Advisor a, Advisor b) {
			Advice aa = a.getAdvice();
			Advice ba = b.getAdvice();
			if (aa == null || ba == null) {
				return (aa == ba);
			}
			return (aa.getClass() == ba.getClass());
		}

		private boolean equalsPointcuts(Advisor a, Advisor b) {
			// 如果切面其中之一 (不是两个都是) 是 PointcutAdvisor, 那是不匹配的.
			// 处理使用IntroductionAdvisor的情况 (see SPR-3959).
			return (!(a instanceof PointcutAdvisor) ||
					(b instanceof PointcutAdvisor &&
							ObjectUtils.nullSafeEquals(((PointcutAdvisor) a).getPointcut(), ((PointcutAdvisor) b).getPointcut())));
		}

		@Override
		public int hashCode() {
			int hashCode = 0;
			Advisor[] advisors = this.advised.getAdvisors();
			for (Advisor advisor : advisors) {
				Advice advice = advisor.getAdvice();
				if (advice != null) {
					hashCode = 13 * hashCode + advice.getClass().hashCode();
				}
			}
			hashCode = 13 * hashCode + (this.advised.isFrozen() ? 1 : 0);
			hashCode = 13 * hashCode + (this.advised.isExposeProxy() ? 1 : 0);
			hashCode = 13 * hashCode + (this.advised.isOptimize() ? 1 : 0);
			hashCode = 13 * hashCode + (this.advised.isOpaque() ? 1 : 0);
			return hashCode;
		}
	}


	/**
	 * CGLIB GeneratorStrategy变体, 它将应用程序ClassLoader公开为类生成时的线程上下文ClassLoader
	 * (为了让ASM在执行常见的超类解析时获取它).
	 */
	private static class ClassLoaderAwareUndeclaredThrowableStrategy extends UndeclaredThrowableStrategy {

		private final ClassLoader classLoader;

		public ClassLoaderAwareUndeclaredThrowableStrategy(ClassLoader classLoader) {
			super(UndeclaredThrowableException.class);
			this.classLoader = classLoader;
		}

		@Override
		public byte[] generate(ClassGenerator cg) throws Exception {
			if (this.classLoader == null) {
				return super.generate(cg);
			}

			Thread currentThread = Thread.currentThread();
			ClassLoader threadContextClassLoader;
			try {
				threadContextClassLoader = currentThread.getContextClassLoader();
			}
			catch (Throwable ex) {
				// 无法访问线程上下文ClassLoader - 回退...
				return super.generate(cg);
			}

			boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
			if (overrideClassLoader) {
				currentThread.setContextClassLoader(this.classLoader);
			}
			try {
				return super.generate(cg);
			}
			finally {
				if (overrideClassLoader) {
					// 重置原始线程上下文ClassLoader.
					currentThread.setContextClassLoader(threadContextClassLoader);
				}
			}
		}
	}
}
