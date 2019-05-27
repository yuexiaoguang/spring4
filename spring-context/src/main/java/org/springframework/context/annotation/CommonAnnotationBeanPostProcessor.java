package org.springframework.context.annotation;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.jndi.support.SimpleJndiBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现, 支持开箱即用的常见Java注解,
 * 特别是{@code javax.annotation}包中的JSR-250注释.
 * 许多Java EE 5技术 (e.g. JSF 1.2)以及Java 6的JAX-WS都支持这些常见的Java注解.
 *
 * <p>此后处理器包括对 {@link javax.annotation.PostConstruct}和{@link javax.annotation.PreDestroy}注解的支持
 *  - 分别作为init注释和destroy注释 - 通过使用预先配置的注解类型继承自 {@link InitDestroyAnnotationBeanPostProcessor}.
 *
 * <p>中心元素是 {@link javax.annotation.Resource}注解, 用于命名bean的注解驱动注入,
 * 默认情况下, 从包含Spring BeanFactory, 在JNDI中仅解析{@code mappedName}引用.
 * {@link #setAlwaysUseJndiLookup "alwaysUseJndiLookup" 标志} 强制执行JNDI查找,
 * 等同于{@code name}引用和默认名称的标准Java EE 5资源注入.
 * 目标bean可以是简单的POJO, 除了必须匹配的类型之外没有特殊要求.
 *
 * <p>也支持JAX-WS {@link javax.xml.ws.WebServiceRef}注解,
 * 类似于 {@link javax.annotation.Resource}, 但具有创建特定JAX-WS服务端点的能力.
 * 这可以指向按名称显式定义的资源, 也可以在本地指定的JAX-WS服务类上运行.
 * 最后, 这个后处理器还支持EJB 3 {@link javax.ejb.EJB}注解,
 * 类似于 {@link javax.annotation.Resource}, 具有为回退检索指定本地bean名称和全局JNDI名称的功能.
 * 在这种情况下, 目标bean可以是普通POJO以及EJB 3会话Bean.
 *
 * <p>Java 6 (JDK 1.6) 以及 Java EE 5/6中提供了此后处理器支持的通用注解
 * (它还为其常用注解提供了一个独立的jar, 允许在任何基于Java 5的应用程序中使用).
 *
 * <p>对于默认用法, 将资源名称解析为Spring bean名称, 只需在应用程序上下文中定义以下内容即可:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * 对于直接JNDI访问, 将资源名称解析为Java EE应用程序的 "java:comp/env/"命名空间中的JNDI资源引用, 请使用以下命令:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="alwaysUseJndiLookup" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * {@code mappedName}引用将始终在JNDI中解析, 也允许全局JNDI名称 (包括 "java:"前缀).
 * "alwaysUseJndiLookup" 标志只影响{@code name}引用和默认名称 (从字段名称/属性名称推断出来).
 *
 * <p><b>NOTE:</b> 默认的CommonAnnotationBeanPostProcessor 将由"context:annotation-config"和"context:component-scan"XML标签注册.
 * 如果要指定自定义CommonAnnotationBeanPostProcessor bean定义, 请删除或关闭默认注解配置!
 * <p><b>NOTE:</b> 注解将在XML注入之前执行;
 * 因此后一种配置将覆盖前者, 用于通过两种方法连接的属性.
 */
@SuppressWarnings("serial")
public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, Serializable {

	// Common Annotations 1.1 Resource.lookup() available? Not present on JDK 6...
	private static final Method lookupAttribute = ClassUtils.getMethodIfAvailable(Resource.class, "lookup");

	private static Class<? extends Annotation> webServiceRefClass = null;

	private static Class<? extends Annotation> ejbRefClass = null;

	static {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> clazz = (Class<? extends Annotation>)
					ClassUtils.forName("javax.xml.ws.WebServiceRef", CommonAnnotationBeanPostProcessor.class.getClassLoader());
			webServiceRefClass = clazz;
		}
		catch (ClassNotFoundException ex) {
			webServiceRefClass = null;
		}
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> clazz = (Class<? extends Annotation>)
					ClassUtils.forName("javax.ejb.EJB", CommonAnnotationBeanPostProcessor.class.getClassLoader());
			ejbRefClass = clazz;
		}
		catch (ClassNotFoundException ex) {
			ejbRefClass = null;
		}
	}


	private final Set<String> ignoredResourceTypes = new HashSet<String>(1);

	private boolean fallbackToDefaultTypeMatch = true;

	private boolean alwaysUseJndiLookup = false;

	private transient BeanFactory jndiFactory = new SimpleJndiBeanFactory();

	private transient BeanFactory resourceFactory;

	private transient BeanFactory beanFactory;

	private transient StringValueResolver embeddedValueResolver;

	private transient final Map<String, InjectionMetadata> injectionMetadataCache =
			new ConcurrentHashMap<String, InjectionMetadata>(256);


	/**
	 * init和destroy注解类型分别设置为 {@link javax.annotation.PostConstruct}和{@link javax.annotation.PreDestroy}.
	 */
	public CommonAnnotationBeanPostProcessor() {
		setOrder(Ordered.LOWEST_PRECEDENCE - 3);
		setInitAnnotationType(PostConstruct.class);
		setDestroyAnnotationType(PreDestroy.class);
		ignoreResourceType("javax.xml.ws.WebServiceContext");
	}


	/**
	 * 解析{@code @Resource}注解时, 忽略给定的资源类型.
	 * <p>默认情况下, {@code javax.xml.ws.WebServiceContext}接口将被忽略, 因为它将由JAX-WS运行时解析.
	 * 
	 * @param resourceType 要忽略的资源类型
	 */
	public void ignoreResourceType(String resourceType) {
		Assert.notNull(resourceType, "Ignored resource type must not be null");
		this.ignoredResourceTypes.add(resourceType);
	}

	/**
	 * 设置是否允许在没有指定显式名称的情况下回退到类型匹配.
	 * 仍将首先检查默认名称 (即字段名称或bean属性名称); 如果存在该名称的bean, 则会被占用.
	 * 但是, 如果不存在该名称的bean, 则如果此标志为 "true", 将尝试依赖关系的按类型解析.
	 * <p>默认是"true". 切换为"false", 为了在所有情况下强制执行按名称查找, 在没有名称匹配的情况下抛出异常.
	 */
	public void setFallbackToDefaultTypeMatch(boolean fallbackToDefaultTypeMatch) {
		this.fallbackToDefaultTypeMatch = fallbackToDefaultTypeMatch;
	}

	/**
	 * 设置是否始终使用等效于标准Java EE 5资源注入的JNDI查找, <b>即使对于{@code name}属性和默认名称</b>.
	 * <p>默认是 "false": 资源名称用于包含BeanFactory的Spring bean查找;
	 * 只有{@code mappedName}属性直接指向JNDI.
	 * 将此标志切换为 "true" 以在任何情况下强制执行Java EE样式的JNDI查找, 即使对于{@code name}属性和默认名称也是如此.
	 */
	public void setAlwaysUseJndiLookup(boolean alwaysUseJndiLookup) {
		this.alwaysUseJndiLookup = alwaysUseJndiLookup;
	}

	/**
	 * 指定要注入带 {@code @Resource} / {@code @WebServiceRef} / {@code @EJB}注解的字段和setter方法的对象的工厂,
	 * <b>用于直接指向JNDI的{@code mappedName}属性</b>.
	 * 如果"alwaysUseJndiLookup"设置为"true", 也将使用此工厂, 以便强制执行JNDI查找, 即使对于{@code name}属性和默认名称也是如此.
	 * <p>对于JNDI查找行为, 默认值为{@link org.springframework.jndi.support.SimpleJndiBeanFactory}, 等同于标准Java EE 5资源注入.
	 */
	public void setJndiFactory(BeanFactory jndiFactory) {
		Assert.notNull(jndiFactory, "BeanFactory must not be null");
		this.jndiFactory = jndiFactory;
	}

	/**
	 * 指定要注入带{@code @Resource} / {@code @WebServiceRef} / {@code @EJB}注解的字段和setter方法的对象的工厂,
	 * <b>用于{@code name}属性和默认名称</b>.
	 * <p>默认情况下是定义此后处理器的BeanFactory, 将资源名称查找为Spring bean名称.
	 * 显式指定资源工厂, 用于此后处理器的编程用法.
	 * <p>指定Spring的 {@link org.springframework.jndi.support.SimpleJndiBeanFactory},
	 * 导致JNDI查找行为等同于标准Java EE 5资源注入, 即使对于{@code name}属性和默认名称.
	 * 这与"alwaysUseJndiLookup"标志启用的行为相同.
	 */
	public void setResourceFactory(BeanFactory resourceFactory) {
		Assert.notNull(resourceFactory, "BeanFactory must not be null");
		this.resourceFactory = resourceFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		if (this.resourceFactory == null) {
			this.resourceFactory = beanFactory;
		}
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		super.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
		if (beanType != null) {
			InjectionMetadata metadata = findResourceMetadata(beanName, beanType, null);
			metadata.checkConfigMembers(beanDefinition);
		}
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
		}
		return pvs;
	}


	private InjectionMetadata findResourceMetadata(String beanName, final Class<?> clazz, PropertyValues pvs) {
		// 回退到类名作为缓存键, 以便与自定义调用者向后兼容.
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						metadata.clear(pvs);
					}
					try {
						metadata = buildResourceMetadata(clazz);
						this.injectionMetadataCache.put(cacheKey, metadata);
					}
					catch (NoClassDefFoundError err) {
						throw new IllegalStateException("Failed to introspect bean class [" + clazz.getName() +
								"] for resource metadata: could not find class that it depends on", err);
					}
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildResourceMetadata(final Class<?> clazz) {
		LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
		Class<?> targetClass = clazz;

		do {
			final LinkedList<InjectionMetadata.InjectedElement> currElements =
					new LinkedList<InjectionMetadata.InjectedElement>();

			ReflectionUtils.doWithLocalFields(targetClass, new ReflectionUtils.FieldCallback() {
				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
					if (webServiceRefClass != null && field.isAnnotationPresent(webServiceRefClass)) {
						if (Modifier.isStatic(field.getModifiers())) {
							throw new IllegalStateException("@WebServiceRef annotation is not supported on static fields");
						}
						currElements.add(new WebServiceRefElement(field, field, null));
					}
					else if (ejbRefClass != null && field.isAnnotationPresent(ejbRefClass)) {
						if (Modifier.isStatic(field.getModifiers())) {
							throw new IllegalStateException("@EJB annotation is not supported on static fields");
						}
						currElements.add(new EjbRefElement(field, field, null));
					}
					else if (field.isAnnotationPresent(Resource.class)) {
						if (Modifier.isStatic(field.getModifiers())) {
							throw new IllegalStateException("@Resource annotation is not supported on static fields");
						}
						if (!ignoredResourceTypes.contains(field.getType().getName())) {
							currElements.add(new ResourceElement(field, field, null));
						}
					}
				}
			});

			ReflectionUtils.doWithLocalMethods(targetClass, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
					if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
						return;
					}
					if (method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
						if (webServiceRefClass != null && bridgedMethod.isAnnotationPresent(webServiceRefClass)) {
							if (Modifier.isStatic(method.getModifiers())) {
								throw new IllegalStateException("@WebServiceRef annotation is not supported on static methods");
							}
							if (method.getParameterTypes().length != 1) {
								throw new IllegalStateException("@WebServiceRef annotation requires a single-arg method: " + method);
							}
							PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
							currElements.add(new WebServiceRefElement(method, bridgedMethod, pd));
						}
						else if (ejbRefClass != null && bridgedMethod.isAnnotationPresent(ejbRefClass)) {
							if (Modifier.isStatic(method.getModifiers())) {
								throw new IllegalStateException("@EJB annotation is not supported on static methods");
							}
							if (method.getParameterTypes().length != 1) {
								throw new IllegalStateException("@EJB annotation requires a single-arg method: " + method);
							}
							PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
							currElements.add(new EjbRefElement(method, bridgedMethod, pd));
						}
						else if (bridgedMethod.isAnnotationPresent(Resource.class)) {
							if (Modifier.isStatic(method.getModifiers())) {
								throw new IllegalStateException("@Resource annotation is not supported on static methods");
							}
							Class<?>[] paramTypes = method.getParameterTypes();
							if (paramTypes.length != 1) {
								throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
							}
							if (!ignoredResourceTypes.contains(paramTypes[0].getName())) {
								PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
								currElements.add(new ResourceElement(method, bridgedMethod, pd));
							}
						}
					}
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return new InjectionMetadata(clazz, elements);
	}

	/**
	 * 获取给定名称和类型的延迟解析资源代理, 一旦方法调用进入, 就按需委派给{@link #getResource}.
	 * 
	 * @param element 被注解的字段/方法的描述符
	 * @param requestingBeanName 请求bean的名称
	 * 
	 * @return 资源对象 (never {@code null})
	 */
	protected Object buildLazyResourceProxy(final LookupElement element, final String requestingBeanName) {
		TargetSource ts = new TargetSource() {
			@Override
			public Class<?> getTargetClass() {
				return element.lookupType;
			}
			@Override
			public boolean isStatic() {
				return false;
			}
			@Override
			public Object getTarget() {
				return getResource(element, requestingBeanName);
			}
			@Override
			public void releaseTarget(Object target) {
			}
		};
		ProxyFactory pf = new ProxyFactory();
		pf.setTargetSource(ts);
		if (element.lookupType.isInterface()) {
			pf.addInterface(element.lookupType);
		}
		ClassLoader classLoader = (this.beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() : null);
		return pf.getProxy(classLoader);
	}

	/**
	 * 获取给定名称和类型的资源对象.
	 * 
	 * @param element 被注解的字段/方法的描述符
	 * @param requestingBeanName 请求bean的名称
	 * 
	 * @return 资源对象 (never {@code null})
	 * @throws BeansException 如果未能获得目标资源
	 */
	protected Object getResource(LookupElement element, String requestingBeanName) throws BeansException {
		if (StringUtils.hasLength(element.mappedName)) {
			return this.jndiFactory.getBean(element.mappedName, element.lookupType);
		}
		if (this.alwaysUseJndiLookup) {
			return this.jndiFactory.getBean(element.name, element.lookupType);
		}
		if (this.resourceFactory == null) {
			throw new NoSuchBeanDefinitionException(element.lookupType,
					"No resource factory configured - specify the 'resourceFactory' property");
		}
		return autowireResource(this.resourceFactory, element, requestingBeanName);
	}

	/**
	 * 根据给定的工厂, 通过自动装配获取给定名称和类型的资源对象.
	 * 
	 * @param factory 要自动装配的工厂
	 * @param element 被注解的字段/方法的描述符
	 * @param requestingBeanName 请求bean的名称
	 * 
	 * @return 资源对象 (never {@code null})
	 * @throws BeansException 如果未能获得目标资源
	 */
	protected Object autowireResource(BeanFactory factory, LookupElement element, String requestingBeanName)
			throws BeansException {

		Object resource;
		Set<String> autowiredBeanNames;
		String name = element.name;

		if (this.fallbackToDefaultTypeMatch && element.isDefaultName &&
				factory instanceof AutowireCapableBeanFactory && !factory.containsBean(name)) {
			autowiredBeanNames = new LinkedHashSet<String>();
			resource = ((AutowireCapableBeanFactory) factory).resolveDependency(
					element.getDependencyDescriptor(), requestingBeanName, autowiredBeanNames, null);
		}
		else {
			resource = factory.getBean(name, element.lookupType);
			autowiredBeanNames = Collections.singleton(name);
		}

		if (factory instanceof ConfigurableBeanFactory) {
			ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) factory;
			for (String autowiredBeanName : autowiredBeanNames) {
				if (beanFactory.containsBean(autowiredBeanName)) {
					beanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
				}
			}
		}

		return resource;
	}


	/**
	 * 表示有关被注解的字段或setter方法的通用注入信息的类, 支持@Resource和相关注解.
	 */
	protected abstract class LookupElement extends InjectionMetadata.InjectedElement {

		protected String name;

		protected boolean isDefaultName = false;

		protected Class<?> lookupType;

		protected String mappedName;

		public LookupElement(Member member, PropertyDescriptor pd) {
			super(member, pd);
		}

		/**
		 * 返回查找的资源名称.
		 */
		public final String getName() {
			return this.name;
		}

		/**
		 * 返回查找所需的类型.
		 */
		public final Class<?> getLookupType() {
			return this.lookupType;
		}

		/**
		 * 为底层字段/方法构建DependencyDescriptor.
		 */
		public final DependencyDescriptor getDependencyDescriptor() {
			if (this.isField) {
				return new LookupDependencyDescriptor((Field) this.member, this.lookupType);
			}
			else {
				return new LookupDependencyDescriptor((Method) this.member, this.lookupType);
			}
		}
	}


	/**
	 * 表示被注解的字段或setter方法的注入信息的类, 支持@Resource注解.
	 */
	private class ResourceElement extends LookupElement {

		private final boolean lazyLookup;

		public ResourceElement(Member member, AnnotatedElement ae, PropertyDescriptor pd) {
			super(member, pd);
			Resource resource = ae.getAnnotation(Resource.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			else if (embeddedValueResolver != null) {
				resourceName = embeddedValueResolver.resolveStringValue(resourceName);
			}
			if (resourceType != null && Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = resourceName;
			this.lookupType = resourceType;
			String lookupValue = (lookupAttribute != null ?
					(String) ReflectionUtils.invokeMethod(lookupAttribute, resource) : null);
			this.mappedName = (StringUtils.hasLength(lookupValue) ? lookupValue : resource.mappedName());
			Lazy lazy = ae.getAnnotation(Lazy.class);
			this.lazyLookup = (lazy != null && lazy.value());
		}

		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			return (this.lazyLookup ? buildLazyResourceProxy(this, requestingBeanName) :
					getResource(this, requestingBeanName));
		}
	}


	/**
	 * 表示被注解的字段或setter方法的注入信息的类, 支持 @WebServiceRef 注解.
	 */
	private class WebServiceRefElement extends LookupElement {

		private final Class<?> elementType;

		private final String wsdlLocation;

		public WebServiceRefElement(Member member, AnnotatedElement ae, PropertyDescriptor pd) {
			super(member, pd);
			WebServiceRef resource = ae.getAnnotation(WebServiceRef.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			if (resourceType != null && Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = resourceName;
			this.elementType = resourceType;
			if (Service.class.isAssignableFrom(resourceType)) {
				this.lookupType = resourceType;
			}
			else {
				this.lookupType = resource.value();
			}
			this.mappedName = resource.mappedName();
			this.wsdlLocation = resource.wsdlLocation();
		}

		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			Service service;
			try {
				service = (Service) getResource(this, requestingBeanName);
			}
			catch (NoSuchBeanDefinitionException notFound) {
				// 通过生成的类创建的服务.
				if (Service.class == this.lookupType) {
					throw new IllegalStateException("No resource with name '" + this.name + "' found in context, " +
							"and no specific JAX-WS Service subclass specified. The typical solution is to either specify " +
							"a LocalJaxWsServiceFactoryBean with the given name or to specify the (generated) Service " +
							"subclass as @WebServiceRef(...) value.");
				}
				if (StringUtils.hasLength(this.wsdlLocation)) {
					try {
						Constructor<?> ctor = this.lookupType.getConstructor(URL.class, QName.class);
						WebServiceClient clientAnn = this.lookupType.getAnnotation(WebServiceClient.class);
						if (clientAnn == null) {
							throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
									"] does not carry a WebServiceClient annotation");
						}
						service = (Service) BeanUtils.instantiateClass(ctor,
								new URL(this.wsdlLocation), new QName(clientAnn.targetNamespace(), clientAnn.name()));
					}
					catch (NoSuchMethodException ex) {
						throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
								"] does not have a (URL, QName) constructor. Cannot apply specified WSDL location [" +
								this.wsdlLocation + "].");
					}
					catch (MalformedURLException ex) {
						throw new IllegalArgumentException(
								"Specified WSDL location [" + this.wsdlLocation + "] isn't a valid URL");
					}
				}
				else {
					service = (Service) BeanUtils.instantiateClass(this.lookupType);
				}
			}
			return service.getPort(this.elementType);
		}
	}


	/**
	 * 表示被注解的字段或setter方法的注入信息的类, 支持 @EJB 注解.
	 */
	private class EjbRefElement extends LookupElement {

		private final String beanName;

		public EjbRefElement(Member member, AnnotatedElement ae, PropertyDescriptor pd) {
			super(member, pd);
			EJB resource = ae.getAnnotation(EJB.class);
			String resourceBeanName = resource.beanName();
			String resourceName = resource.name();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			Class<?> resourceType = resource.beanInterface();
			if (resourceType != null && Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.beanName = resourceBeanName;
			this.name = resourceName;
			this.lookupType = resourceType;
			this.mappedName = resource.mappedName();
		}

		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			if (StringUtils.hasLength(this.beanName)) {
				if (beanFactory != null && beanFactory.containsBean(this.beanName)) {
					// 显式指定的本地bean名称找到的本地匹配.
					Object bean = beanFactory.getBean(this.beanName, this.lookupType);
					if (beanFactory instanceof ConfigurableBeanFactory) {
						((ConfigurableBeanFactory) beanFactory).registerDependentBean(this.beanName, requestingBeanName);
					}
					return bean;
				}
				else if (this.isDefaultName && !StringUtils.hasLength(this.mappedName)) {
					throw new NoSuchBeanDefinitionException(this.beanName,
							"Cannot resolve 'beanName' in local BeanFactory. Consider specifying a general 'name' value instead.");
				}
			}
			// JNDI name lookup - may still go to a local BeanFactory.
			return getResource(this, requestingBeanName);
		}
	}


	/**
	 * DependencyDescriptor类的扩展, 使用指定的资源类型覆盖依赖项类型.
	 */
	private static class LookupDependencyDescriptor extends DependencyDescriptor {

		private final Class<?> lookupType;

		public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
			super(field, true);
			this.lookupType = lookupType;
		}

		public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
			super(new MethodParameter(method, 0), true);
			this.lookupType = lookupType;
		}

		@Override
		public Class<?> getDependencyType() {
			return this.lookupType;
		}
	}

}
