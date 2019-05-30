package org.springframework.orm.jpa.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.PersistenceUnit;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.orm.jpa.ExtendedEntityManagerCreator;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * BeanPostProcessor, 处理{@link javax.persistence.PersistenceUnit}
 * 和{@link javax.persistence.PersistenceContext}注解,
 * 用于注入相应的JPA资源{@link javax.persistence.EntityManagerFactory}和{@link javax.persistence.EntityManager}.
 * 将自动注入Spring管理的对象中的此类带注解的字段或方法.
 *
 * <p>如果注解的字段或方法声明为此类, 后处理器将注入{@code EntityManagerFactory}和{@code EntityManager}的子接口.
 * 实际类型将尽早验证, 但共享("事务") {@code EntityManager}引用除外, 其中可能会在第一次实际调用时检测到类型不匹配.
 *
 * <p>Note: 在本实现中，PersistenceAnnotationBeanPostProcessor仅支持具有"unitName"属性的
 * {@code @PersistenceUnit}和{{@code @PersistenceContext}, 或者根本不支持任何属性 (对于默认单元).
 * 如果这些注解在类级别与"name"属性一起出现, 那么它们将被忽略, 因为它们仅用作部署提示  (根据Java EE规范).
 *
 * <p>此后处理器可以获取在Spring应用程序上下文中定义的EntityManagerFactory bean (默认值),
 * 也可以从JNDI获取EntityManagerFactory引用 ("持久化单元引用").
 * 在bean的情况下, 持久化单元名称将与实际部署的单元匹配, 如果未找到部署的名称, 则将bean名称用作后备单元名称.
 * 通常, Spring的{@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}将用于设置此类EntityManagerFactory bean.
 * 或者, 这种bean也可以从JNDI获得, e.g. 使用{@code jee:jndi-lookup} XML配置元素 (bean名称与请求的单元名称匹配).
 * 在这两种情况下, 后处理器定义看起来都很简单:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * 在JNDI情况下, 在此后处理器的{@link #setPersistenceUnits "persistenceUnits" map}中指定相应的JNDI名称,
 * 通常在Java EE部署描述符中使用匹配的{@code persistence-unit-ref}条目.
 * 默认情况下，这些名称被视为资源引用 (根据Java EE资源引用约定), 位于"java:comp/env/"命名空间下面.
 * 例如:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceUnits"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/unit1"/&gt;
 *       &lt;entry key="unit2" value="persistence/unit2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 在这种情况下, 指定的持久化单元将始终在JNDI中解析, 而不是在Spring定义的bean中解析.
 * 然后, 整个持久化单元部署(包括写入持久化类)将由Java EE服务器完成.
 * 持久化上下文 (i.e. EntityManager引用)将基于服务器提供的EntityManagerFactory引用构建,
 * 使用Spring自己的事务同步工具进行事务性EntityManager处理
 * (通常使用Spring的 {@code @Transactional}注解进行分界, 将{@link org.springframework.transaction.jta.JtaTransactionManager}作为后端).
 *
 * <p>如果更喜欢Java EE服务器自己的EntityManager处理,
 * 在此后处理器的{@link #setPersistenceContexts "persistenceContexts" map}中指定条目
 * (或{@link #setExtendedPersistenceContexts "extendedPersistenceContexts" map}),
 * 通常在Java EE部署描述符中匹配{@code persistence-context-ref}条目.
 * 例如:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceContexts"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/context1"/&gt;
 *       &lt;entry key="unit2" value="persistence/context2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 如果应用程序首先只获取EntityManager引用, 那么这就是您需要指定的全部内容.
 * 如果还需要EntityManagerFactory引用, 请指定"persistenceUnits"和"persistenceContexts"的条目, 指向匹配的JNDI位置.
 *
 * <p><b>NOTE: 通常，不要将EXTENDED EntityManager注入STATELESS bean,
 * i.e. 不要在使用定义为'singleton'范围(Spring的默认范围)的Spring bean中使用{@code EXTENDED}类型的{@code @PersistenceContext}.</b>
 * 扩展的EntityManager <i>不是</i>线程安全的, 因此它们不能用于并发访问的bean (Spring管理的单例通常是).
 *
 * <p>Note: 默认的PersistenceAnnotationBeanPostProcessor将由"context:annotation-config"和"context:component-scan" XML标签注册.
 * 如果要指定自定义PersistenceAnnotationBeanPostProcessor bean定义, 请删除或关闭默认注解配置.
 */
@SuppressWarnings("serial")
public class PersistenceAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, DestructionAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, Serializable {

	/* 检查JPA 2.1 PersistenceContext.synchronization() 属性 */
	private static final Method synchronizationAttribute =
			ClassUtils.getMethodIfAvailable(PersistenceContext.class, "synchronization");


	private Object jndiEnvironment;

	private boolean resourceRef = true;

	private transient Map<String, String> persistenceUnits;

	private transient Map<String, String> persistenceContexts;

	private transient Map<String, String> extendedPersistenceContexts;

	private transient String defaultPersistenceUnitName = "";

	private int order = Ordered.LOWEST_PRECEDENCE - 4;

	private transient ListableBeanFactory beanFactory;

	private transient final Map<String, InjectionMetadata> injectionMetadataCache =
			new ConcurrentHashMap<String, InjectionMetadata>(256);

	private final Map<Object, EntityManager> extendedEntityManagersToClose =
			new ConcurrentHashMap<Object, EntityManager>(16);


	/**
	 * 设置用于JNDI查找的JNDI模板.
	 */
	public void setJndiTemplate(Object jndiTemplate) {
		this.jndiEnvironment = jndiTemplate;
	}

	/**
	 * 设置用于JNDI查找的JNDI环境.
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiEnvironment = jndiEnvironment;
	}

	/**
	 * 设置是否在J2EE容器中进行查找, i.e. 如果JNDI名称尚未包含它, 则需要添加前缀"java:comp/env/".
	 * PersistenceAnnotationBeanPostProcessor的默认值是"true".
	 */
	public void setResourceRef(boolean resourceRef) {
		this.resourceRef = resourceRef;
	}

	/**
	 * 指定EntityManagerFactory查找的持久化单元, 作为从持久化单元名称到持久化单元JNDI名称的Map (需要解析为EntityManagerFactory实例).
	 * <p>此处指定的JNDI名称应引用Java EE部署描述符中的{@code persistence-unit-ref}条目, 与目标持久化单元匹配.
	 * <p>如果注解中未指定单元名称, 则将获取{@link #setDefaultPersistenceUnitName 默认持久化单元}的指定值
	 * (默认情况下, 映射到空字符串的值), 或者只有单个持久化单元.
	 * <p>这主要用于Java EE环境, 所有查找由标准JPA注解驱动, 所有EntityManagerFactory引用从JNDI获取.
	 * 在这种情况下, 不需要单独的EntityManagerFactory bean定义.
	 * <p>如果没有指定相应的"persistenceContexts"/"extendedPersistenceContexts",
	 * {@code @PersistenceContext}将被解析为构建在此处定义的EntityManagerFactory之上的EntityManager.
	 * 请注意, 这些将是Spring管理的EntityManager, 它们基于Spring的工具实现事务同步.
	 * 如果更喜欢Java EE服务器自己的EntityManager处理, 请指定相应的"persistenceContexts"/"extendedPersistenceContexts".
	 */
	public void setPersistenceUnits(Map<String, String> persistenceUnits) {
		this.persistenceUnits = persistenceUnits;
	}

	/**
	 * 为EntityManager查找指定<i>事务性</i>持久化上下文, 作为从持久化单元名称到持久化上下文JNDI名称的Map
	 * (需要解析为EntityManager实例).
	 * <p>此处指定的JNDI名称应引用Java EE部署描述符中的{@code persistence-context-ref}条目,
	 * 与目标持久化单元匹配, 并使用持久化上下文类型{@code Transaction}进行设置.
	 * <p>如果注解中未指定单元名称, 则将获取{@link #setDefaultPersistenceUnitName 默认持久化单元}的指定值
	 * (默认情况下, 映射到空字符串的值), 或者只有单个持久化单元.
	 * <p>这主要用于Java EE环境, 所有查找由标准JPA注解驱动, 所有EntityManagerFactory引用从JNDI获取.
	 * 在这种情况下, 不需要单独的EntityManagerFactory bean定义, 并且所有EntityManager处理都由Java EE服务器本身完成.
	 */
	public void setPersistenceContexts(Map<String, String> persistenceContexts) {
		this.persistenceContexts = persistenceContexts;
	}

	/**
	 * 为EntityManager查找指定<i>扩展</i>持久化上下文, 作为从持久化单元名称到持久化上下文JNDI名称的Map
	 * (需要解析为EntityManager实例).
	 * <p>此处指定的JNDI名称应引用Java EE部署描述符中的{@code persistence-context-ref}条目,
	 * 与目标持久化单元匹配, 并使用持久化上下文类型{@code Extended}进行设置.
	 * <p>如果注解中未指定单元名称, 则将获取{@link #setDefaultPersistenceUnitName 默认持久化单元}的指定值
	 * (默认情况下, 映射到空字符串的值), 或者只有单个持久化单元.
	 * <p>这主要用于Java EE环境, 所有查找由标准JPA注解驱动, 所有EntityManagerFactory引用从JNDI获取.
	 * 在这种情况下, 不需要单独的EntityManagerFactory bean定义, 并且所有EntityManager处理都由Java EE服务器本身完成.
	 */
	public void setExtendedPersistenceContexts(Map<String, String> extendedPersistenceContexts) {
		this.extendedPersistenceContexts = extendedPersistenceContexts;
	}

	/**
	 * 指定默认持久化单元名称, 以便在{@code @PersistenceUnit} / {@code @PersistenceContext}注解中未指定单元名称时使用.
	 * <p>这主要用于应用程序上下文中的查找,
	 * 指示目标持久化单元名称 (通常与bean名称匹配), 但也适用于在
	 * {@link #setPersistenceUnits "persistenceUnits"} /
	 * {@link #setPersistenceContexts "persistenceContexts"} /
	 * {@link #setExtendedPersistenceContexts "extendedPersistenceContexts"} map中的查找,
	 * 从而避免了对空字符串的重复映射.
	 * <p>默认是在Spring应用程序上下文中检查单个EntityManagerFactory bean.
	 * 如果有多个此类工厂, 指定此默认持久化单元名称, 或在注解中显式引用命名持久化单元.
	 */
	public void setDefaultPersistenceUnitName(String unitName) {
		this.defaultPersistenceUnitName = (unitName != null ? unitName : "");
	}

	public void setOrder(int order) {
	  this.order = order;
	}

	@Override
	public int getOrder() {
	  return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (beanType != null) {
			InjectionMetadata metadata = findPersistenceMetadata(beanName, beanType, null);
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

		InjectionMetadata metadata = findPersistenceMetadata(beanName, bean.getClass(), pvs);
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of persistence dependencies failed", ex);
		}
		return pvs;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		EntityManager emToClose = this.extendedEntityManagersToClose.remove(bean);
		EntityManagerFactoryUtils.closeEntityManager(emToClose);
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return this.extendedEntityManagersToClose.containsKey(bean);
	}


	private InjectionMetadata findPersistenceMetadata(String beanName, final Class<?> clazz, PropertyValues pvs) {
		// 回退到类名作为缓存键, 以便与自定义调用者向后兼容.
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// 首先快速检查并发Map, 锁定最小.
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						metadata.clear(pvs);
					}
					try {
						metadata = buildPersistenceMetadata(clazz);
						this.injectionMetadataCache.put(cacheKey, metadata);
					}
					catch (NoClassDefFoundError err) {
						throw new IllegalStateException("Failed to introspect bean class [" + clazz.getName() +
								"] for persistence metadata: could not find class that it depends on", err);
					}
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildPersistenceMetadata(final Class<?> clazz) {
		LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
		Class<?> targetClass = clazz;

		do {
			final LinkedList<InjectionMetadata.InjectedElement> currElements =
					new LinkedList<InjectionMetadata.InjectedElement>();

			ReflectionUtils.doWithLocalFields(targetClass, new ReflectionUtils.FieldCallback() {
				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
					if (field.isAnnotationPresent(PersistenceContext.class) ||
							field.isAnnotationPresent(PersistenceUnit.class)) {
						if (Modifier.isStatic(field.getModifiers())) {
							throw new IllegalStateException("Persistence annotations are not supported on static fields");
						}
						currElements.add(new PersistenceElement(field, field, null));
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
					if ((bridgedMethod.isAnnotationPresent(PersistenceContext.class) ||
							bridgedMethod.isAnnotationPresent(PersistenceUnit.class)) &&
							method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("Persistence annotations are not supported on static methods");
						}
						if (method.getParameterTypes().length != 1) {
							throw new IllegalStateException("Persistence annotation requires a single-arg method: " + method);
						}
						PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
						currElements.add(new PersistenceElement(method, bridgedMethod, pd));
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
	 * 返回给定单元名称的指定持久化单元, 如 "persistenceUnits" map所定义.
	 * 
	 * @param unitName 持久化单元的名称
	 * 
	 * @return 相应的EntityManagerFactory, 或{@code null}
	 */
	protected EntityManagerFactory getPersistenceUnit(String unitName) {
		if (this.persistenceUnits != null) {
			String unitNameForLookup = (unitName != null ? unitName : "");
			if ("".equals(unitNameForLookup)) {
				unitNameForLookup = this.defaultPersistenceUnitName;
			}
			String jndiName = this.persistenceUnits.get(unitNameForLookup);
			if (jndiName == null && "".equals(unitNameForLookup) && this.persistenceUnits.size() == 1) {
				jndiName = this.persistenceUnits.values().iterator().next();
			}
			if (jndiName != null) {
				try {
					return lookup(jndiName, EntityManagerFactory.class);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Could not obtain EntityManagerFactory [" + jndiName + "] from JNDI", ex);
				}
			}
		}
		return null;
	}

	/**
	 * 返回给定单元名称的指定持久化上下文, 如"persistenceContexts" (或"extendedPersistenceContexts") map所定义.
	 * 
	 * @param unitName 持久化单元的名称
	 * @param extended 是否获取扩展的持久化上下文
	 * 
	 * @return 相应的EntityManager, 或{@code null}
	 */
	protected EntityManager getPersistenceContext(String unitName, boolean extended) {
		Map<String, String> contexts = (extended ? this.extendedPersistenceContexts : this.persistenceContexts);
		if (contexts != null) {
			String unitNameForLookup = (unitName != null ? unitName : "");
			if ("".equals(unitNameForLookup)) {
				unitNameForLookup = this.defaultPersistenceUnitName;
			}
			String jndiName = contexts.get(unitNameForLookup);
			if (jndiName == null && "".equals(unitNameForLookup) && contexts.size() == 1) {
				jndiName = contexts.values().iterator().next();
			}
			if (jndiName != null) {
				try {
					return lookup(jndiName, EntityManager.class);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Could not obtain EntityManager [" + jndiName + "] from JNDI", ex);
				}
			}
		}
		return null;
	}

	/**
	 * 在当前Spring应用程序上下文中查找具有给定名称的EntityManagerFactory,
	 * 如果未指定单元名称, 则返回单个默认EntityManagerFactory.
	 * 
	 * @param unitName 持久化单元的名称 (可能是{@code null}或空)
	 * @param requestingBeanName 请求bean的名称
	 * 
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException 如果上下文中没有这样的EntityManagerFactory
	 */
	protected EntityManagerFactory findEntityManagerFactory(String unitName, String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		if (this.beanFactory == null) {
			throw new IllegalStateException("ListableBeanFactory required for EntityManagerFactory bean lookup");
		}
		String unitNameForLookup = (unitName != null ? unitName : "");
		if ("".equals(unitNameForLookup)) {
			unitNameForLookup = this.defaultPersistenceUnitName;
		}
		if (!"".equals(unitNameForLookup)) {
			return findNamedEntityManagerFactory(unitNameForLookup, requestingBeanName);
		}
		else {
			return findDefaultEntityManagerFactory(requestingBeanName);
		}
	}

	/**
	 * 在当前的Spring应用程序上下文中查找具有给定名称的EntityManagerFactory.
	 * 
	 * @param unitName 持久化单元的名称 (never empty)
	 * @param requestingBeanName 请求bean的名称
	 * 
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException 如果上下文中没有这样的EntityManagerFactory
	 */
	protected EntityManagerFactory findNamedEntityManagerFactory(String unitName, String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		EntityManagerFactory emf = EntityManagerFactoryUtils.findEntityManagerFactory(this.beanFactory, unitName);
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(unitName, requestingBeanName);
		}
		return emf;
	}

	/**
	 * 在Spring应用程序上下文中查找单个默认的EntityManagerFactory.
	 * 
	 * @return 默认的EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException 如果上下文中没有单个EntityManagerFactory
	 */
	protected EntityManagerFactory findDefaultEntityManagerFactory(String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			// 具有依赖注册的花式变体
			ConfigurableListableBeanFactory clbf = (ConfigurableListableBeanFactory) this.beanFactory;
			NamedBeanHolder<EntityManagerFactory> emfHolder = clbf.resolveNamedBean(EntityManagerFactory.class);
			clbf.registerDependentBean(emfHolder.getBeanName(), requestingBeanName);
			return emfHolder.getBeanInstance();
		}
		else {
			// 普通变体: 只需找到一个默认bean
			return this.beanFactory.getBean(EntityManagerFactory.class);
		}
	}

	/**
	 * 按名称对给定资源执行JNDI查找.
	 * <p>当为特定持久化单元映射JNDI名称时, 调用EntityManagerFactory和EntityManager查找.
	 * 
	 * @param jndiName 要查找的JNDI名称
	 * @param requiredType 所需的对象类型
	 * 
	 * @return 获取的对象
	 * @throws Exception 如果JNDI查找失败
	 */
	protected <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
		return new LocatorDelegate().lookup(jndiName, requiredType);
	}


	/**
	 * 单独的内部类以隔离JNDI API依赖项 (与Google App Engine的API白名单兼容).
	 */
	private class LocatorDelegate {

		public <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
			JndiLocatorDelegate locator = new JndiLocatorDelegate();
			if (jndiEnvironment instanceof JndiTemplate) {
				locator.setJndiTemplate((JndiTemplate) jndiEnvironment);
			}
			else if (jndiEnvironment instanceof Properties) {
				locator.setJndiEnvironment((Properties) jndiEnvironment);
			}
			else if (jndiEnvironment != null) {
				throw new IllegalStateException("Illegal 'jndiEnvironment' type: " + jndiEnvironment.getClass());
			}
			locator.setResourceRef(resourceRef);
			return locator.lookup(jndiName, requiredType);
		}
	}


	/**
	 * 表示注解字段或setter方法的注入信息的类.
	 */
	private class PersistenceElement extends InjectionMetadata.InjectedElement {

		private final String unitName;

		private PersistenceContextType type;

		private boolean synchronizedWithTransaction = false;

		private Properties properties;

		public PersistenceElement(Member member, AnnotatedElement ae, PropertyDescriptor pd) {
			super(member, pd);
			PersistenceContext pc = ae.getAnnotation(PersistenceContext.class);
			PersistenceUnit pu = ae.getAnnotation(PersistenceUnit.class);
			Class<?> resourceType = EntityManager.class;
			if (pc != null) {
				if (pu != null) {
					throw new IllegalStateException("Member may only be annotated with either " +
							"@PersistenceContext or @PersistenceUnit, not both: " + member);
				}
				Properties properties = null;
				PersistenceProperty[] pps = pc.properties();
				if (!ObjectUtils.isEmpty(pps)) {
					properties = new Properties();
					for (PersistenceProperty pp : pps) {
						properties.setProperty(pp.name(), pp.value());
					}
				}
				this.unitName = pc.unitName();
				this.type = pc.type();
				this.synchronizedWithTransaction = (synchronizationAttribute == null ||
						"SYNCHRONIZED".equals(ReflectionUtils.invokeMethod(synchronizationAttribute, pc).toString()));
				this.properties = properties;
			}
			else {
				resourceType = EntityManagerFactory.class;
				this.unitName = pu.unitName();
			}
			checkResourceType(resourceType);
		}

		/**
		 * 根据应用程序上下文解析对象.
		 */
		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			// 解析为EntityManagerFactory或EntityManager.
			if (this.type != null) {
				return (this.type == PersistenceContextType.EXTENDED ?
						resolveExtendedEntityManager(target, requestingBeanName) :
						resolveEntityManager(requestingBeanName));
			}
			else {
				// 好的, 所以需要一个EntityManagerFactory...
				return resolveEntityManagerFactory(requestingBeanName);
			}
		}

		private EntityManagerFactory resolveEntityManagerFactory(String requestingBeanName) {
			// 从JNDI获取EntityManagerFactory?
			EntityManagerFactory emf = getPersistenceUnit(this.unitName);
			if (emf == null) {
				// 需要搜索EntityManagerFactory bean.
				emf = findEntityManagerFactory(this.unitName, requestingBeanName);
			}
			return emf;
		}

		private EntityManager resolveEntityManager(String requestingBeanName) {
			// 从JNDI获取EntityManager引用?
			EntityManager em = getPersistenceContext(this.unitName, false);
			if (em == null) {
				// 找不到预先构建的EntityManager -> 基于工厂构建一个.
				// 从JNDI获取EntityManagerFactory?
				EntityManagerFactory emf = getPersistenceUnit(this.unitName);
				if (emf == null) {
					// 需要搜索EntityManagerFactory bean.
					emf = findEntityManagerFactory(this.unitName, requestingBeanName);
				}
				// 注入共享事务EntityManager代理.
				if (emf instanceof EntityManagerFactoryInfo &&
						((EntityManagerFactoryInfo) emf).getEntityManagerInterface() != null) {
					// 根据信息的供应商特定类型创建EntityManager (可能比字段的类型更具体).
					em = SharedEntityManagerCreator.createSharedEntityManager(
							emf, this.properties, this.synchronizedWithTransaction);
				}
				else {
					// 根据字段的类型创建EntityManager.
					em = SharedEntityManagerCreator.createSharedEntityManager(
							emf, this.properties, this.synchronizedWithTransaction, getResourceType());
				}
			}
			return em;
		}

		private EntityManager resolveExtendedEntityManager(Object target, String requestingBeanName) {
			// 从JNDI获取EntityManager引用?
			EntityManager em = getPersistenceContext(this.unitName, true);
			if (em == null) {
				// 找不到预先构建的EntityManager -> 基于工厂构建一个.
				// 从JNDI获取EntityManagerFactory?
				EntityManagerFactory emf = getPersistenceUnit(this.unitName);
				if (emf == null) {
					// 需要搜索EntityManagerFactory bean.
					emf = findEntityManagerFactory(this.unitName, requestingBeanName);
				}
				// 注入容器管理的扩展EntityManager.
				em = ExtendedEntityManagerCreator.createContainerManagedEntityManager(
						emf, this.properties, this.synchronizedWithTransaction);
			}
			if (em instanceof EntityManagerProxy && beanFactory != null &&
					beanFactory.containsBean(requestingBeanName) && !beanFactory.isPrototype(requestingBeanName)) {
				extendedEntityManagersToClose.put(target, ((EntityManagerProxy) em).getTargetEntityManager());
			}
			return em;
		}
	}
}
