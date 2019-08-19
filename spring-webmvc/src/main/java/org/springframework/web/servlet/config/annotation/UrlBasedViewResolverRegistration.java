package org.springframework.web.servlet.config.annotation;

import java.util.Map;

import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * 协助配置{{@link org.springframework.web.servlet.view.UrlBasedViewResolver}.
 */
public class UrlBasedViewResolverRegistration {

	protected final UrlBasedViewResolver viewResolver;


	public UrlBasedViewResolverRegistration(UrlBasedViewResolver viewResolver) {
		this.viewResolver = viewResolver;
	}


	protected UrlBasedViewResolver getViewResolver() {
		return this.viewResolver;
	}

	/**
	 * 设置在构建URL时视图名称的前缀.
	 */
	public UrlBasedViewResolverRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * 设置在构建URL时视图名称的后缀.
	 */
	public UrlBasedViewResolverRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * 设置应该用于创建视图的视图类.
	 */
	public UrlBasedViewResolverRegistration viewClass(Class<?> viewClass) {
		this.viewResolver.setViewClass(viewClass);
		return this;
	}

	/**
	 * 设置此视图解析器可以处理的视图名称 (或名称模式).
	 * 视图名称可以包含简单的通配符, 以便'my*', '*Report'和'*Repo*'都匹配视图名称 'myReport'.
	 */
	public UrlBasedViewResolverRegistration viewNames(String... viewNames) {
		this.viewResolver.setViewNames(viewNames);
		return this;
	}

	/**
	 * 设置要添加到此视图解析器解析的所有视图的每个请求的模型的静态属性.
	 * 这允许设置任何类型的属性值, 例如bean引用.
	 */
	public UrlBasedViewResolverRegistration attributes(Map<String, ?> attributes) {
		this.viewResolver.setAttributesMap(attributes);
		return this;
	}

	/**
	 * 指定视图缓存的最大条目数.
	 * 默认 1024.
	 */
	public UrlBasedViewResolverRegistration cacheLimit(int cacheLimit) {
		this.viewResolver.setCacheLimit(cacheLimit);
		return this;
	}

	/**
	 * 启用或禁用缓存.
	 * <p>这相当于将{@link #cacheLimit "cacheLimit"}属性分别设置为默认限制 (1024)或 0.
	 * <p>默认"true": 启用缓存. 禁用此功能仅用于调试和开发.
	 */
	public UrlBasedViewResolverRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}
}
