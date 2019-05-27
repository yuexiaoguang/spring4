package org.springframework.beans.factory.support;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 静态{@link org.springframework.beans.factory.BeanFactory}实现, 允许以编程方式注册现有的单例实例.
 * 不支持原型bean或别名.
 *
 * <p>作为{@link org.springframework.beans.factory.ListableBeanFactory}接口的简单实现的示例,
 * 管理现有的bean实例, 而不是基于bean定义创建新的实例, 而不实现任何扩展的SPI接口
 * (such as {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}).
 *
 * <p>对于基于bean定义的完整工厂, 请查看 {@link DefaultListableBeanFactory}.
 */
public class StaticListableBeanFactory implements ListableBeanFactory {

	/** 从bean名称到bean实例的Map */
	private final Map<String, Object> beans;


	/**
	 * 创建一个常规 {@code StaticListableBeanFactory}, 通过{@link #addBean}调用填充单例bean实例.
	 */
	public StaticListableBeanFactory() {
		this.beans = new LinkedHashMap<String, Object>();
	}

	/**
	 * <p>请注意, 给定的{@code Map}可以预先填充bean;
	 * 或者新的, 仍允许通过{@link #addBean}注册bean;
	 * 或{@link java.util.Collections#emptyMap()}用于对bean的空集合强制执行操作的虚拟工厂.
	 * 
	 * @param beans 用于保存此工厂bean的{@code Map}, 其中bean名称字符串作为Key, 相应的单例对象作为值
	 */
	public StaticListableBeanFactory(Map<String, Object> beans) {
		Assert.notNull(beans, "Beans Map must not be null");
		this.beans = beans;
	}


	/**
	 * 添加一个新的单例bean. 将覆盖给定名称的任何现有实例.
	 * 
	 * @param name bean的名称
	 * @param bean bean的实例
	 */
	public void addBean(String name, Object bean) {
		this.beans.put(name, bean);
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = this.beans.get(beanName);

		if (bean == null) {
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}

		// 如果bean不是工厂, 请不要让调用代码尝试取消引用bean工厂
		if (BeanFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, bean.getClass());
		}

		if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			try {
				return ((FactoryBean<?>) bean).getObject();
			}
			catch (Exception ex) {
				throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
			}
		}
		else {
			return bean;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		Object bean = getBean(name);
		if (requiredType != null && !requiredType.isInstance(bean)) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
		}
		return (T) bean;
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(name);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		String[] beanNames = getBeanNamesForType(requiredType);
		if (beanNames.length == 1) {
			return getBean(beanNames[0], requiredType);
		}
		else if (beanNames.length > 1) {
			throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
		}
		else {
			throw new NoSuchBeanDefinitionException(requiredType);
		}
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		Object bean = getBean(name);
		// 如果是FactoryBean, 则返回已创建的对象的单例状态.
		return (bean instanceof FactoryBean && ((FactoryBean<?>) bean).isSingleton());
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		Object bean = getBean(name);
		// 如果是FactoryBean, 返回创建的对象的原型状态.
		return ((bean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) bean).isPrototype()) ||
				(bean instanceof FactoryBean && !((FactoryBean<?>) bean).isSingleton()));
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
		String beanName = BeanFactoryUtils.transformedBeanName(name);

		Object bean = this.beans.get(beanName);
		if (bean == null) {
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}

		if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			// 如果它是FactoryBean, 我们想要查看它创建的内容, 而不是工厂类.
			return ((FactoryBean<?>) bean).getObjectType();
		}
		return bean.getClass();
	}

	@Override
	public String[] getAliases(String name) {
		return new String[0];
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beans.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return StringUtils.toStringArray(this.beans.keySet());
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		boolean isFactoryType = false;
		if (type != null) {
			Class<?> resolved = type.resolve();
			if (resolved != null && FactoryBean.class.isAssignableFrom(resolved)) {
				isFactoryType = true;
			}
		}
		List<String> matches = new ArrayList<String>();
		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			String name = entry.getKey();
			Object beanInstance = entry.getValue();
			if (beanInstance instanceof FactoryBean && !isFactoryType) {
				Class<?> objectType = ((FactoryBean<?>) beanInstance).getObjectType();
				if (objectType != null && (type == null || type.isAssignableFrom(objectType))) {
					matches.add(name);
				}
			}
			else {
				if (type == null || type.isInstance(beanInstance)) {
					matches.add(name);
				}
			}
		}
		return StringUtils.toStringArray(matches);
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type) {
		return getBeanNamesForType(ResolvableType.forClass(type));
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		return getBeanNamesForType(ResolvableType.forClass(type));
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
		Map<String, T> matches = new LinkedHashMap<String, T>();

		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			String beanName = entry.getKey();
			Object beanInstance = entry.getValue();
			// Is bean a FactoryBean?
			if (beanInstance instanceof FactoryBean && !isFactoryType) {
				// Match object created by FactoryBean.
				FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
				Class<?> objectType = factory.getObjectType();
				if ((includeNonSingletons || factory.isSingleton()) &&
						objectType != null && (type == null || type.isAssignableFrom(objectType))) {
					matches.put(beanName, getBean(beanName, type));
				}
			}
			else {
				if (type == null || type.isInstance(beanInstance)) {
					// 如果要匹配的类型是FactoryBean, 则返回FactoryBean本身.
					// 否则返回bean实例.
					if (isFactoryType) {
						beanName = FACTORY_BEAN_PREFIX + beanName;
					}
					matches.put(beanName, (T) beanInstance);
				}
			}
		}
		return matches;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		List<String> results = new ArrayList<String>();
		for (String beanName : this.beans.keySet()) {
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.add(beanName);
			}
		}
		return StringUtils.toStringArray(results);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		Map<String, Object> results = new LinkedHashMap<String, Object>();
		for (String beanName : this.beans.keySet()) {
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.put(beanName, getBean(beanName));
			}
		}
		return results;
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException{

		Class<?> beanType = getType(beanName);
		return (beanType != null ? AnnotationUtils.findAnnotation(beanType, annotationType) : null);
	}

}
