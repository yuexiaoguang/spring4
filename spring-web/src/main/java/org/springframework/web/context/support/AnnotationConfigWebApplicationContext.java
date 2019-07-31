package org.springframework.web.context.support;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;

/**
 * {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}实现,
 * 它接受带注解的类作为输入 - 特别是带
 * {@link org.springframework.context.annotation.Configuration @Configuration}注解的类,
 * 也包括普通的{@link org.springframework.stereotype.Component @Component}类
 * 和使用{@code javax.inject}注解的JSR-330兼容类.
 * 允许逐个注册类 (将类名指定为配置位置) 以及类路径扫描 (将底层包指定为配置位置).
 *
 * <p>这基本上等同于Web环境的
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext AnnotationConfigApplicationContext}.
 *
 * <p>要使用此应用程序上下文, 必须将ContextLoader的
 * {@linkplain ContextLoader#CONTEXT_CLASS_PARAM "contextClass"} context-param
 * 和/或FrameworkServlet的"contextClass" init-param设置为此类的完全限定名称.
 *
 * <p>从Spring 3.1开始, 当使用新的{@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}
 * 代码替代{@code web.xml}时,
 * 也可以直接实例化该类并将其注入到Spring的{@code DispatcherServlet}或{@code ContextLoaderListener}中.
 * 有关详细信息和用法示例, 请参阅其Javadoc.
 *
 * <p>与{@link XmlWebApplicationContext}不同, 不假定默认配置类位置.
 * 相反, 需要为FrameworkServlet的{@link ContextLoader}和/或"contextConfigLocation" init-param
 * 设置{@linkplain ContextLoader#CONFIG_LOCATION_PARAM "contextConfigLocation"} context-param.
 * param-value可以包含完全限定的类名和基本包以扫描组件.
 * 有关如何处理这些位置的详细信息, 请参阅{@link #loadBeanDefinitions}.
 *
 * <p>作为设置"contextConfigLocation"参数的替代方法, 用户可以实现
 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}
 * 并设置{@linkplain ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM "contextInitializerClasses"} context-param / init-param.
 * 在这种情况下, 用户应该支持{@link #setConfigLocation(String)}方法上的{@link #refresh()}和 {@link #scan(String...)}方法,
 * 该方法主要供{@code ContextLoader}使用.
 *
 * <p>Note: 如果有多个{@code @Configuration}类, 以后的{@code @Bean}定义将覆盖先前加载的文件中定义的那些.
 * 这可以用来通过额外的Configuration类故意覆盖某些bean定义.
 */
public class AnnotationConfigWebApplicationContext extends AbstractRefreshableWebApplicationContext
		implements AnnotationConfigRegistry {

	private BeanNameGenerator beanNameGenerator;

	private ScopeMetadataResolver scopeMetadataResolver;

	private final Set<Class<?>> annotatedClasses = new LinkedHashSet<Class<?>>();

	private final Set<String> basePackages = new LinkedHashSet<String>();


	/**
	 * 设置自定义{@link BeanNameGenerator},
	 * 与{@link AnnotatedBeanDefinitionReader}和/或{@link ClassPathBeanDefinitionScanner}一起使用.
	 * <p>默认{@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * 返回自定义{@link BeanNameGenerator},
	 * 与{@link AnnotatedBeanDefinitionReader}和/或{@link ClassPathBeanDefinitionScanner}一起使用.
	 */
	protected BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}

	/**
	 * 设置自定义{@link ScopeMetadataResolver},
	 * 与{@link AnnotatedBeanDefinitionReader}和/或{@link ClassPathBeanDefinitionScanner}一起使用.
	 * <p>默认{@link org.springframework.context.annotation.AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	/**
	 * 返回自定义{@link ScopeMetadataResolver},
	 * 与{@link AnnotatedBeanDefinitionReader}和/或{@link ClassPathBeanDefinitionScanner}一起使用.
	 */
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return this.scopeMetadataResolver;
	}


	/**
	 * 注册一个或多个要处理的带注解的类.
	 * <p>请注意, 必须调用{@link #refresh()}才能使上下文完全处理新类.
	 * 
	 * @param annotatedClasses 一个或多个带注解的类,
	 * e.g. {@link org.springframework.context.annotation.Configuration @Configuration}类
	 */
	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses, "At least one annotated class must be specified");
		this.annotatedClasses.addAll(Arrays.asList(annotatedClasses));
	}

	/**
	 * 在指定的基础包中执行扫描.
	 * <p>请注意, 必须调用{@link #refresh()}才能使上下文完全处理新类.
	 * 
	 * @param basePackages 用于检查带注解的类的包
	 */
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		this.basePackages.addAll(Arrays.asList(basePackages));
	}


	/**
	 * 为{@link #register(Class...)}指定的类注册{@link org.springframework.beans.factory.config.BeanDefinition},
	 * 并扫描{@link #scan(String...)}指定的包.
	 * <p>对于{@link #setConfigLocation(String)}或{@link #setConfigLocations(String[])}指定的任何值,
	 * 首先尝试将每个位置作为类加载, 如果类加载成功则注册{@code BeanDefinition},
	 * 如果类加载失败(i.e. 引发{@code ClassNotFoundException}), 则假定该值是一个包并尝试扫描它以获取带注解的类.
	 * <p>启用默认的注解配置后处理器集, 以便可以使用{@code @Autowired}, {@code @Required}, 和关联的注解.
	 * <p>除非为构造型注解提供了{@code value}属性, 否则使用生成的bean定义名称注册配置类bean定义.
	 * 
	 * @param beanFactory 将bean定义加载到的bean工厂
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);
		ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);

		BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
		if (beanNameGenerator != null) {
			reader.setBeanNameGenerator(beanNameGenerator);
			scanner.setBeanNameGenerator(beanNameGenerator);
			beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
		}

		ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
		if (scopeMetadataResolver != null) {
			reader.setScopeMetadataResolver(scopeMetadataResolver);
			scanner.setScopeMetadataResolver(scopeMetadataResolver);
		}

		if (!this.annotatedClasses.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info("Registering annotated classes: [" +
						StringUtils.collectionToCommaDelimitedString(this.annotatedClasses) + "]");
			}
			reader.register(ClassUtils.toClassArray(this.annotatedClasses));
		}

		if (!this.basePackages.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info("Scanning base packages: [" +
						StringUtils.collectionToCommaDelimitedString(this.basePackages) + "]");
			}
			scanner.scan(StringUtils.toStringArray(this.basePackages));
		}

		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				try {
					Class<?> clazz = getClassLoader().loadClass(configLocation);
					if (logger.isInfoEnabled()) {
						logger.info("Successfully resolved class for [" + configLocation + "]");
					}
					reader.register(clazz);
				}
				catch (ClassNotFoundException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not load class for config location [" + configLocation +
								"] - trying package scan. " + ex);
					}
					int count = scanner.scan(configLocation);
					if (logger.isInfoEnabled()) {
						if (count == 0) {
							logger.info("No annotated classes found for specified class/package [" + configLocation + "]");
						}
						else {
							logger.info("Found " + count + " annotated classes in package [" + configLocation + "]");
						}
					}
				}
			}
		}
	}


	/**
	 * 为给定的bean工厂构建一个{@link AnnotatedBeanDefinitionReader}.
	 * <p>这应该使用{@code Environment}预先配置, 但不能预先配置{@code BeanNameGenerator}或{@code ScopeMetadataResolver}.
	 * 
	 * @param beanFactory 将bean定义加载到的bean工厂
	 */
	protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
		return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
	}

	/**
	 * 为给定的bean工厂构建一个{@link ClassPathBeanDefinitionScanner}.
	 * <p>这应该使用{@code Environment}预先配置, 但不能预先配置{@code BeanNameGenerator}或{@code ScopeMetadataResolver}.
	 * 
	 * @param beanFactory 将bean定义加载到的bean工厂
	 */
	protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
		return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
	}

}
