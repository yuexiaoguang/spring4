package org.springframework.core.io.support;

import java.util.Locale;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * 用于加载本地化资源的Helper类, 通过名称, 扩展名和当前区域设置指定.
 */
public class LocalizedResourceHelper {

	/** 在文件名部分之间使用的默认分隔符: 下划线 */
	public static final String DEFAULT_SEPARATOR = "_";


	private final ResourceLoader resourceLoader;

	private String separator = DEFAULT_SEPARATOR;


	/**
	 * 使用DefaultResourceLoader.
	 */
	public LocalizedResourceHelper() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * @param resourceLoader 要使用的ResourceLoader
	 */
	public LocalizedResourceHelper(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 设置在文件名之间使用的分隔符.
	 * 默认是下划线 ("_").
	 */
	public void setSeparator(String separator) {
		this.separator = (separator != null ? separator : DEFAULT_SEPARATOR);
	}


	/**
	 * 查找给定名称, 扩展名和区域设置的最具体的本地化资源:
	 * <p>将按以下顺序搜索文件, 类似于{@code java.util.ResourceBundle}的搜索顺序:
	 * <ul>
	 * <li>[name]_[language]_[country]_[variant][extension]
	 * <li>[name]_[language]_[country][extension]
	 * <li>[name]_[language][extension]
	 * <li>[name][extension]
	 * </ul>
	 * <p>如果找不到任何特定文件, 则将返回默认位置的资源描述符.
	 * 
	 * @param name 文件名, 没有本地化部分, 也没有扩展名
	 * @param extension 文件扩展名 (e.g. ".xls")
	 * @param locale 当前的区域设置 (may be {@code null})
	 * 
	 * @return 找到的最具体的本地化资源
	 */
	public Resource findLocalizedResource(String name, String extension, Locale locale) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(extension, "Extension must not be null");

		Resource resource = null;

		if (locale != null) {
			String lang = locale.getLanguage();
			String country = locale.getCountry();
			String variant = locale.getVariant();

			// 检查包含语言, 国家和变体本地化的文件.
			if (variant.length() > 0) {
				String location =
						name + this.separator + lang + this.separator + country + this.separator + variant + extension;
				resource = this.resourceLoader.getResource(location);
			}

			// 检查语言和国家/地区本地化的文件.
			if ((resource == null || !resource.exists()) && country.length() > 0) {
				String location = name + this.separator + lang + this.separator + country + extension;
				resource = this.resourceLoader.getResource(location);
			}

			// 检查具有语言本地化的文档.
			if ((resource == null || !resource.exists()) && lang.length() > 0) {
				String location = name + this.separator + lang + extension;
				resource = this.resourceLoader.getResource(location);
			}
		}

		// 检查没有本地化的文档.
		if (resource == null || !resource.exists()) {
			String location = name + extension;
			resource = this.resourceLoader.getResource(location);
		}

		return resource;
	}

}
