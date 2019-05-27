package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * 内部类, 用于为Java类缓存JavaBeans {@link java.beans.PropertyDescriptor}信息. 不适合应用程序代码直接使用.
 *
 * <p>在应用程序的ClassLoader中自己缓存描述符是必需的, 而不是依赖于JDK的系统范围的BeanInfo缓存 (为了避免ClassLoader关闭时出现泄漏).
 *
 * <p>信息以静态方式缓存, 所以不需要为我们操作的每个JavaBean创建这个类的新对象.
 * 于是, 此类实现工厂设计模式, 使用私有构造函数和静态 {@link #forClass(Class)}工厂方法来获取实例.
 *
 * <p>请注意, 缓存可以有效地工作, 需要满足一些先决条件:
 * 更喜欢Spring jar与应用程序类位于同一ClassLoader中的安排, 在应用程序的生命周期中的任何情况下都允许清理缓存.
 * 对于Web应用程序, 考虑在多个ClassLoader布局的情况下在{@code web.xml}中声明本地
 * {@link org.springframework.web.util.IntrospectorCleanupListener}, 这也将允许有效的缓存.
 *
 * <p>如果没有设置清理监听器的非干净的ClassLoader安排, 这个类将回退到一个基于弱引用的缓存模型, 
 * 每次垃圾收集器删除它们时, 都会重新创建很多请求的条目.
 * 在这种情况下, 考虑 {@link #IGNORE_BEANINFO_PROPERTY_NAME}系统属性.
 */
public class CachedIntrospectionResults {

	/**
	 * 在调用JavaBeans {@link Introspector}时, 指示Spring使用{@link Introspector＃IGNORE_ALL_BEANINFO}模式的系统属性:
	 * "spring.beaninfo.ignore", 为“true”跳过搜索{@code BeanInfo}类 (通常用于首先没有为应用程序中的bean定义此类的情况).
	 * <p>默认是 "false", 考虑所有{@code BeanInfo}元数据类, 比如标准 {@link Introspector#getBeanInfo(Class)}调用.
	 * 如果遇到不存在的{@code BeanInfo}类的重复ClassLoader访问, 考虑将此标志切换为“true”, 如果这种访问在启动或延迟加载时很昂贵.
	 * <p>请注意, 此类效果还可能表示缓存无法有效运行的情况:
	 * 更喜欢Spring jar与应用程序类位于同一ClassLoader中的安排, 在应用程序的生命周期中的任何情况下都允许清理缓存.
	 * 对于Web应用程序, 考虑在多个ClassLoader布局的情况下在{@code web.xml}中声明本地
	 * {@link org.springframework.web.util.IntrospectorCleanupListener}, 这也将允许有效的缓存.
	 */
	public static final String IGNORE_BEANINFO_PROPERTY_NAME = "spring.beaninfo.ignore";


	private static final boolean shouldIntrospectorIgnoreBeaninfoClasses =
			SpringProperties.getFlag(IGNORE_BEANINFO_PROPERTY_NAME);

	/** Stores the BeanInfoFactory instances */
	private static List<BeanInfoFactory> beanInfoFactories = SpringFactoriesLoader.loadFactories(
			BeanInfoFactory.class, CachedIntrospectionResults.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);

	/**
	 * 这个CachedIntrospectionResults类将始终接受来自acceptedClassLoaders的类, 即使这些类不符合缓存安全性.
	 */
	static final Set<ClassLoader> acceptedClassLoaders =
			Collections.newSetFromMap(new ConcurrentHashMap<ClassLoader, Boolean>(16));

	/**
	 * 由包含CachedIntrospectionResults的类作为Key, 强引用.
	 * 用于缓存安全的bean类.
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache =
			new ConcurrentHashMap<Class<?>, CachedIntrospectionResults>(64);

	/**
	 * 由包含CachedIntrospectionResults的类作为Key, 软引用.
	 * 用于非缓存安全的bean类.
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache =
			new ConcurrentReferenceHashMap<Class<?>, CachedIntrospectionResults>(64);


	/**
	 * 接受给定的ClassLoader作为缓存安全, 即使它的类在此CachedIntrospectionResults类中不符合缓存安全性.
	 * <p>此配置方法仅适用于Spring类驻留在“公共”ClassLoader中的情况 (e.g. 系统 ClassLoader), 其生命周期未与应用程序耦合.
	 * 在这种情况下, 默认情况下, CachedIntrospectionResults不会缓存任何应用程序的类, 因为它们会在公共ClassLoader中创建泄漏.
	 * <p>应用程序启动时的{@code acceptClassLoader}调用, 应在应用程序关闭时与{@link #clearClassLoader}调用配对.
	 * 
	 * @param classLoader 要接受的ClassLoader
	 */
	public static void acceptClassLoader(ClassLoader classLoader) {
		if (classLoader != null) {
			acceptedClassLoaders.add(classLoader);
		}
	}

	/**
	 * 清除给定ClassLoader的反射缓存, 删除该ClassLoader下所有类的反射结果,
	 * 并从接受列表中删除ClassLoader（及其子项）.
	 * 
	 * @param classLoader 用于清除缓存的ClassLoader
	 */
	public static void clearClassLoader(ClassLoader classLoader) {
		for (Iterator<ClassLoader> it = acceptedClassLoaders.iterator(); it.hasNext();) {
			ClassLoader registeredLoader = it.next();
			if (isUnderneathClassLoader(registeredLoader, classLoader)) {
				it.remove();
			}
		}
		for (Iterator<Class<?>> it = strongClassCache.keySet().iterator(); it.hasNext();) {
			Class<?> beanClass = it.next();
			if (isUnderneathClassLoader(beanClass.getClassLoader(), classLoader)) {
				it.remove();
			}
		}
		for (Iterator<Class<?>> it = softClassCache.keySet().iterator(); it.hasNext();) {
			Class<?> beanClass = it.next();
			if (isUnderneathClassLoader(beanClass.getClassLoader(), classLoader)) {
				it.remove();
			}
		}
	}

	/**
	 * 为给定的bean类创建CachedIntrospectionResults.
	 * 
	 * @param beanClass 要分析的bean类
	 * 
	 * @return 对应的CachedIntrospectionResults
	 * @throws BeansException 反射失败
	 */
	@SuppressWarnings("unchecked")
	static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
		CachedIntrospectionResults results = strongClassCache.get(beanClass);
		if (results != null) {
			return results;
		}
		results = softClassCache.get(beanClass);
		if (results != null) {
			return results;
		}

		results = new CachedIntrospectionResults(beanClass);
		ConcurrentMap<Class<?>, CachedIntrospectionResults> classCacheToUse;

		if (ClassUtils.isCacheSafe(beanClass, CachedIntrospectionResults.class.getClassLoader()) ||
				isClassLoaderAccepted(beanClass.getClassLoader())) {
			classCacheToUse = strongClassCache;
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Not strongly caching class [" + beanClass.getName() + "] because it is not cache-safe");
			}
			classCacheToUse = softClassCache;
		}

		CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
		return (existing != null ? existing : results);
	}

	/**
	 * 检查此CachedIntrospectionResults类是否配置为接受给定的ClassLoader.
	 * 
	 * @param classLoader 要检查的ClassLoader
	 * 
	 * @return 是否接受给定的ClassLoader
	 */
	private static boolean isClassLoaderAccepted(ClassLoader classLoader) {
		for (ClassLoader acceptedLoader : acceptedClassLoaders) {
			if (isUnderneathClassLoader(classLoader, acceptedLoader)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的ClassLoader是否在给定parent下面, 即, parent是否在候选人的等级内.
	 * 
	 * @param candidate 要检查的候选ClassLoader
	 * @param parent 要检查的父级ClassLoader
	 */
	private static boolean isUnderneathClassLoader(ClassLoader candidate, ClassLoader parent) {
		if (candidate == parent) {
			return true;
		}
		if (candidate == null) {
			return false;
		}
		ClassLoader classLoaderToCheck = candidate;
		while (classLoaderToCheck != null) {
			classLoaderToCheck = classLoaderToCheck.getParent();
			if (classLoaderToCheck == parent) {
				return true;
			}
		}
		return false;
	}


	/** 反射bean类的BeanInfo对象 */
	private final BeanInfo beanInfo;

	/** 属性名作为Key */
	private final Map<String, PropertyDescriptor> propertyDescriptorCache;

	/** PropertyDescriptor作为Key */
	private final ConcurrentMap<PropertyDescriptor, TypeDescriptor> typeDescriptorCache;


	/**
	 * 为给定的类创建一个新的CachedIntrospectionResults实例.
	 * 
	 * @param beanClass 要分析的bean类
	 * 
	 * @throws BeansException 反射失败
	 */
	private CachedIntrospectionResults(Class<?> beanClass) throws BeansException {
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Getting BeanInfo for class [" + beanClass.getName() + "]");
			}

			BeanInfo beanInfo = null;
			for (BeanInfoFactory beanInfoFactory : beanInfoFactories) {
				beanInfo = beanInfoFactory.getBeanInfo(beanClass);
				if (beanInfo != null) {
					break;
				}
			}
			if (beanInfo == null) {
				// 如果没有工厂支持该类, 使用默认的
				beanInfo = (shouldIntrospectorIgnoreBeaninfoClasses ?
						Introspector.getBeanInfo(beanClass, Introspector.IGNORE_ALL_BEANINFO) :
						Introspector.getBeanInfo(beanClass));
			}
			this.beanInfo = beanInfo;

			if (logger.isTraceEnabled()) {
				logger.trace("Caching PropertyDescriptors for class [" + beanClass.getName() + "]");
			}
			this.propertyDescriptorCache = new LinkedHashMap<String, PropertyDescriptor>();

			// 这个调用很慢，所以我们做了一次.
			PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (Class.class == beanClass &&
						("classLoader".equals(pd.getName()) ||  "protectionDomain".equals(pd.getName()))) {
					// Ignore Class.getClassLoader() and getProtectionDomain() methods - nobody needs to bind to those
					continue;
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Found bean property '" + pd.getName() + "'" +
							(pd.getPropertyType() != null ? " of type [" + pd.getPropertyType().getName() + "]" : "") +
							(pd.getPropertyEditorClass() != null ?
									"; editor [" + pd.getPropertyEditorClass().getName() + "]" : ""));
				}
				pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
				this.propertyDescriptorCache.put(pd.getName(), pd);
			}

			// 明确检查setter/getter方法的实现接口, 特别是对于Java 8默认方法...
			Class<?> clazz = beanClass;
			while (clazz != null) {
				Class<?>[] ifcs = clazz.getInterfaces();
				for (Class<?> ifc : ifcs) {
					BeanInfo ifcInfo = Introspector.getBeanInfo(ifc, Introspector.IGNORE_ALL_BEANINFO);
					PropertyDescriptor[] ifcPds = ifcInfo.getPropertyDescriptors();
					for (PropertyDescriptor pd : ifcPds) {
						if (!this.propertyDescriptorCache.containsKey(pd.getName())) {
							pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
							this.propertyDescriptorCache.put(pd.getName(), pd);
						}
					}
				}
				clazz = clazz.getSuperclass();
			}

			this.typeDescriptorCache = new ConcurrentReferenceHashMap<PropertyDescriptor, TypeDescriptor>();
		}
		catch (IntrospectionException ex) {
			throw new FatalBeanException("Failed to obtain BeanInfo for class [" + beanClass.getName() + "]", ex);
		}
	}

	BeanInfo getBeanInfo() {
		return this.beanInfo;
	}

	Class<?> getBeanClass() {
		return this.beanInfo.getBeanDescriptor().getBeanClass();
	}

	PropertyDescriptor getPropertyDescriptor(String name) {
		PropertyDescriptor pd = this.propertyDescriptorCache.get(name);
		if (pd == null && StringUtils.hasLength(name)) {
			// 与Property相同的宽松后备检查...
			pd = this.propertyDescriptorCache.get(StringUtils.uncapitalize(name));
			if (pd == null) {
				pd = this.propertyDescriptorCache.get(StringUtils.capitalize(name));
			}
		}
		return (pd == null || pd instanceof GenericTypeAwarePropertyDescriptor ? pd :
				buildGenericTypeAwarePropertyDescriptor(getBeanClass(), pd));
	}

	PropertyDescriptor[] getPropertyDescriptors() {
		PropertyDescriptor[] pds = new PropertyDescriptor[this.propertyDescriptorCache.size()];
		int i = 0;
		for (PropertyDescriptor pd : this.propertyDescriptorCache.values()) {
			pds[i] = (pd instanceof GenericTypeAwarePropertyDescriptor ? pd :
					buildGenericTypeAwarePropertyDescriptor(getBeanClass(), pd));
			i++;
		}
		return pds;
	}

	private PropertyDescriptor buildGenericTypeAwarePropertyDescriptor(Class<?> beanClass, PropertyDescriptor pd) {
		try {
			return new GenericTypeAwarePropertyDescriptor(beanClass, pd.getName(), pd.getReadMethod(),
					pd.getWriteMethod(), pd.getPropertyEditorClass());
		}
		catch (IntrospectionException ex) {
			throw new FatalBeanException("Failed to re-introspect class [" + beanClass.getName() + "]", ex);
		}
	}

	TypeDescriptor addTypeDescriptor(PropertyDescriptor pd, TypeDescriptor td) {
		TypeDescriptor existing = this.typeDescriptorCache.putIfAbsent(pd, td);
		return (existing != null ? existing : td);
	}

	TypeDescriptor getTypeDescriptor(PropertyDescriptor pd) {
		return this.typeDescriptorCache.get(pd);
	}
}
