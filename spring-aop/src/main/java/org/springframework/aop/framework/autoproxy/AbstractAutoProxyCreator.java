package org.springframework.aop.framework.autoproxy;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现,
 * 用AOP代理包装每个符合条件的bean, 在调用bean本身之前委托给指定的拦截器.
 *
 * <p>这个类区分“常见”拦截器: 共享它创建的所有代理, 和“特定”拦截器: 每个bean实例都唯一. 不需要任何常见的拦截器.
 * 如果有, 它们是使用interceptorNames属性设置的.
 * 和{@link org.springframework.aop.framework.ProxyFactoryBean}一样, 使用当前工厂中的拦截器名称而不是bean引用,
 * 来允许正确处理原型切面和拦截器: 例如, 支持有状态的混合.
 * {@link #setInterceptorNames "interceptorNames"}条目支持任何增强类型.
 *
 * <p>如果需要使用类似代理包装大量bean, 则自动代理特别有用, i.e.委托给同一个拦截器.
 * 而不是x目标bean的x重复代理定义, 你可以在bean工厂注册一个这样的后处理器来达到同样的效果.
 *
 * <p>子类可以应用任何策略来决定是否要代理bean, e.g. 通过类型, 通过名称, 通过定义细节, etc.
 * 它们还可以返回应该只应用于特定bean实例的其他拦截器.
 * 一个简单的具体实现是 {@link BeanNameAutoProxyCreator}, 通过给定的名称识别要代理的bean.
 *
 * <p>可以使用任意数量的{@link TargetSourceCreator}实现来创建自定义目标源: 例如, 池原型对象.
 * 即使没有增强，也会发生自动代理, 只要TargetSourceCreator指定自定义{@link org.springframework.aop.TargetSource}.
 * 如果没有设置TargetSourceCreators, 或如果没有匹配, 默认将使用{@link org.springframework.aop.target.SingletonTargetSource},
 * 包装目标bean 实例.
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * 子类的便捷常量: “不代理”的返回值.
	 */
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * 子类的便捷常量: “没有额外拦截器的代理，只是常见的拦截器”的返回值.
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Default is global AdvisorAdapterRegistry */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * 指示是否应冻结代理. 从超类重写以防止配置过早冻结.
	 */
	private boolean freezeProxy = false;

	/** 默认没有常见的拦截器 */
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	private TargetSourceCreator[] customTargetSourceCreators;

	private BeanFactory beanFactory;

	private final Set<String> targetSourcedBeans =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

	private final Set<Object> earlyProxyReferences =
			Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>(16));

	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<Object, Class<?>>(16);

	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<Object, Boolean>(256);


	/**
	 * 设置是否应冻结代理, 防止在创建增强后添加增强.
	 * <p>从超类重写以防止在创建代理之前冻结代理配置.
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * 指定要使用的{@link AdvisorAdapterRegistry}.
	 * <p>默认是全局 {@link AdvisorAdapterRegistry}.
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * 设置要按此顺序应用的自定义{@code TargetSourceCreators}.
	 * 如果列表为空, 或者它们都返回null, 将为每个bean创建一个{@link SingletonTargetSource}.
	 * <p>请注意，即使对于没有找到增强或切面的目标bean，TargetSourceCreators也会启动.
	 * 如果{@code TargetSourceCreator}为特定bean返回{@link TargetSource}, 在任何情况下, 该bean都将被代理.
	 * <p>只有在{@link BeanFactory}中使用此后处理器且触发其{@link BeanFactoryAware}回调时，才能调用{@code TargetSourceCreators}.
	 * 
	 * @param targetSourceCreators {@code TargetSourceCreators}列表.
	 * 顺序很重要: 将使用从第一个匹配的{@code TargetSourceCreator}返回的{@code TargetSource}（即，返回非null的第一个）.
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * 设置普通拦截器. 这些必须是当前工厂中的bean名称.
	 * 它们可以是Spring支持的任何增强或切面类型.
	 * <p>如果未设置此属性, 则没有普通拦截器. 这完全有效, 如果匹配Advisor的“特定”拦截器都是我们想要的.
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * 设置是否应在bean特定的拦截器之前应用普通拦截器.
	 * 默认是 "true"; 否则，将首先应用特定于bean的拦截器.
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回拥有的 {@link BeanFactory}.
	 * 可能是 {@code null}, 因为这个后处理器不需要属于bean工厂.
	 */
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		if (!this.earlyProxyReferences.contains(cacheKey)) {
			this.earlyProxyReferences.add(cacheKey);
		}
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (beanName == null || !this.targetSourcedBeans.contains(beanName)) {
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// 如果有自定义TargetSource，请在此处创建代理.
		// 禁止目标bean的不必要的默认实例化: TargetSource将以自定义方式处理目标实例.
		if (beanName != null) {
			TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
			if (targetSource != null) {
				this.targetSourcedBeans.add(beanName);
				Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
				Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
				this.proxyTypes.put(cacheKey, proxy.getClass());
				return proxy;
			}
		}

		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
		return true;
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return pvs;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * 如果bean被标识为子类代理的bean，则使用配置的拦截器创建代理.
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (!this.earlyProxyReferences.contains(cacheKey)) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * 为给定的bean类和bean名称构建缓存键.
	 * <p>Note: 截至4.2.3, 此实现不再返回连接的类/名称字符串，而是返回最有效的缓存键:
	 * 一个普通的bean 名称, 如果是{@code FactoryBean}, 请加上{@link BeanFactory#FACTORY_BEAN_PREFIX};
	 * 或者如果没有指定bean名称, 那么给定的bean {@code Class}就是这样.
	 * 
	 * @param beanClass bean类
	 * @param beanName bean名称
	 * 
	 * @return 给定类和名称的缓存键
	 */
	protected Object getCacheKey(Class<?> beanClass, String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		}
		else {
			return beanClass;
		}
	}

	/**
	 * 如有必要, 包装给定的bean, i.e. 如果它有资格被代理.
	 * 
	 * @param bean 原始bean实例
	 * @param beanName bean名称
	 * @param cacheKey 用于元数据访问的缓存键
	 * 
	 * @return 封装了bean的代理, 或原始bean实例
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// 如果有增强，则创建代理.
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * 返回给定的bean类是否表示永远不应该代理的基础结构类.
	 * <p>默认实现将Advice, Advisor和AopInfrastructureBean视为基础结构类.
	 * 
	 * @param beanClass bean类
	 * 
	 * @return bean是否代表基础结构类
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * 如果此后处理器不应考虑给定bean进行自动代理，则子类应重写此方法以返回{@code true}.
	 * <p>有时需要避免这种情况发生，如果它会导致循环引用. 此实现返回 {@code false}.
	 * 
	 * @param beanClass bean类
	 * @param beanName bean名称
	 * 
	 * @return 是否跳过给定的bean
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return false;
	}

	/**
	 * 为bean实例创建目标源. 如果设置, 则使用任何TargetSourceCreator.
	 * 如果不应使用自定义TargetSource, 则返回{@code null}.
	 * <p>此实现使用"customTargetSourceCreators"属性. 子类可以重写此方法以使用不同的机制.
	 * 
	 * @param beanClass 用于创建TargetSource的bean的类
	 * @param beanName bean的名称
	 * 
	 * @return 此bean的TargetSource
	 */
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// 无法为直接注册的单例创建奇特的目标来源.
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isDebugEnabled()) {
						logger.debug("TargetSourceCreator [" + tsc +
								" found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		return null;
	}

	/**
	 * 为给定的bean创建AOP代理.
	 * 
	 * @param beanClass bean类
	 * @param beanName bean名称
	 * @param specificInterceptors 特定于此bean的拦截器集 (可能为空, 但不会是 null)
	 * @param targetSource 代理的TargetSource, 已预先配置以便访问bean
	 * 
	 * @return bean的AOP代理
	 */
	protected Object createProxy(
			Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);

		if (!proxyFactory.isProxyTargetClass()) {
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}

		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		proxyFactory.addAdvisors(advisors);
		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		return proxyFactory.getProxy(getProxyClassLoader());
	}

	/**
	 * 确定给定的bean是否应该使用其目标类而不是其接口进行代理.
	 * <p>检查对应bean定义的 {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}.
	 * 
	 * @param beanClass bean的类
	 * @param beanName bean的名称
	 * 
	 * @return 是否应该使用其目标类代理给定的bean
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * 子类返回的Advisor是否已经预过滤, 以匹配bean的目标类,在为AOP调用构建切面链时, 允许跳过ClassFilter检查.
	 * <p>默认是 {@code false}. 如果子类始终返回预过滤的Advisor, 则子类可以覆盖它.
	 * 
	 * @return 切面是否经过预先筛选
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * 确定适用于Advisor接口的给定bean的切面, 包括特定的拦截器以及常见的拦截器.
	 * 
	 * @param beanName bean名称
	 * @param specificInterceptors 特定于此bean的拦截器集 (可能是空的, 但不会是 null)
	 * 
	 * @return 给定bean的切面列表
	 */
	protected Advisor[] buildAdvisors(String beanName, Object[] specificInterceptors) {
		// 正确处理原型...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<Object>();
		if (specificInterceptors != null) {
			allInterceptors.addAll(Arrays.asList(specificInterceptors));
			if (commonInterceptors.length > 0) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				}
				else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isDebugEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.debug("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * 将指定的拦截器名称解析为Advisor对象.
	 */
	private Advisor[] resolveInterceptorNames() {
		ConfigurableBeanFactory cbf = (this.beanFactory instanceof ConfigurableBeanFactory ?
				(ConfigurableBeanFactory) this.beanFactory : null);
		List<Advisor> advisors = new ArrayList<Advisor>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Object next = this.beanFactory.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[advisors.size()]);
	}

	/**
	 * 子类可以选择实现: 例如, 更改公开的接口.
	 * <p>默认实现为空.
	 * 
	 * @param proxyFactory 已经使用TargetSource和接口配置的ProxyFactory将在此方法返回后立即用于创建代理
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * 返回代理给定的bean, 要应用的额外增强（例如AOP联盟拦截器）和切面.
	 * 
	 * @param beanClass 要增强的bean的类
	 * @param beanName bean的名称
	 * @param customTargetSource 由{@link #getCustomTargetSource}方法返回的TargetSource: 可以被忽略.
	 * 如果没有使用自定义目标源, 则为{@code null}.
	 * 
	 * @return 特定bean的一系列额外拦截器; 或者如果没有额外的拦截器，则为空数组，而只是常见的拦截器;
	 * 或{@code null}如果没有代理, 甚至没有常见的拦截器.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * 
	 * @throws BeansException 发生错误
	 */
	protected abstract Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException;

}
