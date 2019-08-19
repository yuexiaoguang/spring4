package org.springframework.web.servlet.view;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;

/**
 * {@link org.springframework.web.servlet.ViewResolver}实现,
 * 它在{@link ResourceBundle}中使用bean定义, 由包的基础名称指定.
 *
 * <p>包通常在位于类路径中的属性文件中定义.
 * 默认包的基础名称是"views".
 *
 * <p>这个{@code ViewResolver}支持本地化的视图定义, 使用{@link java.util.PropertyResourceBundle}的默认支持.
 * 例如, 基本名称"views"将被解析为类路径资源"views_de_AT.properties",
 * "views_de.properties", "views.properties" - 用于给定的Locale "de_AT".
 *
 * <p>Note: 这个{@code ViewResolver}实现了{@link Ordered}接口, 以便灵活地参与{@code ViewResolver}链接.
 * 例如, 可以通过此{@code ViewResolver}定义一些特殊视图 (其"order"值为0), 而所有剩余视图可以通过{@link UrlBasedViewResolver}解析.
 */
public class ResourceBundleViewResolver extends AbstractCachingViewResolver
		implements Ordered, InitializingBean, DisposableBean {

	/** 如果未提供其他基本名称, 则为默认基本名称 */
	public static final String DEFAULT_BASENAME = "views";


	private String[] basenames = new String[] {DEFAULT_BASENAME};

	private ClassLoader bundleClassLoader = Thread.currentThread().getContextClassLoader();

	private String defaultParentView;

	private Locale[] localesToInitialize;

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	/* Locale -> BeanFactory */
	private final Map<Locale, BeanFactory> localeCache =
			new HashMap<Locale, BeanFactory>();

	/* List of ResourceBundle -> BeanFactory */
	private final Map<List<ResourceBundle>, ConfigurableApplicationContext> bundleCache =
			new HashMap<List<ResourceBundle>, ConfigurableApplicationContext>();


	/**
	 * 按照{@link java.util.ResourceBundle}约定设置单个基本名称.
	 * 默认为"views".
	 * <p>{@code ResourceBundle}支持不同的区域设置后缀.
	 * 例如, "views"的基本名称可能映射到{@code ResourceBundle}文件"views", "views_en_au" 和 "views_de".
	 * <p>请注意, ResourceBundle名称实际上是类路径位置: 因此, JDK的标准ResourceBundle将点视为包分隔符.
	 * 这意味着"test.theme"实际上等同于"test/theme", 就像程序化{@code java.util.ResourceBundle}用法一样.
	 */
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * 设置一个基本名称数组, 每个基本名称遵循{@link java.util.ResourceBundle}约定.
	 * 默认是单个基本名称"views".
	 * <p>{@code ResourceBundle}支持不同的区域设置后缀.
	 * 例如, "views"的基本名称可能映射到{@code ResourceBundle}文件"views", "views_en_au" 和 "views_de".
	 * <p>在解析消息代码时, 将依次检查关联的资源包.
	 * 请注意, 由于顺序查找, <i>上一个</i>资源包中的消息定义将覆盖后一个包中的消息定义.
	 * <p>请注意, ResourceBundle名称实际上是类路径位置: 因此, JDK的标准ResourceBundle将点视为包分隔符.
	 * 这意味着"test.theme"实际上等同于"test/theme", 就像程序化{@code java.util.ResourceBundle}用法一样.
	 */
	public void setBasenames(String... basenames) {
		this.basenames = basenames;
	}

	/**
	 * 设置加载资源包的{@link ClassLoader}.
	 * 默认是线程上下文{@code ClassLoader}.
	 */
	public void setBundleClassLoader(ClassLoader classLoader) {
		this.bundleClassLoader = classLoader;
	}

	/**
	 * 返回加载资源包的{@link ClassLoader}.
	 * <p>默认是指定的包{@code ClassLoader}, 通常是线程上下文{@code ClassLoader}.
	 */
	protected ClassLoader getBundleClassLoader() {
		return this.bundleClassLoader;
	}

	/**
	 * 为{@code ResourceBundle}中定义的视图设置默认父级.
	 * <p>这避免了包中重复的 "yyy1.(parent)=xxx", "yyy2.(parent)=xxx"定义, 特别是如果所有定义的视图共享同一个父级.
	 * <p>父级通常会定义视图类和公共属性.
	 * 具体视图可能只包含一个URL定义: "yyy1.url=/my.jsp", "yyy2.url=/your.jsp".
	 * <p>定义自己的父级或携带自己的类的View定义仍然可以覆盖它.
	 * 严格地说, 默认父设置不适用于带有类的bean定义的规则是出于向后兼容性的原因.
	 * 它仍然符合典型的用例.
	 */
	public void setDefaultParentView(String defaultParentView) {
		this.defaultParentView = defaultParentView;
	}

	/**
	 * 指定Locales以便在实际访问时实时地初始化, 而不是延迟初始化.
	 * <p>允许预先初始化常见的语言环境, 实时地检查这些语言环境的视图配置.
	 */
	public void setLocalesToInitialize(Locale... localesToInitialize) {
		this.localesToInitialize = localesToInitialize;
	}

	/**
	 * 指定此ViewResolver bean的顺序值.
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}, 表示无序.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 如有必要, 实时地初始化语言环境.
	 */
	@Override
	public void afterPropertiesSet() throws BeansException {
		if (this.localesToInitialize != null) {
			for (Locale locale : this.localesToInitialize) {
				initFactory(locale);
			}
		}
	}


	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		BeanFactory factory = initFactory(locale);
		try {
			return factory.getBean(viewName, View.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Allow for ViewResolver chaining...
			return null;
		}
	}

	/**
	 * 对于给定的{@link Locale locale}, 从{@code ResourceBundle}初始化View {@link BeanFactory}.
	 * <p>由于并行线程的访问而同步.
	 * 
	 * @param locale 目标{@code Locale}
	 * 
	 * @return 给定Locale的View工厂
	 * @throws BeansException 初始化错误
	 */
	protected synchronized BeanFactory initFactory(Locale locale) throws BeansException {
		// 尝试查找Locale的缓存工厂: 之前是否已经遇到过该Locale?
		if (isCache()) {
			BeanFactory cachedFactory = this.localeCache.get(locale);
			if (cachedFactory != null) {
				return cachedFactory;
			}
		}

		// 构建Locale的ResourceBundle引用列表.
		List<ResourceBundle> bundles = new LinkedList<ResourceBundle>();
		for (String basename : this.basenames) {
			ResourceBundle bundle = getBundle(basename, locale);
			bundles.add(bundle);
		}

		// 尝试为ResourceBundle列表查找缓存的工厂: 即使Locale不同, 也可能找到相同的包.
		if (isCache()) {
			BeanFactory cachedFactory = this.bundleCache.get(bundles);
			if (cachedFactory != null) {
				this.localeCache.put(locale, cachedFactory);
				return cachedFactory;
			}
		}

		// 为视图创建子ApplicationContext.
		GenericWebApplicationContext factory = new GenericWebApplicationContext();
		factory.setParent(getApplicationContext());
		factory.setServletContext(getServletContext());

		// 从资源包加载bean定义.
		PropertiesBeanDefinitionReader reader = new PropertiesBeanDefinitionReader(factory);
		reader.setDefaultParentBean(this.defaultParentView);
		for (ResourceBundle bundle : bundles) {
			reader.registerBeanDefinitions(bundle);
		}

		factory.refresh();

		// Locale和ResourceBundle列表的缓存工厂.
		if (isCache()) {
			this.localeCache.put(locale, factory);
			this.bundleCache.put(bundles, factory);
		}

		return factory;
	}

	/**
	 * 获取给定基本名称的资源包和{@link Locale}.
	 * 
	 * @param basename 要查找的基本名称
	 * @param locale 要寻找的{@code Locale}
	 * 
	 * @return 对应的{@code ResourceBundle}
	 * @throws MissingResourceException 如果找不到匹配的包
	 */
	protected ResourceBundle getBundle(String basename, Locale locale) throws MissingResourceException {
		return ResourceBundle.getBundle(basename, locale, getBundleClassLoader());
	}


	/**
	 * 在上下文关闭时, 关闭包View工厂.
	 */
	@Override
	public void destroy() throws BeansException {
		for (ConfigurableApplicationContext factory : this.bundleCache.values()) {
			factory.close();
		}
		this.localeCache.clear();
		this.bundleCache.clear();
	}

}
