package org.springframework.jndi.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.ResolvableType;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.jndi.TypeMismatchNamingException;

/**
 * 基于JNDI的Spring的{@link org.springframework.beans.factory.BeanFactory}接口的简单实现.
 * 不支持枚举bean定义, 因此不实现{@link org.springframework.beans.factory.ListableBeanFactory}接口.
 *
 * <p>此工厂将给定的bean名称解析为J2EE应用程序的 "java:comp/env/"命名空间中的JNDI名称.
 * 它为所有获取的对象缓存已解析的类型, 并且可选地还缓存可共享对象 (如果它们被明确标记为{@link #addShareableResource 可共享资源}.
 *
 * <p>该工厂的主要目的是与Spring的
 * {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}结合使用,
 * 配置为"resourceFactory", 用于将{@code @Resource}注解解析为没有中间bean定义的JNDI对象.
 * 当然, 它也可以用于类似的查找场景, 特别是如果需要BeanFactory样式的类型检查.
 */
public class SimpleJndiBeanFactory extends JndiLocatorSupport implements BeanFactory {

	/** 已知可共享的资源的JNDI名称, i.e. 可以被缓存 */
	private final Set<String> shareableResources = new HashSet<String>();

	/** 可共享的单例对象的缓存: bean name --> bean instance */
	private final Map<String, Object> singletonObjects = new HashMap<String, Object>();

	/** 不可共享的资源类型的缓存: bean name --> bean type */
	private final Map<String, Class<?>> resourceTypes = new HashMap<String, Class<?>>();


	public SimpleJndiBeanFactory() {
		setResourceRef(true);
	}


	/**
	 * 添加可共享JNDI资源的名称, 该工厂一旦获得就可以缓存该资源.
	 * 
	 * @param shareableResource JNDI名称(通常在"java:comp/env/"命名空间内)
	 */
	public void addShareableResource(String shareableResource) {
		this.shareableResources.add(shareableResource);
	}

	/**
	 * 设置可共享JNDI资源的名称列表, 该工厂一旦获得就可以缓存该资源.
	 * 
	 * @param shareableResources JNDI名称(通常在"java:comp/env/"命名空间内)
	 */
	public void setShareableResources(String... shareableResources) {
		this.shareableResources.addAll(Arrays.asList(shareableResources));
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------


	@Override
	public Object getBean(String name) throws BeansException {
		return getBean(name, Object.class);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		try {
			if (isSingleton(name)) {
				return doGetSingleton(name, requiredType);
			}
			else {
				return lookup(name, requiredType);
			}
		}
		catch (NameNotFoundException ex) {
			throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
		}
		catch (TypeMismatchNamingException ex) {
			throw new BeanNotOfRequiredTypeException(name, ex.getRequiredType(), ex.getActualType());
		}
		catch (NamingException ex) {
			throw new BeanDefinitionStoreException("JNDI environment", name, "JNDI lookup failed", ex);
		}
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(requiredType.getSimpleName(), requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		if (args != null) {
			throw new UnsupportedOperationException(
					"SimpleJndiBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(name);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		if (args != null) {
			throw new UnsupportedOperationException(
					"SimpleJndiBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		if (this.singletonObjects.containsKey(name) || this.resourceTypes.containsKey(name)) {
			return true;
		}
		try {
			doGetType(name);
			return true;
		}
		catch (NamingException ex) {
			return false;
		}
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return this.shareableResources.contains(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return !this.shareableResources.contains(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (type != null && typeToMatch.isAssignableFrom(type));
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		try {
			return doGetType(name);
		}
		catch (NameNotFoundException ex) {
			throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
		}
		catch (NamingException ex) {
			return null;
		}
	}

	@Override
	public String[] getAliases(String name) {
		return new String[0];
	}


	@SuppressWarnings("unchecked")
	private <T> T doGetSingleton(String name, Class<T> requiredType) throws NamingException {
		synchronized (this.singletonObjects) {
			if (this.singletonObjects.containsKey(name)) {
				Object jndiObject = this.singletonObjects.get(name);
				if (requiredType != null && !requiredType.isInstance(jndiObject)) {
					throw new TypeMismatchNamingException(
							convertJndiName(name), requiredType, (jndiObject != null ? jndiObject.getClass() : null));
				}
				return (T) jndiObject;
			}
			T jndiObject = lookup(name, requiredType);
			this.singletonObjects.put(name, jndiObject);
			return jndiObject;
		}
	}

	private Class<?> doGetType(String name) throws NamingException {
		if (isSingleton(name)) {
			Object jndiObject = doGetSingleton(name, null);
			return (jndiObject != null ? jndiObject.getClass() : null);
		}
		else {
			synchronized (this.resourceTypes) {
				if (this.resourceTypes.containsKey(name)) {
					return this.resourceTypes.get(name);
				}
				else {
					Object jndiObject = lookup(name, null);
					Class<?> type = (jndiObject != null ? jndiObject.getClass() : null);
					this.resourceTypes.put(name, type);
					return type;
				}
			}
		}
	}

}
