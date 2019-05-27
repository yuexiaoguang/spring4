package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

/**
 * 独立应用程序上下文, 接受带注解的类作为输入 - 尤其是带{@link Configuration @Configuration}注解的类,
 * 还有使用普通的{@link org.springframework.stereotype.Component @Component}类型,
 * 和使用{@code javax.inject}注解的JSR-330兼容类.
 * 允许使用{@link #register(Class...)}逐个注册类, 以及使用 {@link #scan(String...)}进行类路径扫描.
 *
 * <p>在多个{@code @Configuration}类的情况下, 后面的类中定义的@{@link Bean}方法将覆盖在早期类中定义的那些方法.
 * 这可以用来通过额外的 {@code @Configuration}类覆盖某些bean定义.
 *
 * <p>See @{@link Configuration}'s javadoc for usage examples.
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

	private final AnnotatedBeanDefinitionReader reader;

	private final ClassPathBeanDefinitionScanner scanner;


	/**
	 * 创建需要通过{@link #register}调用填充, 然后手动{@linkplain #refresh 刷新}的AnnotationConfigApplicationContext.
	 */
	public AnnotationConfigApplicationContext() {
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * @param beanFactory 用于此上下文的DefaultListableBeanFactory实例
	 */
	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 从给定的带注解的类派生bean定义, 并自动刷新上下文.
	 * 
	 * @param annotatedClasses 一个或多个带注解的类, e.g. {@link Configuration @Configuration}类
	 */
	public AnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
		this();
		register(annotatedClasses);
		refresh();
	}

	/**
	 * 扫描给定包中的bean定义, 并自动刷新上下文.
	 * 
	 * @param basePackages 用于检查带注解的类的包
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		this();
		scan(basePackages);
		refresh();
	}


	/**
	 * 将给定的自定义{@code Environment}传播到底层 {@link AnnotatedBeanDefinitionReader}和{@link ClassPathBeanDefinitionScanner}.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * 提供自定义{@link BeanNameGenerator}以用于 {@link AnnotatedBeanDefinitionReader}和/或{@link ClassPathBeanDefinitionScanner}.
	 * <p>默认是 {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 * <p>在调用{@link #register(Class...)}和/或{@link #scan(String...)}之前, 必须调用此方法.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
	}

	/**
	 * 设置用于检测bean类的{@link ScopeMetadataResolver}.
	 * <p>默认是{@link AnnotationScopeMetadataResolver}.
	 * <p>在调用{@link #register(Class...)}和/或{@link #scan(String...)}之前, 必须调用此方法.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}

	@Override
	protected void prepareRefresh() {
		this.scanner.clearCache();
		super.prepareRefresh();
	}


	//---------------------------------------------------------------------
	// Implementation of AnnotationConfigRegistry
	//---------------------------------------------------------------------

	/**
	 * 注册一个或多个要处理的带注解的类.
	 * <p>请注意, 必须调用{@link #refresh()}才能使上下文完全处理新类.
	 * 
	 * @param annotatedClasses 一个或多个带注解的类, e.g. {@link Configuration @Configuration}类
	 */
	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses, "At least one annotated class must be specified");
		this.reader.register(annotatedClasses);
	}

	/**
	 * 在指定的基础包中执行扫描.
	 * <p>请注意, 必须调用{@link #refresh()}才能使上下文完全处理新类.
	 * 
	 * @param basePackages 要检查带注解的类的包
	 */
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		this.scanner.scan(basePackages);
	}

}
