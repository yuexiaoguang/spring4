package org.springframework.context.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.asm.Type;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.DefaultGeneratorStrategy;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.cglib.transform.ClassEmitterTransformer;
import org.springframework.cglib.transform.TransformingClassGenerator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 通过生成一个CGLIB子类来增强{@link Configuration}类, 该子类与Spring容器交互以尊重{@code @Bean}方法的bean作用域语义.
 * 每个这样的{@code @Bean}方法都将在生成的子类中重写, 如果容器实际请求构造新实例, 才委托给实际的{@code @Bean}方法实现.
 * 否则, 调用这样的{@code @Bean}方法作为对容器的引用, 通过名称获取相应的bean.
 */
class ConfigurationClassEnhancer {

	// 要使用的回调. 请注意, 这些回调必须是无状态的.
	private static final Callback[] CALLBACKS = new Callback[] {
			new BeanMethodInterceptor(),
			new BeanFactoryAwareMethodInterceptor(),
			NoOp.INSTANCE
	};

	private static final ConditionalCallbackFilter CALLBACK_FILTER = new ConditionalCallbackFilter(CALLBACKS);

	private static final String BEAN_FACTORY_FIELD = "$$beanFactory";


	private static final Log logger = LogFactory.getLog(ConfigurationClassEnhancer.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();


	/**
	 * 加载指定的类并生成一个CGLIB子类, 它配备了容器感知回调, 能够尊重作用域和其他bean语义.
	 * 
	 * @return 增强的子类
	 */
	public Class<?> enhance(Class<?> configClass, ClassLoader classLoader) {
		if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Ignoring request to enhance %s as it has " +
						"already been enhanced. This usually indicates that more than one " +
						"ConfigurationClassPostProcessor has been registered (e.g. via " +
						"<context:annotation-config>). This is harmless, but you may " +
						"want check your configuration and remove one CCPP if possible",
						configClass.getName()));
			}
			return configClass;
		}
		Class<?> enhancedClass = createClass(newEnhancer(configClass, classLoader));
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Successfully enhanced %s; enhanced class name is: %s",
					configClass.getName(), enhancedClass.getName()));
		}
		return enhancedClass;
	}

	/**
	 * 创建一个新的CGLIB {@link Enhancer}实例.
	 */
	private Enhancer newEnhancer(Class<?> configSuperClass, ClassLoader classLoader) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(configSuperClass);
		enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
		enhancer.setUseFactory(false);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
		enhancer.setCallbackFilter(CALLBACK_FILTER);
		enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
		return enhancer;
	}

	/**
	 * 使用增强器生成超类的子类, 确保为新子类注册回调.
	 */
	private Class<?> createClass(Enhancer enhancer) {
		Class<?> subclass = enhancer.createClass();
		// 静态注册回调 (而不是线程本地) 对于OSGi环境中的使用至关重要 (SPR-5932)...
		Enhancer.registerStaticCallbacks(subclass, CALLBACKS);
		return subclass;
	}


	/**
	 * 标记接口由所有@Configuration CGLIB子类实现.
	 * 通过检查候选类是否已分配给它, 来促进{@link ConfigurationClassEnhancer#enhance}的幂等行为, e.g. 已经得到了加强.
	 * <p>还扩展了{@link BeanFactoryAware}, 因为所有增强的 {@code @Configuration}类都需要访问创建它们的{@link BeanFactory}.
	 * <p>请注意, 此接口仅供框架内部使用, 但必须保持public才能允许访问从其他包生成的子类 (i.e. 用户代码).
	 */
	public interface EnhancedConfiguration extends BeanFactoryAware {
	}


	/**
	 * 条件 {@link Callback}.
	 */
	private interface ConditionalCallback extends Callback {

		boolean isMatch(Method candidateMethod);
	}


	/**
	 * 通过查询{@link Callback}, 来按照通过{@link ConditionalCallback}定义它们的顺序工作.
	 */
	private static class ConditionalCallbackFilter implements CallbackFilter {

		private final Callback[] callbacks;

		private final Class<?>[] callbackTypes;

		public ConditionalCallbackFilter(Callback[] callbacks) {
			this.callbacks = callbacks;
			this.callbackTypes = new Class<?>[callbacks.length];
			for (int i = 0; i < callbacks.length; i++) {
				this.callbackTypes[i] = callbacks[i].getClass();
			}
		}

		@Override
		public int accept(Method method) {
			for (int i = 0; i < this.callbacks.length; i++) {
				Callback callback = this.callbacks[i];
				if (!(callback instanceof ConditionalCallback) || ((ConditionalCallback) callback).isMatch(method)) {
					return i;
				}
			}
			throw new IllegalStateException("No callback available for method " + method.getName());
		}

		public Class<?>[] getCallbackTypes() {
			return this.callbackTypes;
		}
	}


	/**
	 * 自定义扩展CGLIB的 DefaultGeneratorStrategy, 引入了一个{@link BeanFactory}字段.
	 * 还将应用程序ClassLoader公开为类生成时的线程上下文ClassLoader (为了让ASM在执行常见的超类解析时获取它).
	 */
	private static class BeanFactoryAwareGeneratorStrategy extends DefaultGeneratorStrategy {

		private final ClassLoader classLoader;

		public BeanFactoryAwareGeneratorStrategy(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		protected ClassGenerator transform(ClassGenerator cg) throws Exception {
			ClassEmitterTransformer transformer = new ClassEmitterTransformer() {
				@Override
				public void end_class() {
					declare_field(Constants.ACC_PUBLIC, BEAN_FACTORY_FIELD, Type.getType(BeanFactory.class), null);
					super.end_class();
				}
			};
			return new TransformingClassGenerator(cg, transformer);
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
				// Cannot access thread context ClassLoader - falling back...
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


	/**
	 * 拦截{@code @Configuration}类实例上的{@link BeanFactoryAware#setBeanFactory(BeanFactory)},
	 * 以便记录{@link BeanFactory}.
	 */
	private static class BeanFactoryAwareMethodInterceptor implements MethodInterceptor, ConditionalCallback {

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);
			Assert.state(field != null, "Unable to find generated BeanFactory field");
			field.set(obj, args[0]);

			// 实际的 (非CGLIB) 超类是否实现了BeanFactoryAware?
			// 如果是这样, 请调用其 setBeanFactory()方法. 如果不是, 结束.
			if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {
				return proxy.invokeSuper(obj, args);
			}
			return null;
		}

		@Override
		public boolean isMatch(Method candidateMethod) {
			return (candidateMethod.getName().equals("setBeanFactory") &&
					candidateMethod.getParameterTypes().length == 1 &&
					BeanFactory.class == candidateMethod.getParameterTypes()[0] &&
					BeanFactoryAware.class.isAssignableFrom(candidateMethod.getDeclaringClass()));
		}
	}


	/**
	 * 拦截带{@link Bean}注解的方法的调用, 以确保正确处理bean语义, 例如作用域和AOP代理.
	 */
	private static class BeanMethodInterceptor implements MethodInterceptor, ConditionalCallback {

		/**
		 * 增强{@link Bean @Bean}方法以检查提供的BeanFactory是否存在此bean对象.
		 * 
		 * @throws Throwable 作为在调用代理方法的超级实现时, 可能抛出的异常的全部捕获 i.e., 实际的{@code @Bean}方法
		 */
		@Override
		public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
					MethodProxy cglibMethodProxy) throws Throwable {

			ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
			String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);

			// 确定此bean是否为范围代理
			Scope scope = AnnotatedElementUtils.findMergedAnnotation(beanMethod, Scope.class);
			if (scope != null && scope.proxyMode() != ScopedProxyMode.NO) {
				String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
				if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {
					beanName = scopedBeanName;
				}
			}

			// 要处理bean间方法引用的情况, 必须显式检查已缓存实例的容器.

			// 首先, 检查请求的bean是否是FactoryBean.
			// 如果是这样, 创建一个子类代理, 拦截对 getObject() 的调用, 并返回缓存的bean实例.
			// 这确保了从@Bean方法中调用FactoryBean的语义, 与在XML中引用FactoryBean的语义相同. See SPR-6602.
			if (factoryContainsBean(beanFactory, BeanFactory.FACTORY_BEAN_PREFIX + beanName) &&
					factoryContainsBean(beanFactory, beanName)) {
				Object factoryBean = beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
				if (factoryBean instanceof ScopedProxyFactoryBean) {
					// 范围内的代理工厂bean是一种特殊情况, 不应进一步代理
				}
				else {
					// 它是候选FactoryBean - 继续进行增强
					return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);
				}
			}

			if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
				// 工厂正在调用bean方法以实例化和注册bean
				// (i.e. 通过调用 getBean()) -> 调用方法的超级实现来实际创建bean实例.
				if (logger.isWarnEnabled() &&
						BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
					logger.warn(String.format("@Bean method %s.%s is non-static and returns an object " +
									"assignable to Spring's BeanFactoryPostProcessor interface. This will " +
									"result in a failure to process annotations such as @Autowired, " +
									"@Resource and @PostConstruct within the method's declaring " +
									"@Configuration class. Add the 'static' modifier to this method to avoid " +
									"these container lifecycle issues; see @Bean javadoc for complete details.",
							beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));
				}
				return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
			}

			return obtainBeanInstanceFromFactory(beanMethod, beanMethodArgs, beanFactory, beanName);
		}

		private Object obtainBeanInstanceFromFactory(Method beanMethod, Object[] beanMethodArgs,
				ConfigurableBeanFactory beanFactory, String beanName) {

			// 用户 (i.e. 非工厂) 通过直接或间接调用bean方法来请求此bean.
			// 在某些自动装配场景中, bean可能已被标记为 '在创建中';
			// 如果是, 则暂时将创建中状态设置为false以避免异常.
			boolean alreadyInCreation = beanFactory.isCurrentlyInCreation(beanName);
			try {
				if (alreadyInCreation) {
					beanFactory.setCurrentlyInCreation(beanName, false);
				}
				boolean useArgs = !ObjectUtils.isEmpty(beanMethodArgs);
				if (useArgs && beanFactory.isSingleton(beanName)) {
					// 被引用的空参数仅供参考, 期望它们可以自动连接到常规单例引用?
					// 一个安全的假设, 因为@Bean单例参数不能是可选的...
					for (Object arg : beanMethodArgs) {
						if (arg == null) {
							useArgs = false;
							break;
						}
					}
				}
				Object beanInstance = (useArgs ? beanFactory.getBean(beanName, beanMethodArgs) :
						beanFactory.getBean(beanName));
				if (beanInstance != null && !ClassUtils.isAssignableValue(beanMethod.getReturnType(), beanInstance)) {
					String msg = String.format("@Bean method %s.%s called as a bean reference " +
								"for type [%s] but overridden by non-compatible bean instance of type [%s].",
								beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
								beanMethod.getReturnType().getName(), beanInstance.getClass().getName());
					try {
						BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
						msg += " Overriding bean of same name declared in: " + beanDefinition.getResourceDescription();
					}
					catch (NoSuchBeanDefinitionException ex) {
						// Ignore - 根本没有详细的消息.
					}
					throw new IllegalStateException(msg);
				}
				Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
				if (currentlyInvoked != null) {
					String outerBeanName = BeanAnnotationHelper.determineBeanNameFor(currentlyInvoked);
					beanFactory.registerDependentBean(beanName, outerBeanName);
				}
				return beanInstance;
			}
			finally {
				if (alreadyInCreation) {
					beanFactory.setCurrentlyInCreation(beanName, true);
				}
			}
		}

		@Override
		public boolean isMatch(Method candidateMethod) {
			return (candidateMethod.getDeclaringClass() != Object.class &&
					BeanAnnotationHelper.isBeanAnnotated(candidateMethod));
		}

		private ConfigurableBeanFactory getBeanFactory(Object enhancedConfigInstance) {
			Field field = ReflectionUtils.findField(enhancedConfigInstance.getClass(), BEAN_FACTORY_FIELD);
			Assert.state(field != null, "Unable to find generated bean factory field");
			Object beanFactory = ReflectionUtils.getField(field, enhancedConfigInstance);
			Assert.state(beanFactory != null, "BeanFactory has not been injected into @Configuration class");
			Assert.state(beanFactory instanceof ConfigurableBeanFactory,
					"Injected BeanFactory is not a ConfigurableBeanFactory");
			return (ConfigurableBeanFactory) beanFactory;
		}

		/**
		 * 检查BeanFactory以查看名为 <var>beanName</var>的bean是否已存在.
		 * 说明所请求的bean可能 "正在创建"的事实, i.e.: 正在为这个bean的初始请求提供服务.
		 * 从增强的工厂方法的角度来看, 这意味着bean实际上并不存在,
		 * 现在我们的工作是通过在相应的工厂方法中执行逻辑来首次创建它.
		 * <p>换句话说, 此检查重新调整 {@link ConfigurableBeanFactory#isCurrentlyInCreation(String)}
		 * 以确定容器是否正在调用此方法或用户是否正在调用此方法.
		 * 
		 * @param beanName 要检查的bean的名称
		 * 
		 * @return 工厂中是否已存在<var>beanName</var>
		 */
		private boolean factoryContainsBean(ConfigurableBeanFactory beanFactory, String beanName) {
			return (beanFactory.containsBean(beanName) && !beanFactory.isCurrentlyInCreation(beanName));
		}

		/**
		 * 检查给定方法是否对应于容器当前调用的工厂方法.
		 * 仅比较方法名称和参数类型, 以便解决协变返回类型的潜在问题
		 * (目前只知道在Groovy类上发生).
		 */
		private boolean isCurrentlyInvokedFactoryMethod(Method method) {
			Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
			return (currentlyInvoked != null && method.getName().equals(currentlyInvoked.getName()) &&
					Arrays.equals(method.getParameterTypes(), currentlyInvoked.getParameterTypes()));
		}

		/**
		 * 创建一个子类代理, 拦截对 getObject()的调用, 委托给当前的BeanFactory而不是创建一个新的实例.
		 * 这些代理仅在从Bean方法中调用FactoryBean时创建, 即使在直接对FactoryBean实例进行操作时也允许正确的作用域语义.
		 * 如果通过 &-dereferencing 通过容器获取FactoryBean实例, 则不会代理它. 这也与XML配置的工作方式一致.
		 */
		private Object enhanceFactoryBean(final Object factoryBean, Class<?> exposedType,
				final ConfigurableBeanFactory beanFactory, final String beanName) {

			try {
				Class<?> clazz = factoryBean.getClass();
				boolean finalClass = Modifier.isFinal(clazz.getModifiers());
				boolean finalMethod = Modifier.isFinal(clazz.getMethod("getObject").getModifiers());
				if (finalClass || finalMethod) {
					if (exposedType.isInterface()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Creating interface proxy for FactoryBean '" + beanName + "' of type [" +
									clazz.getName() + "] for use within another @Bean method because its " +
									(finalClass ? "implementation class" : "getObject() method") +
									" is final: Otherwise a getObject() call would not be routed to the factory.");
						}
						return createInterfaceProxyForFactoryBean(factoryBean, exposedType, beanFactory, beanName);
					}
					else {
						if (logger.isInfoEnabled()) {
							logger.info("Unable to proxy FactoryBean '" + beanName + "' of type [" +
									clazz.getName() + "] for use within another @Bean method because its " +
									(finalClass ? "implementation class" : "getObject() method") +
									" is final: A getObject() call will NOT be routed to the factory. " +
									"Consider declaring the return type as a FactoryBean interface.");
						}
						return factoryBean;
					}
				}
			}
			catch (NoSuchMethodException ex) {
				// No getObject() method -> 不应该发生, 但只要没有人试图调用它...
			}

			return createCglibProxyForFactoryBean(factoryBean, beanFactory, beanName);
		}

		private Object createInterfaceProxyForFactoryBean(final Object factoryBean, Class<?> interfaceType,
				final ConfigurableBeanFactory beanFactory, final String beanName) {

			return Proxy.newProxyInstance(
					factoryBean.getClass().getClassLoader(), new Class<?>[] {interfaceType},
					new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if (method.getName().equals("getObject") && args == null) {
								return beanFactory.getBean(beanName);
							}
							return ReflectionUtils.invokeMethod(method, factoryBean, args);
						}
					});
		}

		private Object createCglibProxyForFactoryBean(final Object factoryBean,
				final ConfigurableBeanFactory beanFactory, final String beanName) {

			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(factoryBean.getClass());
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			enhancer.setCallbackType(MethodInterceptor.class);

			// 理想情况下创建增强的FactoryBean代理, 而没有构造函数副作用, 类似于ObjenesisCglibAopProxy中的AOP代理创建...
			Class<?> fbClass = enhancer.createClass();
			Object fbProxy = null;

			if (objenesis.isWorthTrying()) {
				try {
					fbProxy = objenesis.newInstance(fbClass, enhancer.getUseCache());
				}
				catch (ObjenesisException ex) {
					logger.debug("Unable to instantiate enhanced FactoryBean using Objenesis, " +
							"falling back to regular construction", ex);
				}
			}

			if (fbProxy == null) {
				try {
					fbProxy = fbClass.newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Unable to instantiate enhanced FactoryBean using Objenesis, " +
							"and regular FactoryBean instantiation via default constructor fails as well", ex);
				}
			}

			((Factory) fbProxy).setCallback(0, new MethodInterceptor() {
				@Override
				public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
					if (method.getName().equals("getObject") && args.length == 0) {
						return beanFactory.getBean(beanName);
					}
					return proxy.invoke(factoryBean, args);
				}
			});

			return fbProxy;
		}
	}

}
