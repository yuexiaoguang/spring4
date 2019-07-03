package org.springframework.test.web.servlet.setup;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DelegatingMessageSource;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * 存根WebApplicationContext, 它接受对象实例的注册.
 *
 * <p>由于已在外部实例化和初始化已注册的对象实例, 因此没有连接, bean初始化,
 * 生命周期事件, 而且没有通常与{@link ApplicationContext}管理的bean关联的预处理和后处理挂钩.
 * 只需简单查找{@link StaticListableBeanFactory}.
 */
class StubWebApplicationContext implements WebApplicationContext {

	private final ServletContext servletContext;

	private final StubBeanFactory beanFactory = new StubBeanFactory();

	private final String id = ObjectUtils.identityToString(this);

	private final String displayName = ObjectUtils.identityToString(this);

	private final long startupDate = System.currentTimeMillis();

	private final Environment environment = new StandardEnvironment();

	private final MessageSource messageSource = new DelegatingMessageSource();

	private final ResourcePatternResolver resourcePatternResolver;


	public StubWebApplicationContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		this.resourcePatternResolver = new ServletContextResourcePatternResolver(servletContext);
	}


	/**
	 * 返回可以初始化{@link ApplicationContextAware} bean的实例.
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return this.beanFactory;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	@Override
	public ApplicationContext getParent() {
		return null;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment ;
	}

	public void addBean(String name, Object bean) {
		this.beanFactory.addBean(name, bean);
	}

	public void addBeans(List<?> beans) {
		if (beans == null) {
			return;
		}
		for (Object bean : beans) {
			String name = bean.getClass().getName() + "#" +  ObjectUtils.getIdentityHexString(bean);
			this.beanFactory.addBean(name, bean);
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		return this.beanFactory.getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return this.beanFactory.getBean(name, requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return this.beanFactory.getBean(requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		return this.beanFactory.getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		return this.beanFactory.getBean(requiredType, args);
	}

	@Override
	public boolean containsBean(String name) {
		return this.beanFactory.containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return this.beanFactory.isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return this.beanFactory.isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		return this.beanFactory.isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		return this.beanFactory.isTypeMatch(name, typeToMatch);
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return this.beanFactory.getType(name);
	}

	@Override
	public String[] getAliases(String name) {
		return this.beanFactory.getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return this.beanFactory.containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beanFactory.getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return this.beanFactory.getBeanDefinitionNames();
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		return this.beanFactory.getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type) {
		return this.beanFactory.getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		return this.beanFactory.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return this.beanFactory.getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		return this.beanFactory.getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		return this.beanFactory.getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		return this.beanFactory.getBeansWithAnnotation(annotationType);
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException{

		return this.beanFactory.findAnnotationOnBean(beanName, annotationType);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public BeanFactory getParentBeanFactory() {
		return null;
	}

	@Override
	public boolean containsLocalBean(String name) {
		return this.beanFactory.containsBean(name);
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	@Override
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		return this.messageSource.getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, locale);
	}


	//---------------------------------------------------------------------
	// Implementation of ResourceLoader interface
	//---------------------------------------------------------------------

	@Override
	public ClassLoader getClassLoader() {
		return ClassUtils.getDefaultClassLoader();
	}

	@Override
	public Resource getResource(String location) {
		return this.resourcePatternResolver.getResource(location);
	}


	//---------------------------------------------------------------------
	// Other
	//---------------------------------------------------------------------

	@Override
	public void publishEvent(ApplicationEvent event) {
	}

	@Override
	public void publishEvent(Object event) {
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	/**
	 * StaticListableBeanFactory的扩展, 它实现了AutowireCapableBeanFactory,
	 * 以便允许{@link ApplicationContextAware}单例的bean初始化.
	 */
	private class StubBeanFactory extends StaticListableBeanFactory implements AutowireCapableBeanFactory {

		@Override
		public Object initializeBean(Object existingBean, String beanName) throws BeansException {
			if (existingBean instanceof ApplicationContextAware) {
				((ApplicationContextAware) existingBean).setApplicationContext(StubWebApplicationContext.this);
			}
			return existingBean;
		}

		@Override
		public <T> T createBean(Class<T> beanClass) {
			return BeanUtils.instantiateClass(beanClass);
		}

		@Override
		public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
			return BeanUtils.instantiateClass(beanClass);
		}

		@Override
		public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
			return BeanUtils.instantiateClass(beanClass);
		}

		@Override
		public void autowireBean(Object existingBean) throws BeansException {
		}

		@Override
		public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) {
		}

		@Override
		public Object configureBean(Object existingBean, String beanName) {
			return existingBean;
		}

		@Override
		public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
			throw new UnsupportedOperationException("Dependency resolution not supported");
		}

		@Override
		public Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName) {
			throw new UnsupportedOperationException("Dependency resolution not supported");
		}

		@Override
		public Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName,
				Set<String> autowiredBeanNames, TypeConverter typeConverter) {
			throw new UnsupportedOperationException("Dependency resolution not supported");
		}

		@Override
		public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
		}

		@Override
		public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) {
			return existingBean;
		}

		@Override
		public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) {
			return existingBean;
		}

		@Override
		public void destroyBean(Object existingBean) {
		}
	}

}
