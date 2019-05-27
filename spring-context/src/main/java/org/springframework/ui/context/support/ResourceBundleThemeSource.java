package org.springframework.ui.context.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

/**
 * {@link ThemeSource}实现, 每个主题查找一个{@link java.util.ResourceBundle}.
 * 主题名称被解释为ResourceBundle basename, 支持所有主题的公共basename前缀.
 */
public class ResourceBundleThemeSource implements HierarchicalThemeSource, BeanClassLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private ThemeSource parentThemeSource;

	private String basenamePrefix = "";

	private String defaultEncoding;

	private Boolean fallbackToSystemLocale;

	private ClassLoader beanClassLoader;

	/** 主题名称 -> Theme实例 */
	private final Map<String, Theme> themeCache = new ConcurrentHashMap<String, Theme>();


	@Override
	public void setParentThemeSource(ThemeSource parent) {
		this.parentThemeSource = parent;

		// 更新现有的Theme对象.
		// 通常在此次调用时不应该有任何内容.
		synchronized (this.themeCache) {
			for (Theme theme : this.themeCache.values()) {
				initParent(theme);
			}
		}
	}

	@Override
	public ThemeSource getParentThemeSource() {
		return this.parentThemeSource;
	}

	/**
	 * 设置应用于ResourceBundle基础名称的前缀, i.e. 主题名称.
	 * E.g.: basenamePrefix="test.", themeName="theme" -> basename="test.theme".
	 * <p>请注意, ResourceBundle名称实际上是类路径位置:
	 * 因此, JDK的标准ResourceBundle将点视为包分隔符.
	 * 意味着"test.theme"实际上等同于"test/theme", 就像程序化{@code java.util.ResourceBundle}用法一样.
	 */
	public void setBasenamePrefix(String basenamePrefix) {
		this.basenamePrefix = (basenamePrefix != null ? basenamePrefix : "");
	}

	/**
	 * 设置用于解析资源包文件的默认字符集.
	 * <p>{@link ResourceBundleMessageSource}的默认值是{@code java.util.ResourceBundle}默认编码: ISO-8859-1.
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 设置在找不到特定区域设置的文件时, 是否回退到系统区域设置.
	 * <p>{@link ResourceBundleMessageSource}的默认值是 "true".
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 此实现返回一个SimpleTheme实例, 其中包含一个基于ResourceBundle的MessageSource,
	 * 其基本名称对应于给定的主题名称 (以配置的"basenamePrefix"为前缀).
	 * <p>SimpleTheme实例按主题名称进行缓存. 如果主题应该反映对底层文件的更改, 请使用可重新加载的MessageSource.
	 */
	@Override
	public Theme getTheme(String themeName) {
		if (themeName == null) {
			return null;
		}
		Theme theme = this.themeCache.get(themeName);
		if (theme == null) {
			synchronized (this.themeCache) {
				theme = this.themeCache.get(themeName);
				if (theme == null) {
					String basename = this.basenamePrefix + themeName;
					MessageSource messageSource = createMessageSource(basename);
					theme = new SimpleTheme(themeName, messageSource);
					initParent(theme);
					this.themeCache.put(themeName, theme);
					if (logger.isDebugEnabled()) {
						logger.debug("Theme created: name '" + themeName + "', basename [" + basename + "]");
					}
				}
			}
		}
		return theme;
	}

	/**
	 * 为给定的basename创建一个MessageSource, 用作相应主题的MessageSource.
	 * <p>默认实现创建ResourceBundleMessageSource.
	 * 例如, 子类可以创建专门配置的ReloadableResourceBundleMessageSource.
	 * 
	 * @param basename 为其创建MessageSource的基本名称
	 * 
	 * @return the MessageSource
	 */
	protected MessageSource createMessageSource(String basename) {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(basename);
		if (this.defaultEncoding != null) {
			messageSource.setDefaultEncoding(this.defaultEncoding);
		}
		if (this.fallbackToSystemLocale != null) {
			messageSource.setFallbackToSystemLocale(this.fallbackToSystemLocale);
		}
		if (this.beanClassLoader != null) {
			messageSource.setBeanClassLoader(this.beanClassLoader);
		}
		return messageSource;
	}

	/**
	 * 使用此ThemeSource的相应父级中的一个, 初始化给定主题的MessageSource.
	 * 
	 * @param theme 要(重新)初始化主题
	 */
	protected void initParent(Theme theme) {
		if (theme.getMessageSource() instanceof HierarchicalMessageSource) {
			HierarchicalMessageSource messageSource = (HierarchicalMessageSource) theme.getMessageSource();
			if (getParentThemeSource() != null && messageSource.getParentMessageSource() == null) {
				Theme parentTheme = getParentThemeSource().getTheme(theme.getName());
				if (parentTheme != null) {
					messageSource.setParentMessageSource(parentTheme.getMessageSource());
				}
			}
		}
	}
}
