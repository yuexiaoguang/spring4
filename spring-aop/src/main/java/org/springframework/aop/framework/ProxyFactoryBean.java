package org.springframework.aop.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean}实现,
 * 在Spring {@link org.springframework.beans.factory.BeanFactory}中构建一个基于bean的AOP代理.
 *
 * <p>{@link org.aopalliance.intercept.MethodInterceptor MethodInterceptors}和
 * {@link org.springframework.aop.Advisor Advisors} 由当前bean工厂中的bean名称列表标识, 通过 "interceptorNames"属性指定.
 * 列表中的最后一个条目可以是目标bean的名称, 也可以是{@link org.springframework.aop.TargetSource};
 * 但是, 通常最好使用 "targetName"/"target"/"targetSource"属性替代.
 *
 * <p>可以在工厂级别添加全局拦截器和切面. 指定的那些在拦截器列表中展开, 其中“xxx *”条目包含在列表中,
 * 将给定前缀与bean名称匹配 (e.g. "global*" 将同时匹配 "globalBean1" 和 "globalBean2", "*" 匹配所有已定义的拦截器).
 * 匹配的拦截器根据其返回的顺序值进行应用, 如果它们实现了 {@link org.springframework.core.Ordered}接口.
 *
 * <p>在给出代理接口时创建JDK代理, 如果没有, 则为实际目标类的CGLIB代理. 
 * 请注意，后者只有在目标类没有final方法时才有效, 因为动态子类将在运行时创建.
 *
 * <p>可以将从该工厂获得的代理转换为{@link Advised}, 或获取ProxyFactoryBean引用并以编程方式操作它.
 * 这对于独立的现有原型引用不起作用. 但是, 它适用于随后从工厂获得的原型. 拦截的变化将立即对单例起作用 (包括现有引用).
 * 但是,更改接口或目标是从工厂获取新实例的必要条件. 这意味着从工厂获得的单例实例不具有相同的对象标识.
 * 但是, 他们确实有相同的拦截器和目标, 而且更改任何引用将更改所有对象.
 */
@SuppressWarnings("serial")
public class ProxyFactoryBean extends ProxyCreatorSupport
		implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

	/**
	 * 拦截器列表中的值后缀表示扩展全局变量.
	 */
	public static final String GLOBAL_SUFFIX = "*";


	protected final Log logger = LogFactory.getLog(getClass());

	private String[] interceptorNames;

	private String targetName;

	private boolean autodetectInterfaces = true;

	private boolean singleton = true;

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	private boolean freezeProxy = false;

	private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private transient boolean classLoaderConfigured = false;

	private transient BeanFactory beanFactory;

	/** 切面链是否已初始化 */
	private boolean advisorChainInitialized = false;

	/** 如果这是一个单例, 缓存的单例代理实例 */
	private Object singletonInstance;


	/**
	 * 设置代理的接口的名称. 如果没有给出接口, 将创建实际类的CGLIB.
	 * <p>这基本上等同于 "setInterfaces"方法, 但不同于镜像TransactionProxyFactoryBean的"setProxyInterfaces".
	 */
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) throws ClassNotFoundException {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * 设置Advice/Advisor bean名称列表. 必须始终将其设置为在Bean工厂中使用此工厂bean.
	 * <p>引用的bean应该是Interceptor, Advisor, Advice类型. 列表中的最后一个条目可以是工厂中任何bean的名称.
	 * 如果它既不是增强也不是切面, 添加一个新的SingletonTargetSource来包装它.
	 * 如果设置了"target" 或 "targetSource" 或 "targetName"属性, 则无法使用此类目标bean, 在这种情况下,
	 * "interceptorNames" 数组必须只包含Advice/Advisor bean名称.
	 * <p><b>NOTE: 不推荐在"interceptorNames" 列表中指定目标bean作为最终名称, 并将在以后的Spring版本中删除.</b>
	 * 使用{@link #setTargetName "targetName"}属性替代.
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * 设置目标bean的名称.
	 * 这是在"interceptorNames"数组末尾指定目标名称的替代方法.
	 * <p>还可以直接指定目标对象或TargetSource对象, 分别通过"target"/"targetSource"属性.
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * 设置是否在未指定的情况下自动检测代理接口.
	 * <p>默认是 "true". 如果未指定接口, 请关闭此标志, 以为完整目标类创建CGLIB代理.
	 */
	public void setAutodetectInterfaces(boolean autodetectInterfaces) {
		this.autodetectInterfaces = autodetectInterfaces;
	}

	/**
	 * 设置singleton属性的值.
	 * 管理此工厂是否应始终返回相同的代理实例 (这意味着同一个目标), 或是否应该返回一个新的原型实例,
	 * 这意味着目标和拦截器也可能是新的实例, 如果它们是从原型bean定义中获得的.
	 * 这允许精确控制对象图中的独立性/唯一性.
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * 指定要使用的AdvisorAdapterRegistry.
	 * 默认是全局 AdvisorAdapterRegistry.
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	/**
	 * 设置ClassLoader以生成代理类.
	 * <p>默认是 bean ClassLoader, i.e. 包含BeanFactory用于加载所有bean类的ClassLoader.
	 * 对于特定代理，可以在此处覆盖此内容.
	 */
	public void setProxyClassLoader(ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		checkInterceptorNames();
	}


	/**
	 * 返回代理. 客户端从此工厂bean获取bean时调用.
	 * 创建此工厂要返回的AOP代理的实例. 实例将被缓存为单例, 并在每次调用{@code getObject()}时为代理创建.
	 * 
	 * @return 反映这个工厂的当前状态的新的AOP代理，
	 */
	@Override
	public Object getObject() throws BeansException {
		initializeAdvisorChain();
		if (isSingleton()) {
			return getSingletonInstance();
		}
		else {
			if (this.targetName == null) {
				logger.warn("Using non-singleton proxies with singleton targets is often undesirable. " +
						"Enable prototype proxies by setting the 'targetName' property.");
			}
			return newPrototypeInstance();
		}
	}

	/**
	 * 返回代理的类型.
	 * 如果已经创建, 将检查单例实例, 否则回退到代理接口 (如果只有一个), 目标bean类型, 或TargetSource的目标类.
	 */
	@Override
	public Class<?> getObjectType() {
		synchronized (this) {
			if (this.singletonInstance != null) {
				return this.singletonInstance.getClass();
			}
		}
		Class<?>[] ifcs = getProxiedInterfaces();
		if (ifcs.length == 1) {
			return ifcs[0];
		}
		else if (ifcs.length > 1) {
			return createCompositeInterface(ifcs);
		}
		else if (this.targetName != null && this.beanFactory != null) {
			return this.beanFactory.getType(this.targetName);
		}
		else {
			return getTargetClass();
		}
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}


	/**
	 * 为给定接口创建复合接口类, 在一个单独的类中实现给定的接口.
	 * <p>默认实现为给定接口构建JDK代理类.
	 * 
	 * @param interfaces 要合并的接口
	 * 
	 * @return 合并后的接口
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
	}

	/**
	 * 返回此类的代理对象的单例实例, 如果尚未创建它, 则延迟创建它.
	 * 
	 * @return 共享的单例代理
	 */
	private synchronized Object getSingletonInstance() {
		if (this.singletonInstance == null) {
			this.targetSource = freshTargetSource();
			if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
				// 依靠AOP架构来告诉我们代理的接口.
				Class<?> targetClass = getTargetClass();
				if (targetClass == null) {
					throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
				}
				setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
			// 初始化共享单例实例.
			super.setFrozen(this.freezeProxy);
			this.singletonInstance = getProxy(createAopProxy());
		}
		return this.singletonInstance;
	}

	/**
	 * 创建此类创建的代理对象的新原型实例, 由独立的AdvisedSupport配置支持.
	 * 
	 * @return 一个完全独立的代理，我们可以孤立地操纵他们的增强
	 */
	private synchronized Object newPrototypeInstance() {
		// 在原型的情况下, 需要为代理提供一个独立的配置实例.
		// 在这种情况下, 没有代理将拥有此对象配置的实例, 但会有一份独立的副本.
		if (logger.isTraceEnabled()) {
			logger.trace("Creating copy of prototype ProxyFactoryBean config: " + this);
		}

		ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());
		// 该副本需要一个新的切面链, 和一个新的 TargetSource.
		TargetSource targetSource = freshTargetSource();
		copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
		if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
			// 依靠AOP架构告诉我们代理的接口.
			copy.setInterfaces(
					ClassUtils.getAllInterfacesForClass(targetSource.getTargetClass(), this.proxyClassLoader));
		}
		copy.setFrozen(this.freezeProxy);

		if (logger.isTraceEnabled()) {
			logger.trace("Using ProxyCreatorSupport copy: " + copy);
		}
		return getProxy(copy.createAopProxy());
	}

	/**
	 * 返回要公开的代理对象.
	 * <p>默认实现使用{@code getProxy}调用工厂的bean类加载器. 可以重写以指定自定义类加载器.
	 * 
	 * @param aopProxy 从中获取代理的AopProxy实例
	 * 
	 * @return 要公开的代理对象
	 */
	protected Object getProxy(AopProxy aopProxy) {
		return aopProxy.getProxy(this.proxyClassLoader);
	}

	/**
	 * 检查interceptorNames列表是否包含目标名称作为最终元素.
	 * 如果找到, 从列表中删除最终名称并将其设置为targetName.
	 */
	private void checkInterceptorNames() {
		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			String finalName = this.interceptorNames[this.interceptorNames.length - 1];
			if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				// 链中的最后一个名称可能是 Advisor/Advice 或 target/TargetSource.
				// 不幸的是我们不知道; 必须看看bean的类型.
				if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
					// 目标不是拦截器.
					this.targetName = finalName;
					if (logger.isDebugEnabled()) {
						logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " +
								"is not an advisor class: treating it as a target or TargetSource");
					}
					String[] newNames = new String[this.interceptorNames.length - 1];
					System.arraycopy(this.interceptorNames, 0, newNames, 0, newNames.length);
					this.interceptorNames = newNames;
				}
			}
		}
	}

	/**
	 * 查看bean工厂元数据以确定此bean名称是否正确, 截取interceptorNames列表, 是一个 Advisor 或 Advice, 或可能是一个目标.
	 * 
	 * @param beanName 要检查的bean名称
	 * 
	 * @return {@code true} 如果是一个 Advisor 或 Advice
	 */
	private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
		Class<?> namedBeanClass = this.beanFactory.getType(beanName);
		if (namedBeanClass != null) {
			return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
		}
		// 如果我们不知道，将它视为目标bean.
		if (logger.isDebugEnabled()) {
			logger.debug("Could not determine type of bean with name '" + beanName +
					"' - assuming it is neither an Advisor nor an Advice");
		}
		return false;
	}

	/**
	 * 创建切面（拦截器）链.
	 * 每次添加新的原型实例时，将刷新源自BeanFactory的切面. 通过工厂API以编程方式添加的拦截器不受此类更改的影响.
	 */
	private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
		if (this.advisorChainInitialized) {
			return;
		}

		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve interceptor names " + Arrays.asList(this.interceptorNames));
			}

			// 除非我们使用属性指定了targetSource，否则Globals不能是最后的...
			if (this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX) &&
					this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				throw new AopConfigException("Target required after globals");
			}

			// 从bean名称中物化拦截器链.
			for (String name : this.interceptorNames) {
				if (logger.isTraceEnabled()) {
					logger.trace("Configuring advisor or advice '" + name + "'");
				}

				if (name.endsWith(GLOBAL_SUFFIX)) {
					if (!(this.beanFactory instanceof ListableBeanFactory)) {
						throw new AopConfigException(
								"Can only use global advisors or interceptors with a ListableBeanFactory");
					}
					addGlobalAdvisor((ListableBeanFactory) this.beanFactory,
							name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
				}

				else {
					// 如果到达了这里, 需要添加一个命名拦截器.
					// 必须检查它是单例还是原型.
					Object advice;
					if (this.singleton || this.beanFactory.isSingleton(name)) {
						// 添加真正的 Advisor/Advice到链.
						advice = this.beanFactory.getBean(name);
					}
					else {
						// 这是一个原型 Advice 或 Advisor: 替换原型.
						// 避免不必要的创建原型bean, 只是为了切面链初始化.
						advice = new PrototypePlaceholderAdvisor(name);
					}
					addAdvisorOnChainCreation(advice, name);
				}
			}
		}

		this.advisorChainInitialized = true;
	}


	/**
	 * 返回一个独立的切面链.
	 * 每次返回一个新的原型实例时, 都需要这样做, 返回原型Advisor和Advice的不同实例.
	 */
	private List<Advisor> freshAdvisorChain() {
		Advisor[] advisors = getAdvisors();
		List<Advisor> freshAdvisors = new ArrayList<Advisor>(advisors.length);
		for (Advisor advisor : advisors) {
			if (advisor instanceof PrototypePlaceholderAdvisor) {
				PrototypePlaceholderAdvisor pa = (PrototypePlaceholderAdvisor) advisor;
				if (logger.isDebugEnabled()) {
					logger.debug("Refreshing bean named '" + pa.getBeanName() + "'");
				}
				// 使用getBean()找到的新原型实例替换占位符
				if (this.beanFactory == null) {
					throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
							"- cannot resolve prototype advisor '" + pa.getBeanName() + "'");
				}
				Object bean = this.beanFactory.getBean(pa.getBeanName());
				Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
				freshAdvisors.add(refreshedAdvisor);
			}
			else {
				// 添加共享实例.
				freshAdvisors.add(advisor);
			}
		}
		return freshAdvisors;
	}

	/**
	 * 添加所有全局拦截器和切点.
	 */
	private void addGlobalAdvisor(ListableBeanFactory beanFactory, String prefix) {
		String[] globalAdvisorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
		String[] globalInterceptorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
		List<Object> beans = new ArrayList<Object>(globalAdvisorNames.length + globalInterceptorNames.length);
		Map<Object, String> names = new HashMap<Object, String>(beans.size());
		for (String name : globalAdvisorNames) {
			Object bean = beanFactory.getBean(name);
			beans.add(bean);
			names.put(bean, name);
		}
		for (String name : globalInterceptorNames) {
			Object bean = beanFactory.getBean(name);
			beans.add(bean);
			names.put(bean, name);
		}
		AnnotationAwareOrderComparator.sort(beans);
		for (Object bean : beans) {
			String name = names.get(bean);
			if (name.startsWith(prefix)) {
				addAdvisorOnChainCreation(bean, name);
			}
		}
	}

	/**
	 * 在创建增强链时调用.
	 * <p>将给定的增强，切面或对象添加到拦截器列表中.
	 * 因为这三种可能性, 不能确定类型.
	 * 
	 * @param next 增强，切面或目标对象
	 * @param name 从拥有的bean工厂中中获取此对象的bean名称
	 */
	private void addAdvisorOnChainCreation(Object next, String name) {
		// 如有必要，需要转换为Advisor，以便源引用与从超类拦截器中找到的引用相匹配.
		Advisor advisor = namedBeanToAdvisor(next);
		if (logger.isTraceEnabled()) {
			logger.trace("Adding advisor with name '" + name + "'");
		}
		addAdvisor(advisor);
	}

	/**
	 * 返回在创建代理时使用的TargetSource.
	 * 如果未在interceptorNames列表的末尾指定目标, TargetSource将是这个类的TargetSource成员.
	 * 否则, 如果需要, 我们获取目标bean并将其包装在TargetSource中.
	 */
	private TargetSource freshTargetSource() {
		if (this.targetName == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Not refreshing target: Bean name not specified in 'interceptorNames'.");
			}
			return this.targetSource;
		}
		else {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve target with name '" + this.targetName + "'");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Refreshing target with name '" + this.targetName + "'");
			}
			Object target = this.beanFactory.getBean(this.targetName);
			return (target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target));
		}
	}

	/**
	 * 将以下来自interceptorNames数组的名称上调用getBean()获得的对象转换为Advisor或TargetSource.
	 */
	private Advisor namedBeanToAdvisor(Object next) {
		try {
			return this.advisorAdapterRegistry.wrap(next);
		}
		catch (UnknownAdviceTypeException ex) {
			// 希望这是一个 Advisor 或 Advice, 但它不是. 这是配置错误.
			throw new AopConfigException("Unknown advisor type " + next.getClass() +
					"; Can only include Advisor or Advice type beans in interceptorNames chain except for last entry," +
					"which may also be target or TargetSource", ex);
		}
	}

	/**
	 * 在增强变更时吹走并重新安排单例.
	 */
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		if (this.singleton) {
			logger.debug("Advice has changed; recaching singleton instance");
			synchronized (this) {
				this.singletonInstance = null;
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依靠默认序列化; 只是在反序列化后初始化状态.
		ois.defaultReadObject();

		// 初始化transient 字段.
		this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
	}


	/**
	 * 在拦截器链中使用，需要在创建代理时用原型替换bean.
	 */
	private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

		private final String beanName;

		private final String message;

		public PrototypePlaceholderAdvisor(String beanName) {
			this.beanName = beanName;
			this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
		}

		public String getBeanName() {
			return this.beanName;
		}

		@Override
		public Advice getAdvice() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public boolean isPerInstance() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public String toString() {
			return this.message;
		}
	}
}
