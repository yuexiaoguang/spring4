package org.springframework.jmx.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Constants;
import org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;
import org.springframework.jmx.export.naming.KeyNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.export.notification.ModelMBeanNotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.MBeanRegistrationSupport;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 允许将任何<i>Spring-managed bean</i>公开给JMX {@link javax.management.MBeanServer}的JMX导出器,
 * 而无需在bean类中定义任何特定于JMX的信息.
 *
 * <p>如果bean实现了一个JMX管理接口, MBeanExporter可以通过其自动检测过程简单地向服务器注册MBean.
 *
 * <p>如果bean没有实现其中一个JMX管理接口, MBeanExporter将使用提供的{@link MBeanInfoAssembler}创建管理信息.
 *
 * <p>可以通过 {@link #setListeners(MBeanExporterListener[]) listeners}属性注册{@link MBeanExporterListener MBeanExporterListeners}列表,
 * 允许通知应用程序代码MBean注册和注销事件.
 *
 * <p>此导出器与MBean和MXBeans兼容.
 */
public class MBeanExporter extends MBeanRegistrationSupport implements MBeanExportOperations,
		BeanClassLoaderAware, BeanFactoryAware, InitializingBean, SmartInitializingSingleton, DisposableBean {

	/**
	 * 自动检测模式, 指示不应使用自动检测.
	 */
	public static final int AUTODETECT_NONE = 0;

	/**
	 * 自动检测模式, 指示只应自动检测有效的MBean.
	 */
	public static final int AUTODETECT_MBEAN = 1;

	/**
	 * 自动检测模式, 指示只有{@link MBeanInfoAssembler}应该能够自动检测bean.
	 */
	public static final int AUTODETECT_ASSEMBLER = 2;

	/**
	 * 自动检测模式, 指示应使用所有自动检测机制.
	 */
	public static final int AUTODETECT_ALL = AUTODETECT_MBEAN | AUTODETECT_ASSEMBLER;


	/**
	 * 用于将{@link javax.management.NotificationListener}映射到{@code MBeanExporter}注册的所有MBean的通配符.
	 */
	private static final String WILDCARD = "*";

	/** JMX的常量 {@code mr_type} "ObjectReference" */
	private static final String MR_TYPE_OBJECT_REFERENCE = "ObjectReference";

	/** 此类中定义的自动检测常量的前缀 */
	private static final String CONSTANT_PREFIX_AUTODETECT = "AUTODETECT_";


	/** 此类的常量实例 */
	private static final Constants constants = new Constants(MBeanExporter.class);

	/** 要作为JMX管理资源公开的bean, 以JMX名称作为键 */
	private Map<String, Object> beans;

	/** 用于此MBeanExporter的自动检测模式 */
	private Integer autodetectMode;

	/** 是否在自动检测MBean时, 实时地初始化候选bean */
	private boolean allowEagerInit = false;

	/** 存储用于此导出器的MBeanInfoAssembler */
	private MBeanInfoAssembler assembler = new SimpleReflectiveMBeanInfoAssembler();

	/** 用于为对象创建ObjectName的策略 */
	private ObjectNamingStrategy namingStrategy = new KeyNamingStrategy();

	/** 指示Spring是否应该修改生成的ObjectNames */
	private boolean ensureUniqueRuntimeObjectNames = true;

	/** 指示Spring是否应在MBean中公开管理的资源ClassLoader */
	private boolean exposeManagedResourceClassLoader = true;

	/** 应从自动检测中排除的一组bean名称 */
	private Set<String> excludedBeans = new HashSet<String>();

	/** 向此导出器注册的MBeanExporterListener. */
	private MBeanExporterListener[] listeners;

	/** 用于此导出器注册的MBean的NotificationListener */
	private NotificationListenerBean[] notificationListeners;

	/** 实际注册的NotificationListener */
	private final Map<NotificationListenerBean, ObjectName[]> registeredNotificationListeners =
			new LinkedHashMap<NotificationListenerBean, ObjectName[]>();

	/** 存储用于生成延迟初始化的代理的ClassLoader */
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** 存储用于自动检测过程的BeanFactory */
	private ListableBeanFactory beanFactory;


	/**
	 * 提供要在JMX {@code MBeanServer}注册的bean的{@code Map}.
	 * <p>String键是创建JMX对象名称的基础.
	 * 默认情况下, 将直接从给定的键创建JMX {@code ObjectName}.
	 * 这可以通过指定自定义{@code NamingStrategy}来自定义.
	 * <p>允许bean实例和bean名称作为值. Bean实例通常通过bean引用链接.
	 * Bean名称将在当前工厂中被解析为bean, 遵守lazy-init标记 (即, 不触发此类bean的初始化).
	 * 
	 * @param beans 将JMX名称作为键, 将bean实例或bean名称作为值
	 */
	public void setBeans(Map<String, Object> beans) {
		this.beans = beans;
	}

	/**
	 * 设置是否在此导出器运行的bean工厂中自动检测MBean.
	 * 如果可用, 还会询问 {@code AutodetectCapableMBeanInfoAssembler}.
	 * <p>默认情况下, 此功能处于关闭状态. 在此处明确指定{@code true}以启用自动检测.
	 */
	public void setAutodetect(boolean autodetect) {
		this.autodetectMode = (autodetect ? AUTODETECT_ALL : AUTODETECT_NONE);
	}

	/**
	 * 设置要使用的自动检测模式.
	 * 
	 * @throws IllegalArgumentException 如果提供的值不是{@code AUTODETECT_}常量之一
	 */
	public void setAutodetectMode(int autodetectMode) {
		if (!constants.getValues(CONSTANT_PREFIX_AUTODETECT).contains(autodetectMode)) {
			throw new IllegalArgumentException("Only values of autodetect constants allowed");
		}
		this.autodetectMode = autodetectMode;
	}

	/**
	 * 设置按名称使用的自动检测模式.
	 * 
	 * @throws IllegalArgumentException 如果提供的值无法解析为{@code AUTODETECT_}常量之一或{@code null}
	 */
	public void setAutodetectModeName(String constantName) {
		if (constantName == null || !constantName.startsWith(CONSTANT_PREFIX_AUTODETECT)) {
			throw new IllegalArgumentException("Only autodetect constants allowed");
		}
		this.autodetectMode = (Integer) constants.asNumber(constantName);
	}

	/**
	 * 指定在Spring应用程序上下文中自动检测MBean时, 是否允许候选bean的实时初始化.
	 * <p>默认 "false", 遵守bean定义上的lazy-init标志.
	 * 将其切换为 "true" 以便搜索lazy-init bean, 包括尚未初始化的FactoryBean生成的对象.
	 */
	public void setAllowEagerInit(boolean allowEagerInit) {
		this.allowEagerInit = allowEagerInit;
	}

	/**
	 * 设置用于此导出器的{@code MBeanInfoAssembler}接口的实现.
	 * 默认是 {@code SimpleReflectiveMBeanInfoAssembler}.
	 * <p>传入的Assembler可以选择实现{@code AutodetectCapableMBeanInfoAssembler}接口, 这使它能够参与导出器的MBean自动检测过程.
	 */
	public void setAssembler(MBeanInfoAssembler assembler) {
		this.assembler = assembler;
	}

	/**
	 * 设置用于此导出器的{@code ObjectNamingStrategy}接口的实现.
	 * 默认是 {@code KeyNamingStrategy}.
	 */
	public void setNamingStrategy(ObjectNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * 指示Spring是否应确保为运行时注册的MBean ({@link #registerManagedResource}) 配置的{@link ObjectNamingStrategy}
	 * 生成的{@link ObjectName ObjectNames}应该被修改:
	 * 确保管理的{@code Class}的每个实例的唯一性.
	 * <p>默认 {@code true}.
	 */
	public void setEnsureUniqueRuntimeObjectNames(boolean ensureUniqueRuntimeObjectNames) {
		this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
	}

	/**
	 * 指示在允许对MBean进行调用之前, 是否应在{@link Thread#getContextClassLoader() 线程上下文ClassLoader}上公开托管资源.
	 * <p>默认 {@code true}, 公开{@link SpringModelMBean}, 它执行线程上下文ClassLoader管理.
	 * 关闭此标志以公开标准JMX {@link javax.management.modelmbean.RequiredModelMBean}.
	 */
	public void setExposeManagedResourceClassLoader(boolean exposeManagedResourceClassLoader) {
		this.exposeManagedResourceClassLoader = exposeManagedResourceClassLoader;
	}

	/**
	 * 设置应从自动检测中排除的bean的名称列表.
	 */
	public void setExcludedBeans(String... excludedBeans) {
		this.excludedBeans.clear();
		if (excludedBeans != null) {
			this.excludedBeans.addAll(Arrays.asList(excludedBeans));
		}
	}

	/**
	 * 添加应从自动检测中排除的bean的名称.
	 */
	public void addExcludedBean(String excludedBean) {
		Assert.notNull(excludedBean, "ExcludedBean must not be null");
		this.excludedBeans.add(excludedBean);
	}

	/**
	 * 设置应该通知其MBean注册和取消注册事件的{@code MBeanExporterListener}.
	 */
	public void setListeners(MBeanExporterListener... listeners) {
		this.listeners = listeners;
	}

	/**
	 * 设置{@link NotificationListenerBean NotificationListenerBeans},
	 * 其中包含将在{@link MBeanServer}中注册的{@link javax.management.NotificationListener NotificationListeners}.
	 */
	public void setNotificationListeners(NotificationListenerBean... notificationListeners) {
		this.notificationListeners = notificationListeners;
	}

	/**
	 * 设置在{@link javax.management.MBeanServer}上注册的{@link NotificationListener NotificationListeners}.
	 * <P>{@code Map}中每个条目的Key, 是{@link javax.management.ObjectName}的{@link String}格式, 或者是应该注册的MBean的bean名称.
	 * 为Key指定星号 ({@code *}) 将使监听器在启动时与此类注册的所有MBean关联.
	 * <p>每个条目的值是要注册的 {@link javax.management.NotificationListener}.
	 * 有关更高级的选项, 例如注册{@link javax.management.NotificationFilter NotificationFilters}和回传对象,
	 * 请参阅{@link #setNotificationListeners(NotificationListenerBean[])}.
	 */
	public void setNotificationListenerMappings(Map<?, ? extends NotificationListener> listeners) {
		Assert.notNull(listeners, "'listeners' must not be null");
		List<NotificationListenerBean> notificationListeners =
				new ArrayList<NotificationListenerBean>(listeners.size());

		for (Map.Entry<?, ? extends NotificationListener> entry : listeners.entrySet()) {
			// 从Map值中获取监听器.
			NotificationListenerBean bean = new NotificationListenerBean(entry.getValue());
			// 从Key获取ObjectName.
			Object key = entry.getKey();
			if (key != null && !WILDCARD.equals(key)) {
				// 此监听器映射到特定的ObjectName.
				bean.setMappedObjectName(entry.getKey());
			}
			notificationListeners.add(bean);
		}

		this.notificationListeners =
				notificationListeners.toArray(new NotificationListenerBean[notificationListeners.size()]);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 只有在{@link #setBeans(java.util.Map) "beans"} {@link Map}中解析bean名称以及MBeans的自动检测时, 才需要此回调
	 * (在后一种情况下, 需要{@code ListableBeanFactory}).
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
		else {
			logger.info("MBeanExporter not running in a ListableBeanFactory: autodetection of MBeans not available.");
		}
	}


	//---------------------------------------------------------------------
	// Lifecycle in bean factory: automatically register/unregister beans
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() {
		// 如果没有提供服务器, 那么尝试找一个. 这在已加载的MBeanServer的环境中很有用.
		if (this.server == null) {
			this.server = JmxUtils.locateMBeanServer();
		}
	}

	/**
	 * 在常规单例实例化阶段之后, 自动启动bean注册.
	 */
	@Override
	public void afterSingletonsInstantiated() {
		try {
			logger.info("Registering beans for JMX exposure on startup");
			registerBeans();
			registerNotificationListeners();
		}
		catch (RuntimeException ex) {
			// 注销此导出器已注册的Bean.
			unregisterNotificationListeners();
			unregisterBeans();
			throw ex;
		}
	}

	/**
	 * 当封闭的{@code ApplicationContext}被销毁时, 注销此导出已通过JMX公开的所有bean.
	 */
	@Override
	public void destroy() {
		logger.info("Unregistering JMX-exposed beans on shutdown");
		unregisterNotificationListeners();
		unregisterBeans();
	}


	//---------------------------------------------------------------------
	// Implementation of MBeanExportOperations interface
	//---------------------------------------------------------------------

	@Override
	public ObjectName registerManagedResource(Object managedResource) throws MBeanExportException {
		Assert.notNull(managedResource, "Managed resource must not be null");
		ObjectName objectName;
		try {
			objectName = getObjectName(managedResource, null);
			if (this.ensureUniqueRuntimeObjectNames) {
				objectName = JmxUtils.appendIdentityToObjectName(objectName, managedResource);
			}
		}
		catch (Throwable ex) {
			throw new MBeanExportException("Unable to generate ObjectName for MBean [" + managedResource + "]", ex);
		}
		registerManagedResource(managedResource, objectName);
		return objectName;
	}

	@Override
	public void registerManagedResource(Object managedResource, ObjectName objectName) throws MBeanExportException {
		Assert.notNull(managedResource, "Managed resource must not be null");
		Assert.notNull(objectName, "ObjectName must not be null");
		try {
			if (isMBean(managedResource.getClass())) {
				doRegister(managedResource, objectName);
			}
			else {
				ModelMBean mbean = createAndConfigureMBean(managedResource, managedResource.getClass().getName());
				doRegister(mbean, objectName);
				injectNotificationPublisherIfNecessary(managedResource, mbean, objectName);
			}
		}
		catch (JMException ex) {
			throw new UnableToRegisterMBeanException(
					"Unable to register MBean [" + managedResource + "] with object name [" + objectName + "]", ex);
		}
	}

	@Override
	public void unregisterManagedResource(ObjectName objectName) {
		Assert.notNull(objectName, "ObjectName must not be null");
		doUnregister(objectName);
	}


	//---------------------------------------------------------------------
	// Exporter implementation
	//---------------------------------------------------------------------

	/**
	 * 使用{@link MBeanServer}注册已定义的bean.
	 * <p>每个bean都通过{@code ModelMBean}暴露给{@code MBeanServer}.
	 * 使用的{@code ModelMBean}接口的实际实现, 取决于配置的{@code ModelMBeanProvider}接口的实现.
	 * 默认情况下, 使用所有JMX实现提供的{@code RequiredModelMBean}类.
	 * <p>为每个bean生成的管理接口取决于所使用的{@code MBeanInfoAssembler}实现.
	 * 给每个bean的{@code ObjectName}取决于所使用的{@code ObjectNamingStrategy}接口的实现.
	 */
	protected void registerBeans() {
		// beans属性可以为 null, 例如, 如果仅依赖于自动检测.
		if (this.beans == null) {
			this.beans = new HashMap<String, Object>();
			// 在没有显式指定的bean中使用AUTODETECT_ALL作为默认值.
			if (this.autodetectMode == null) {
				this.autodetectMode = AUTODETECT_ALL;
			}
		}

		// 如果需要, 执行自动检测.
		int mode = (this.autodetectMode != null ? this.autodetectMode : AUTODETECT_NONE);
		if (mode != AUTODETECT_NONE) {
			if (this.beanFactory == null) {
				throw new MBeanExportException("Cannot autodetect MBeans if not running in a BeanFactory");
			}
			if (mode == AUTODETECT_MBEAN || mode == AUTODETECT_ALL) {
				// 自动检测已经是MBean的bean.
				logger.debug("Autodetecting user-defined JMX MBeans");
				autodetectMBeans();
			}
			// 允许assembler有机会投票支持bean.
			if ((mode == AUTODETECT_ASSEMBLER || mode == AUTODETECT_ALL) &&
					this.assembler instanceof AutodetectCapableMBeanInfoAssembler) {
				autodetectBeans((AutodetectCapableMBeanInfoAssembler) this.assembler);
			}
		}

		if (!this.beans.isEmpty()) {
			for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
				registerBeanNameOrInstance(entry.getValue(), entry.getKey());
			}
		}
	}

	/**
	 * 返回是否应将指定的bean定义视为lazy-init.
	 * 
	 * @param beanFactory 应该包含bean定义的bean工厂
	 * @param beanName 要检查的bean的名称
	 */
	protected boolean isBeanDefinitionLazyInit(ListableBeanFactory beanFactory, String beanName) {
		return (beanFactory instanceof ConfigurableListableBeanFactory && beanFactory.containsBeanDefinition(beanName) &&
				((ConfigurableListableBeanFactory) beanFactory).getBeanDefinition(beanName).isLazyInit());
	}

	/**
	 * 使用{@link #setServer MBeanServer}注册单个bean.
	 * <p>此方法负责决定如何将bean暴露给 {@code MBeanServer}.
	 * 具体来说, 如果提供的{@code mapValue}是为延迟初始化配置的bean的名称,
	 * 然后, 使用{@code MBeanServer}注册资源的代理, 以便遵守延迟加载行为.
	 * 如果bean已经是MBean, 那么它将直接在{@code MBeanServer}上注册, 无需任何干预.
	 * 对于所有其他bean或bean名称, 资源本身直接在{@code MBeanServer}中注册.
	 * 
	 * @param mapValue bean映射中为此bean配置的值; 可以是bean的{@code String}名称, 也可以是bean本身
	 * @param beanKey bean映射中与此bean关联的键
	 * 
	 * @return 注册资源的{@code ObjectName}; 如果实际资源是{@code null}, 则为{@code null}
	 * @throws MBeanExportException 如果导出失败
	 */
	protected ObjectName registerBeanNameOrInstance(Object mapValue, String beanKey) throws MBeanExportException {
		try {
			if (mapValue instanceof String) {
				// Bean名称指向工厂中可能的lazy-init bean.
				if (this.beanFactory == null) {
					throw new MBeanExportException("Cannot resolve bean names if not running in a BeanFactory");
				}
				String beanName = (String) mapValue;
				if (isBeanDefinitionLazyInit(this.beanFactory, beanName)) {
					ObjectName objectName = registerLazyInit(beanName, beanKey);
					replaceNotificationListenerBeanNameKeysIfNecessary(beanName, objectName);
					return objectName;
				}
				else {
					Object bean = this.beanFactory.getBean(beanName);
					if (bean != null) {
						ObjectName objectName = registerBeanInstance(bean, beanKey);
						replaceNotificationListenerBeanNameKeysIfNecessary(beanName, objectName);
						return objectName;
					}
				}
			}
			else if (mapValue != null) {
				// 普通bean实例 -> 直接注册它.
				if (this.beanFactory != null) {
					Map<String, ?> beansOfSameType =
							this.beanFactory.getBeansOfType(mapValue.getClass(), false, this.allowEagerInit);
					for (Map.Entry<String, ?> entry : beansOfSameType.entrySet()) {
						if (entry.getValue() == mapValue) {
							String beanName = entry.getKey();
							ObjectName objectName = registerBeanInstance(mapValue, beanKey);
							replaceNotificationListenerBeanNameKeysIfNecessary(beanName, objectName);
							return objectName;
						}
					}
				}
				return registerBeanInstance(mapValue, beanKey);
			}
		}
		catch (Throwable ex) {
			throw new UnableToRegisterMBeanException(
					"Unable to register MBean [" + mapValue + "] with key '" + beanKey + "'", ex);
		}
		return null;
	}

	/**
	 * 将{@code NotificationListener}映射中用作键的bean名称替换为其对应的{@code ObjectName}值.
	 * 
	 * @param beanName 要注册的bean的名称
	 * @param objectName 将使用{@code MBeanServer}注册bean的{@code ObjectName}
	 */
	private void replaceNotificationListenerBeanNameKeysIfNecessary(String beanName, ObjectName objectName) {
		if (this.notificationListeners != null) {
			for (NotificationListenerBean notificationListener : this.notificationListeners) {
				notificationListener.replaceObjectName(beanName, objectName);
			}
		}
	}

	/**
	 * 使用{@code MBeanServer}为普通bean注册现有MBean或MBean适配器.
	 * 
	 * @param bean 要注册的bean, MBean或普通bean
	 * @param beanKey bean映射中与此bean关联的键
	 * 
	 * @return 使用{@code MBeanServer}注册bean的{@code ObjectName}
	 */
	private ObjectName registerBeanInstance(Object bean, String beanKey) throws JMException {
		ObjectName objectName = getObjectName(bean, beanKey);
		Object mbeanToExpose = null;
		if (isMBean(bean.getClass())) {
			mbeanToExpose = bean;
		}
		else {
			DynamicMBean adaptedBean = adaptMBeanIfPossible(bean);
			if (adaptedBean != null) {
				mbeanToExpose = adaptedBean;
			}
		}

		if (mbeanToExpose != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Located MBean '" + beanKey + "': registering with JMX server as MBean [" +
						objectName + "]");
			}
			doRegister(mbeanToExpose, objectName);
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Located managed bean '" + beanKey + "': registering with JMX server as MBean [" +
						objectName + "]");
			}
			ModelMBean mbean = createAndConfigureMBean(bean, beanKey);
			doRegister(mbean, objectName);
			injectNotificationPublisherIfNecessary(bean, mbean, objectName);
		}

		return objectName;
	}

	/**
	 * 通过代理间接注册配置为使用{@code MBeanServer}进行延迟初始化的bean.
	 * 
	 * @param beanName {@code BeanFactory}中bean的名称
	 * @param beanKey bean映射中与此bean关联的键
	 * 
	 * @return 使用{@code MBeanServer}注册bean的{@code ObjectName}
	 */
	private ObjectName registerLazyInit(String beanName, String beanKey) throws JMException {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.setFrozen(true);

		if (isMBean(this.beanFactory.getType(beanName))) {
			// 一个直接的MBean... 为它创建一个简单的lazy-init CGLIB代理.
			LazyInitTargetSource targetSource = new LazyInitTargetSource();
			targetSource.setTargetBeanName(beanName);
			targetSource.setBeanFactory(this.beanFactory);
			proxyFactory.setTargetSource(targetSource);

			Object proxy = proxyFactory.getProxy(this.beanClassLoader);
			ObjectName objectName = getObjectName(proxy, beanKey);
			if (logger.isDebugEnabled()) {
				logger.debug("Located MBean '" + beanKey + "': registering with JMX server as lazy-init MBean [" +
						objectName + "]");
			}
			doRegister(proxy, objectName);
			return objectName;
		}

		else {
			// 一个简单的 bean... 创建一个带有通知支持的lazy-init ModelMBean代理.
			NotificationPublisherAwareLazyTargetSource targetSource = new NotificationPublisherAwareLazyTargetSource();
			targetSource.setTargetBeanName(beanName);
			targetSource.setBeanFactory(this.beanFactory);
			proxyFactory.setTargetSource(targetSource);

			Object proxy = proxyFactory.getProxy(this.beanClassLoader);
			ObjectName objectName = getObjectName(proxy, beanKey);
			if (logger.isDebugEnabled()) {
				logger.debug("Located simple bean '" + beanKey + "': registering with JMX server as lazy-init MBean [" +
						objectName + "]");
			}
			ModelMBean mbean = createAndConfigureMBean(proxy, beanKey);
			targetSource.setModelMBean(mbean);
			targetSource.setObjectName(objectName);
			doRegister(mbean, objectName);
			return objectName;
		}
	}

	/**
	 * 检索bean的{@code ObjectName}.
	 * <p>如果bean实现了{@code SelfNaming} 接口, 那么将使用{@code SelfNaming.getObjectName()}检索{@code ObjectName}.
	 * 否则, 使用配置的{@code ObjectNamingStrategy}.
	 * 
	 * @param bean {@code BeanFactory}中bean的名称
	 * @param beanKey bean映射中与bean关联的键
	 * 
	 * @return 提供的bean的{@code ObjectName}
	 * @throws javax.management.MalformedObjectNameException 如果检索到的{@code ObjectName}格式错误
	 */
	protected ObjectName getObjectName(Object bean, String beanKey) throws MalformedObjectNameException {
		if (bean instanceof SelfNaming) {
			return ((SelfNaming) bean).getObjectName();
		}
		else {
			return this.namingStrategy.getObjectName(bean, beanKey);
		}
	}

	/**
	 * 确定给定的b​​ean类是否可以作为MBean.
	 * <p>默认实现委托给{@link JmxUtils#isMBean},
	 * 它检查{@link javax.management.DynamicMBean}类以及具有相应"*MBean"接口 (标准MBean) 或相应"*MXBean"接口 (Java 6 MXBeans)的类.
	 * 
	 * @param beanClass 要分析的bean类
	 * 
	 * @return 该类是否有资格作为MBean
	 */
	protected boolean isMBean(Class<?> beanClass) {
		return JmxUtils.isMBean(beanClass);
	}

	/**
	 * 为给定的bean实例构建适应的MBean.
	 * <p>对于AOP代理, 默认实现为目标的MBean/MXBean接口构建JMX 1.2 StandardMBean, 将接口的管理操作委托给代理.
	 * 
	 * @param bean 原始的bean实例
	 * 
	 * @return 适应的MBean, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	protected DynamicMBean adaptMBeanIfPossible(Object bean) throws JMException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (targetClass != bean.getClass()) {
			Class<?> ifc = JmxUtils.getMXBeanInterface(targetClass);
			if (ifc != null) {
				if (!ifc.isInstance(bean)) {
					throw new NotCompliantMBeanException("Managed bean [" + bean +
							"] has a target class with an MXBean interface but does not expose it in the proxy");
				}
				return new StandardMBean(bean, ((Class<Object>) ifc), true);
			}
			else {
				ifc = JmxUtils.getMBeanInterface(targetClass);
				if (ifc != null) {
					if (!ifc.isInstance(bean)) {
						throw new NotCompliantMBeanException("Managed bean [" + bean +
								"] has a target class with an MBean interface but does not expose it in the proxy");
					}
					return new StandardMBean(bean, ((Class<Object>) ifc));
				}
			}
		}
		return null;
	}

	/**
	 * 创建一个MBean, 该MBean使用所提供的受管资源的相应管理接口进行配置.
	 * 
	 * @param managedResource 要作为MBean导出的资源
	 * @param beanKey 与托管bean关联的Key
	 */
	protected ModelMBean createAndConfigureMBean(Object managedResource, String beanKey)
			throws MBeanExportException {
		try {
			ModelMBean mbean = createModelMBean();
			mbean.setModelMBeanInfo(getMBeanInfo(managedResource, beanKey));
			mbean.setManagedResource(managedResource, MR_TYPE_OBJECT_REFERENCE);
			return mbean;
		}
		catch (Throwable ex) {
			throw new MBeanExportException("Could not create ModelMBean for managed resource [" +
					managedResource + "] with key '" + beanKey + "'", ex);
		}
	}

	/**
	 * 创建实现了{@code ModelMBean}的类的实例.
	 * <p>调用此方法以获取在注册bean时使用的{@code ModelMBean}实例.
	 * 在注册阶段, 每个bean调用一次此方法, 并且必须返回{@code ModelMBean}的新实例
	 * 
	 * @return 实现{@code ModelMBean}的类的新实例
	 * @throws javax.management.MBeanException 如果ModelMBean的创建失败
	 */
	protected ModelMBean createModelMBean() throws MBeanException {
		return (this.exposeManagedResourceClassLoader ? new SpringModelMBean() : new RequiredModelMBean());
	}

	/**
	 * 使用提供的Key和提供的类型获取bean的{@code ModelMBeanInfo}.
	 */
	private ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException {
		ModelMBeanInfo info = this.assembler.getMBeanInfo(managedBean, beanKey);
		if (logger.isWarnEnabled() && ObjectUtils.isEmpty(info.getAttributes()) &&
				ObjectUtils.isEmpty(info.getOperations())) {
			logger.warn("Bean with key '" + beanKey +
					"' has been registered as an MBean but has no exposed attributes or operations");
		}
		return info;
	}


	//---------------------------------------------------------------------
	// Autodetection process
	//---------------------------------------------------------------------

	/**
	 * 使用{@code AutodetectCapableMBeanInfoAssembler}时调用.
	 * 使assembler有机会将{@code BeanFactory}中的其他bean添加到要通过JMX公开的bean列表中.
	 * <p>如果已经手动添加bean, 则此实现可防止将bean自动添加到列表中, 它会阻止某些内部类自动注册.
	 */
	private void autodetectBeans(final AutodetectCapableMBeanInfoAssembler assembler) {
		autodetect(new AutodetectCallback() {
			@Override
			public boolean include(Class<?> beanClass, String beanName) {
				return assembler.includeBean(beanClass, beanName);
			}
		});
	}

	/**
	 * 尝试检测{@code ApplicationContext}中定义的有效MBean中的bean, 并使用{@code MBeanServer}自动注册它们.
	 */
	private void autodetectMBeans() {
		autodetect(new AutodetectCallback() {
			@Override
			public boolean include(Class<?> beanClass, String beanName) {
				return isMBean(beanClass);
			}
		});
	}

	/**
	 * 执行实际的自动检测过程, 委托给{@code AutodetectCallback}实例, 以对包含给定bean进行投票.
	 * 
	 * @param callback 在决定是否包含bean时使用的{@code AutodetectCallback}
	 */
	private void autodetect(AutodetectCallback callback) {
		Set<String> beanNames = new LinkedHashSet<String>(this.beanFactory.getBeanDefinitionCount());
		beanNames.addAll(Arrays.asList(this.beanFactory.getBeanDefinitionNames()));
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			beanNames.addAll(Arrays.asList(((ConfigurableBeanFactory) this.beanFactory).getSingletonNames()));
		}
		for (String beanName : beanNames) {
			if (!isExcluded(beanName) && !isBeanDefinitionAbstract(this.beanFactory, beanName)) {
				try {
					Class<?> beanClass = this.beanFactory.getType(beanName);
					if (beanClass != null && callback.include(beanClass, beanName)) {
						boolean lazyInit = isBeanDefinitionLazyInit(this.beanFactory, beanName);
						Object beanInstance = (!lazyInit ? this.beanFactory.getBean(beanName) : null);
						if (!ScopedProxyUtils.isScopedTarget(beanName) && !this.beans.containsValue(beanName) &&
								(beanInstance == null ||
										!CollectionUtils.containsInstance(this.beans.values(), beanInstance))) {
							// Not already registered for JMX exposure.
							this.beans.put(beanName, (beanInstance != null ? beanInstance : beanName));
							if (logger.isInfoEnabled()) {
								logger.info("Bean with name '" + beanName + "' has been autodetected for JMX exposure");
							}
						}
						else {
							if (logger.isDebugEnabled()) {
								logger.debug("Bean with name '" + beanName + "' is already registered for JMX exposure");
							}
						}
					}
				}
				catch (CannotLoadBeanClassException ex) {
					if (this.allowEagerInit) {
						throw ex;
					}
					// 否则忽略类无法解析的bean
				}
			}
		}
	}

	/**
	 * 指示排除的bean列表中是否存在特定的bean名称.
	 */
	private boolean isExcluded(String beanName) {
		return (this.excludedBeans.contains(beanName) ||
					(beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX) &&
							this.excludedBeans.contains(beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length()))));
	}

	/**
	 * 返回是否应将指定的bean定义视为抽象.
	 */
	private boolean isBeanDefinitionAbstract(ListableBeanFactory beanFactory, String beanName) {
		return (beanFactory instanceof ConfigurableListableBeanFactory && beanFactory.containsBeanDefinition(beanName) &&
				((ConfigurableListableBeanFactory) beanFactory).getBeanDefinition(beanName).isAbstract());
	}


	//---------------------------------------------------------------------
	// Notification and listener management
	//---------------------------------------------------------------------

	/**
	 * 如果提供的托管资源实现{@link NotificationPublisherAware},
	 * 则会注入{@link org.springframework.jmx.export.notification.NotificationPublisher}的实例.
	 */
	private void injectNotificationPublisherIfNecessary(
			Object managedResource, ModelMBean modelMBean, ObjectName objectName) {

		if (managedResource instanceof NotificationPublisherAware) {
			((NotificationPublisherAware) managedResource).setNotificationPublisher(
					new ModelMBeanNotificationPublisher(modelMBean, objectName, managedResource));
		}
	}

	/**
	 * 使用{@link MBeanServer}注册已配置的{@link NotificationListener NotificationListeners}.
	 */
	private void registerNotificationListeners() throws MBeanExportException {
		if (this.notificationListeners != null) {
			for (NotificationListenerBean bean : this.notificationListeners) {
				try {
					ObjectName[] mappedObjectNames = bean.getResolvedObjectNames();
					if (mappedObjectNames == null) {
						// 映射到MBeanExporter注册的所有MBean.
						mappedObjectNames = getRegisteredObjectNames();
					}
					if (this.registeredNotificationListeners.put(bean, mappedObjectNames) == null) {
						for (ObjectName mappedObjectName : mappedObjectNames) {
							this.server.addNotificationListener(mappedObjectName, bean.getNotificationListener(),
									bean.getNotificationFilter(), bean.getHandback());
						}
					}
				}
				catch (Throwable ex) {
					throw new MBeanExportException("Unable to register NotificationListener", ex);
				}
			}
		}
	}

	/**
	 * 从{@link MBeanServer}注销已配置的{@link NotificationListener NotificationListeners}.
	 */
	private void unregisterNotificationListeners() {
		for (Map.Entry<NotificationListenerBean, ObjectName[]> entry : this.registeredNotificationListeners.entrySet()) {
			NotificationListenerBean bean = entry.getKey();
			ObjectName[] mappedObjectNames = entry.getValue();
			for (ObjectName mappedObjectName : mappedObjectNames) {
				try {
					this.server.removeNotificationListener(mappedObjectName, bean.getNotificationListener(),
							bean.getNotificationFilter(), bean.getHandback());
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Unable to unregister NotificationListener", ex);
					}
				}
			}
		}
		this.registeredNotificationListeners.clear();
	}

	/**
	 * 注册MBean时调用. 通知所有已注册的{@link MBeanExporterListener MBeanExporterListeners}注册事件.
	 * <p>请注意, 如果{@link MBeanExporterListener} 在收到通知时抛出 (运行时)异常,
	 * 这将基本上中断通知过程, 任何尚未通知的剩余侦听器将 (显然)不会收到
	 * {@link MBeanExporterListener#mbeanRegistered(javax.management.ObjectName)}回调.
	 * 
	 * @param objectName 已注册的MBean的{@code ObjectName}
	 */
	@Override
	protected void onRegister(ObjectName objectName) {
		notifyListenersOfRegistration(objectName);
	}

	/**
	 * 在注销MBean时调用. 通知所有已注册的{@link MBeanExporterListener MBeanExporterListeners}注销事件.
	 * <p>请注意, 如果{@link MBeanExporterListener} 在收到通知时抛出 (运行时)异常,
	 * 这将基本上中断通知过程, 任何尚未通知的剩余侦听器将 (显然)不会收到
	 * {@link MBeanExporterListener#mbeanUnregistered(javax.management.ObjectName)}回调.
	 * 
	 * @param objectName 已注销的MBean的{@code ObjectName}
	 */
	@Override
	protected void onUnregister(ObjectName objectName) {
		notifyListenersOfUnregistration(objectName);
	}


    /**
	 * 通知所有已注册的{@link MBeanExporterListener MBeanExporterListeners}所提供的{@link ObjectName}标识的MBean的注册.
	 */
	private void notifyListenersOfRegistration(ObjectName objectName) {
		if (this.listeners != null) {
			for (MBeanExporterListener listener : this.listeners) {
				listener.mbeanRegistered(objectName);
			}
		}
	}

	/**
	 * 通知所有已注册的{@link MBeanExporterListener MBeanExporterListeners}所提供的{@link ObjectName}标识的MBean的注销.
	 */
	private void notifyListenersOfUnregistration(ObjectName objectName) {
		if (this.listeners != null) {
			for (MBeanExporterListener listener : this.listeners) {
				listener.mbeanUnregistered(objectName);
			}
		}
	}


	//---------------------------------------------------------------------
	// Inner classes for internal use
	//---------------------------------------------------------------------

	/**
	 * 自动检测过程的内部回调接口.
	 */
	private static interface AutodetectCallback {

		/**
		 * 在自动检测过程中调用, 以确定是否应包含bean.
		 * 
		 * @param beanClass bean的类
		 * @param beanName bean的名称
		 */
		boolean include(Class<?> beanClass, String beanName);
	}


	/**
	 * 扩展{@link LazyInitTargetSource}, 如果需要, 将在创建延迟资源时,
	 * 将{@link org.springframework.jmx.export.notification.NotificationPublisher}注入其中.
	 */
	@SuppressWarnings("serial")
	private class NotificationPublisherAwareLazyTargetSource extends LazyInitTargetSource {

		private ModelMBean modelMBean;

		private ObjectName objectName;

		public void setModelMBean(ModelMBean modelMBean) {
			this.modelMBean = modelMBean;
		}

		public void setObjectName(ObjectName objectName) {
			this.objectName = objectName;
		}

		@Override
		public Object getTarget() {
			try {
				return super.getTarget();
			}
			catch (RuntimeException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to retrieve target for JMX-exposed bean [" + this.objectName + "]: " + ex);
				}
				throw ex;
			}
		}

		@Override
		protected void postProcessTargetObject(Object targetObject) {
			injectNotificationPublisherIfNecessary(targetObject, this.modelMBean, this.objectName);
		}
	}

}
