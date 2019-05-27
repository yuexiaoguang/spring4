package org.springframework.scripting.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.asm.Type;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.proxy.InterfaceMaker;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor},
 * 处理{@link org.springframework.scripting.ScriptFactory}定义,
 * 用它生成的实际脚本Java对象替换每个工厂.
 *
 * <p>类似于{@link org.springframework.beans.factory.FactoryBean}机制,
 * 但是专为脚本量身定制, 而不是内置于Spring的核心容器本身, 而是作为扩展实现的.
 *
 * <p><b>NOTE:</b> 此后处理器最重要的特性是, 构造函数参数应用于{@link org.springframework.scripting.ScriptFactory}实例,
 * 而bean属性值应用于生成的脚本对象.
 * 通常, 构造函数参数包括脚本源定位器和可能的脚本接口, 而bean属性值包括要注入脚本对象本身的引用和配置值.
 *
 * <p>以下{@link ScriptFactoryPostProcessor}将自动应用于下面的两个{@link org.springframework.scripting.ScriptFactory}定义.
 * 在运行时, 实际的脚本对象将暴露为"bshMessenger"和 "groovyMessenger", 而不是{@link org.springframework.scripting.ScriptFactory}实例.
 * 这两个都应该可以转换为示例的{@code Messenger}接口.
 *
 * <pre class="code">&lt;bean class="org.springframework.scripting.support.ScriptFactoryPostProcessor"/&gt;
 *
 * &lt;bean id="bshMessenger" class="org.springframework.scripting.bsh.BshScriptFactory"&gt;
 *   &lt;constructor-arg value="classpath:mypackage/Messenger.bsh"/&gt;
 *   &lt;constructor-arg value="mypackage.Messenger"/&gt;
 *   &lt;property name="message" value="Hello World!"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="groovyMessenger" class="org.springframework.scripting.groovy.GroovyScriptFactory"&gt;
 *   &lt;constructor-arg value="classpath:mypackage/Messenger.groovy"/&gt;
 *   &lt;property name="message" value="Hello World!"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p><b>NOTE:</b> 请注意, Spring XML bean定义文件的上述摘录仅使用 &lt;bean/&gt;-style语法
 * (为了说明使用{@link ScriptFactoryPostProcessor}本身).
 * 实际上, 你永远不会显式为 {@link ScriptFactoryPostProcessor}创建一个 &lt;bean/&gt; 定义;
 * 相反, 您将从{@code 'lang'}命名空间导入标签, 并使用该命名空间中的标签简单地创建脚本bean...
 * 作为这样做的一部分, 将隐式为您创建{@link ScriptFactoryPostProcessor}.
 *
 * <p>Spring参考文档包含许多在{@code 'lang'}命名空间中使用标签的示例;
 * 举个例子, 在下面找到一个使用{@code 'lang:groovy'}标签定义的Groovy支持的bean.
 *
 * <pre class="code">
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;beans xmlns="http://www.springframework.org/schema/beans"
 *     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *     xmlns:lang="http://www.springframework.org/schema/lang"&gt;
 *
 *   &lt;!-- this is the bean definition for the Groovy-backed Messenger implementation --&gt;
 *   &lt;lang:groovy id="messenger" script-source="classpath:Messenger.groovy"&gt;
 *     &lt;lang:property name="message" value="I Can Do The Frug" /&gt;
 *   &lt;/lang:groovy&gt;
 *
 *   &lt;!-- an otherwise normal bean that will be injected by the Groovy-backed Messenger --&gt;
 *   &lt;bean id="bookingService" class="x.y.DefaultBookingService"&gt;
 *     &lt;property name="messenger" ref="messenger" /&gt;
 *   &lt;/bean&gt;
 *
 * &lt;/beans&gt;</pre>
 */
public class ScriptFactoryPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements
		BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware, DisposableBean, Ordered {

	/**
	 * {@link org.springframework.core.io.Resource}式前缀表示内联脚本.
	 * <p>内联脚本是在 (通常是XML)配置中定义的脚本, 而不是在外部文件中定义的脚本.
	 */
	public static final String INLINE_SCRIPT_PREFIX = "inline:";

	public static final String REFRESH_CHECK_DELAY_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ScriptFactoryPostProcessor.class, "refreshCheckDelay");

	public static final String PROXY_TARGET_CLASS_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ScriptFactoryPostProcessor.class, "proxyTargetClass");

	public static final String LANGUAGE_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ScriptFactoryPostProcessor.class, "language");

	private static final String SCRIPT_FACTORY_NAME_PREFIX = "scriptFactory.";

	private static final String SCRIPTED_OBJECT_NAME_PREFIX = "scriptedObject.";

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private long defaultRefreshCheckDelay = -1;

	private boolean defaultProxyTargetClass = false;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private ConfigurableBeanFactory beanFactory;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	final DefaultListableBeanFactory scriptBeanFactory = new DefaultListableBeanFactory();

	/** bean名称 -> ScriptSource对象 */
	private final Map<String, ScriptSource> scriptSourceCache = new HashMap<String, ScriptSource>();

	/**
	 * 设置刷新检查之间的延迟, 以毫秒为单位.
	 * 默认 -1, 表示根本没有刷新检查.
	 * <p>请注意, 实际刷新只会在{@link org.springframework.scripting.ScriptSource}表明它已被修改时发生.
	 */
	public void setDefaultRefreshCheckDelay(long defaultRefreshCheckDelay) {
		this.defaultRefreshCheckDelay = defaultRefreshCheckDelay;
	}

	/**
	 * 用于表示应创建可刷新代理, 以代理目标类而不是其接口.
	 * 
	 * @param defaultProxyTargetClass
	 */
	public void setDefaultProxyTargetClass(boolean defaultProxyTargetClass) {
		this.defaultProxyTargetClass = defaultProxyTargetClass;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("ScriptFactoryPostProcessor doesn't work with " +
					"non-ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;

		// 必需, 以便正确解析引用 (向上容器层次结构).
		this.scriptBeanFactory.setParentBeanFactory(this.beanFactory);

		// 必需, 以便所有BeanPostProcessors, Scopes等可用.
		this.scriptBeanFactory.copyConfigurationFrom(this.beanFactory);

		// 筛选出作为AOP基础结构一部分的BeanPostProcessors, 因为这些只适用于原始工厂中定义的bean.
		for (Iterator<BeanPostProcessor> it = this.scriptBeanFactory.getBeanPostProcessors().iterator(); it.hasNext();) {
			if (it.next() instanceof AopInfrastructureBean) {
				it.remove();
			}
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public int getOrder() {
		return Integer.MIN_VALUE;
	}

	@Override
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		// 这里只对ScriptFactory实现应用特殊处理.
		if (!ScriptFactory.class.isAssignableFrom(beanClass)) {
			return null;
		}

		BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);

		try {
			String scriptFactoryBeanName = SCRIPT_FACTORY_NAME_PREFIX + beanName;
			String scriptedObjectBeanName = SCRIPTED_OBJECT_NAME_PREFIX + beanName;
			prepareScriptBeans(bd, scriptFactoryBeanName, scriptedObjectBeanName);

			ScriptFactory scriptFactory = this.scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
			ScriptSource scriptSource = getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
			Class<?>[] interfaces = scriptFactory.getScriptInterfaces();

			Class<?> scriptedType = scriptFactory.getScriptedObjectType(scriptSource);
			if (scriptedType != null) {
				return scriptedType;
			}
			else if (!ObjectUtils.isEmpty(interfaces)) {
				return (interfaces.length == 1 ? interfaces[0] : createCompositeInterface(interfaces));
			}
			else {
				if (bd.isSingleton()) {
					Object bean = this.scriptBeanFactory.getBean(scriptedObjectBeanName);
					if (bean != null) {
						return bean.getClass();
					}
				}
			}
		}
		catch (Exception ex) {
			if (ex instanceof BeanCreationException &&
					((BeanCreationException) ex).getMostSpecificCause() instanceof BeanCurrentlyInCreationException) {
				if (logger.isTraceEnabled()) {
					logger.trace("Could not determine scripted object type for bean '" + beanName + "': "
							+ ex.getMessage());
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not determine scripted object type for bean '" + beanName + "'", ex);
				}
			}
		}

		return null;
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		// 这里只对ScriptFactory实现应用特殊处理.
		if (!ScriptFactory.class.isAssignableFrom(beanClass)) {
			return null;
		}

		BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);
		String scriptFactoryBeanName = SCRIPT_FACTORY_NAME_PREFIX + beanName;
		String scriptedObjectBeanName = SCRIPTED_OBJECT_NAME_PREFIX + beanName;
		prepareScriptBeans(bd, scriptFactoryBeanName, scriptedObjectBeanName);

		ScriptFactory scriptFactory = this.scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
		ScriptSource scriptSource = getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
		boolean isFactoryBean = false;
		try {
			Class<?> scriptedObjectType = scriptFactory.getScriptedObjectType(scriptSource);
			// 如果工厂无法确定类型, 则返回的类型可能为null.
			if (scriptedObjectType != null) {
				isFactoryBean = FactoryBean.class.isAssignableFrom(scriptedObjectType);
			}
		}
		catch (Exception ex) {
			throw new BeanCreationException(beanName,
					"Could not determine scripted object type for " + scriptFactory, ex);
		}

		long refreshCheckDelay = resolveRefreshCheckDelay(bd);
		if (refreshCheckDelay >= 0) {
			Class<?>[] interfaces = scriptFactory.getScriptInterfaces();
			RefreshableScriptTargetSource ts = new RefreshableScriptTargetSource(this.scriptBeanFactory,
					scriptedObjectBeanName, scriptFactory, scriptSource, isFactoryBean);
			boolean proxyTargetClass = resolveProxyTargetClass(bd);
			String language = (String) bd.getAttribute(LANGUAGE_ATTRIBUTE);
			if (proxyTargetClass && (language == null || !language.equals("groovy"))) {
				throw new BeanDefinitionValidationException(
						"Cannot use proxyTargetClass=true with script beans where language is not 'groovy': '" +
						language + "'");
			}
			ts.setRefreshCheckDelay(refreshCheckDelay);
			return createRefreshableProxy(ts, interfaces, proxyTargetClass);
		}

		if (isFactoryBean) {
			scriptedObjectBeanName = BeanFactory.FACTORY_BEAN_PREFIX + scriptedObjectBeanName;
		}
		return this.scriptBeanFactory.getBean(scriptedObjectBeanName);
	}

	/**
	 * 在此后处理器使用的内部BeanFactory中准备脚本bean.
	 * 每个原始bean定义将分为ScriptFactory定义和脚本对象定义.
	 * 
	 * @param bd 主要的BeanFactory中的原始bean定义
	 * @param scriptFactoryBeanName 内部ScriptFactory bean的名称
	 * @param scriptedObjectBeanName 内部脚本对象bean的名称
	 */
	protected void prepareScriptBeans(BeanDefinition bd, String scriptFactoryBeanName, String scriptedObjectBeanName) {
		// 在原型的情况下, 避免重新创建脚本bean定义.
		synchronized (this.scriptBeanFactory) {
			if (!this.scriptBeanFactory.containsBeanDefinition(scriptedObjectBeanName)) {

				this.scriptBeanFactory.registerBeanDefinition(
						scriptFactoryBeanName, createScriptFactoryBeanDefinition(bd));
				ScriptFactory scriptFactory =
						this.scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
				ScriptSource scriptSource =
						getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
				Class<?>[] interfaces = scriptFactory.getScriptInterfaces();

				Class<?>[] scriptedInterfaces = interfaces;
				if (scriptFactory.requiresConfigInterface() && !bd.getPropertyValues().isEmpty()) {
					Class<?> configInterface = createConfigInterface(bd, interfaces);
					scriptedInterfaces = ObjectUtils.addObjectToArray(interfaces, configInterface);
				}

				BeanDefinition objectBd = createScriptedObjectBeanDefinition(
						bd, scriptFactoryBeanName, scriptSource, scriptedInterfaces);
				long refreshCheckDelay = resolveRefreshCheckDelay(bd);
				if (refreshCheckDelay >= 0) {
					objectBd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
				}

				this.scriptBeanFactory.registerBeanDefinition(scriptedObjectBeanName, objectBd);
			}
		}
	}

	/**
	 * 获取给定{@link ScriptFactory} {@link BeanDefinition}的刷新检查延迟.
	 * 如果{@link BeanDefinition}在键{@link #REFRESH_CHECK_DELAY_ATTRIBUTE}下
	 * 有{@link org.springframework.core.AttributeAccessor 元数据属性}, 这是一个有效的{@link Number}类型, 则使用此值.
	 * 否则, 使用{@link #defaultRefreshCheckDelay}值.
	 * 
	 * @param beanDefinition 要检查的BeanDefinition
	 * 
	 * @return 刷新检查延迟
	 */
	protected long resolveRefreshCheckDelay(BeanDefinition beanDefinition) {
		long refreshCheckDelay = this.defaultRefreshCheckDelay;
		Object attributeValue = beanDefinition.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
		if (attributeValue instanceof Number) {
			refreshCheckDelay = ((Number) attributeValue).longValue();
		}
		else if (attributeValue instanceof String) {
			refreshCheckDelay = Long.parseLong((String) attributeValue);
		}
		else if (attributeValue != null) {
			throw new BeanDefinitionStoreException("Invalid refresh check delay attribute [" +
					REFRESH_CHECK_DELAY_ATTRIBUTE + "] with value '" + attributeValue +
					"': needs to be of type Number or String");
		}
		return refreshCheckDelay;
	}

	protected boolean resolveProxyTargetClass(BeanDefinition beanDefinition) {
		boolean proxyTargetClass = this.defaultProxyTargetClass;
		Object attributeValue = beanDefinition.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE);
		if (attributeValue instanceof Boolean) {
			proxyTargetClass = (Boolean) attributeValue;
		}
		else if (attributeValue instanceof String) {
			proxyTargetClass = Boolean.valueOf((String) attributeValue);
		}
		else if (attributeValue != null) {
			throw new BeanDefinitionStoreException("Invalid proxy target class attribute [" +
					PROXY_TARGET_CLASS_ATTRIBUTE + "] with value '" + attributeValue +
					"': needs to be of type Boolean or String");
		}
		return proxyTargetClass;
	}

	/**
	 * 基于给定的脚本定义创建ScriptFactory bean定义, 仅提取与ScriptFactory相关的定义数据
	 * (也就是说, 只有bean类和构造函数参数).
	 * 
	 * @param bd 完整的脚本bean定义
	 * 
	 * @return 提取的ScriptFactory bean定义
	 */
	protected BeanDefinition createScriptFactoryBeanDefinition(BeanDefinition bd) {
		GenericBeanDefinition scriptBd = new GenericBeanDefinition();
		scriptBd.setBeanClassName(bd.getBeanClassName());
		scriptBd.getConstructorArgumentValues().addArgumentValues(bd.getConstructorArgumentValues());
		return scriptBd;
	}

	/**
	 * 获取给定bean的ScriptSource, 如果没有缓存, 则延迟创建它.
	 * 
	 * @param beanName 脚本bean的名称
	 * @param scriptSourceLocator 与bean关联的脚本源定位器
	 * 
	 * @return 相应的ScriptSource实例
	 */
	protected ScriptSource getScriptSource(String beanName, String scriptSourceLocator) {
		synchronized (this.scriptSourceCache) {
			ScriptSource scriptSource = this.scriptSourceCache.get(beanName);
			if (scriptSource == null) {
				scriptSource = convertToScriptSource(beanName, scriptSourceLocator, this.resourceLoader);
				this.scriptSourceCache.put(beanName, scriptSource);
			}
			return scriptSource;
		}
	}

	/**
	 * 将给定的脚本源定位器转换为ScriptSource实例.
	 * <p>默认情况下, 支持的定位器是Spring资源位置(例如 "file:C:/myScript.bsh" or "classpath:myPackage/myScript.bsh"),
	 * 和内联脚本("inline:myScriptText...").
	 * 
	 * @param beanName 脚本bean的名称
	 * @param scriptSourceLocator 脚本源定位器
	 * @param resourceLoader 要使用的ResourceLoader
	 * 
	 * @return ScriptSource实例
	 */
	protected ScriptSource convertToScriptSource(String beanName, String scriptSourceLocator,
			ResourceLoader resourceLoader) {

		if (scriptSourceLocator.startsWith(INLINE_SCRIPT_PREFIX)) {
			return new StaticScriptSource(scriptSourceLocator.substring(INLINE_SCRIPT_PREFIX.length()), beanName);
		}
		else {
			return new ResourceScriptSource(resourceLoader.getResource(scriptSourceLocator));
		}
	}

	/**
	 * 为给定的bean定义创建配置接口, 为定义的属性值定义setter方法, 以及init方法和destroy方法.
	 * <p>此实现通过CGLIB的InterfaceMaker创建接口, 从给定接口确定属性类型.
	 * 
	 * @param bd 要创建配置接口的bean定义(属性值)
	 * @param interfaces 要检查的接口 (可能会定义与应该生成的setter相对应的getter)
	 * 
	 * @return 配置接口
	 */
	protected Class<?> createConfigInterface(BeanDefinition bd, Class<?>[] interfaces) {
		InterfaceMaker maker = new InterfaceMaker();
		PropertyValue[] pvs = bd.getPropertyValues().getPropertyValues();
		for (PropertyValue pv : pvs) {
			String propertyName = pv.getName();
			Class<?> propertyType = BeanUtils.findPropertyType(propertyName, interfaces);
			String setterName = "set" + StringUtils.capitalize(propertyName);
			Signature signature = new Signature(setterName, Type.VOID_TYPE, new Type[] {Type.getType(propertyType)});
			maker.add(signature, new Type[0]);
		}
		if (bd instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
			if (abd.getInitMethodName() != null) {
				Signature signature = new Signature(abd.getInitMethodName(), Type.VOID_TYPE, new Type[0]);
				maker.add(signature, new Type[0]);
			}
			if (StringUtils.hasText(abd.getDestroyMethodName())) {
				Signature signature = new Signature(abd.getDestroyMethodName(), Type.VOID_TYPE, new Type[0]);
				maker.add(signature, new Type[0]);
			}
		}
		return maker.create();
	}

	/**
	 * 为给定接口创建复合接口Class, 在单个Class中实现给定接口.
	 * <p>默认实现为给定接口构建JDK代理类.
	 * 
	 * @param interfaces 要合并的接口
	 * 
	 * @return 合并后的接口
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.beanClassLoader);
	}

	/**
	 * 根据给定的脚本定义为脚本对象创建bean定义,
	 * 提取与脚本对象相关的定义数据 (即, 除了bean类和构造函数参数之外的所有内容).
	 * 
	 * @param bd 完整的脚本bean定义
	 * @param scriptFactoryBeanName 内部ScriptFactory bean的名称
	 * @param scriptSource 脚本bean的ScriptSource
	 * @param interfaces 脚本bean应该实现的接口
	 * 
	 * @return 提取的ScriptFactory bean定义
	 */
	protected BeanDefinition createScriptedObjectBeanDefinition(BeanDefinition bd, String scriptFactoryBeanName,
			ScriptSource scriptSource, Class<?>[] interfaces) {

		GenericBeanDefinition objectBd = new GenericBeanDefinition(bd);
		objectBd.setFactoryBeanName(scriptFactoryBeanName);
		objectBd.setFactoryMethodName("getScriptedObject");
		objectBd.getConstructorArgumentValues().clear();
		objectBd.getConstructorArgumentValues().addIndexedArgumentValue(0, scriptSource);
		objectBd.getConstructorArgumentValues().addIndexedArgumentValue(1, interfaces);
		return objectBd;
	}

	/**
	 * 为给定的AOP TargetSource创建可刷新的代理.
	 * 
	 * @param ts 可刷新的TargetSource
	 * @param interfaces 代理接口 (可能是{@code null}以指示代理目标类实现的所有接口)
	 * 
	 * @return 生成的代理
	 */
	protected Object createRefreshableProxy(TargetSource ts, Class<?>[] interfaces, boolean proxyTargetClass) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(ts);
		ClassLoader classLoader = this.beanClassLoader;

		if (interfaces == null) {
			interfaces = ClassUtils.getAllInterfacesForClass(ts.getTargetClass(), this.beanClassLoader);
		}
		proxyFactory.setInterfaces(interfaces);
		if (proxyTargetClass) {
			classLoader = null;  // force use of Class.getClassLoader()
			proxyFactory.setProxyTargetClass(true);
		}

		DelegatingIntroductionInterceptor introduction = new DelegatingIntroductionInterceptor(ts);
		introduction.suppressInterface(TargetSource.class);
		proxyFactory.addAdvice(introduction);

		return proxyFactory.getProxy(classLoader);
	}

	/**
	 * 在关闭时销毁内部bean工厂 (用于脚本).
	 */
	@Override
	public void destroy() {
		this.scriptBeanFactory.destroySingletons();
	}
}
