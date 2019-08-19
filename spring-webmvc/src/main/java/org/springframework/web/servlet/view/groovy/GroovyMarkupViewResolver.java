package org.springframework.web.servlet.view.groovy;

import java.util.Locale;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * {@link AbstractTemplateViewResolver}的便捷子类,
 * 支持{@link GroovyMarkupView} (i.e. Groovy XML/XHTML标记模板) 及其自定义子类.
 *
 * <p>可以通过{@link #setViewClass(Class)}属性指定此解析器创建的所有视图的视图类.
 *
 * <p><b>Note:</b> 链接ViewResolvers时, 此解析器将检查是否存在指定的模板资源,
 * 并且只有在实际找到模板时才返回非null的View对象.
 */
public class GroovyMarkupViewResolver extends AbstractTemplateViewResolver {

	/**
	 * 将默认的{@link #setViewClass 视图类}设置为{@link #requiredViewClass}: 默认为{@link GroovyMarkupView}.
	 */
	public GroovyMarkupViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 允许指定{@link #setPrefix 前缀}和{@link #setSuffix 后缀}.
	 * 
	 * @param prefix 构建URL时视图名称的前缀
	 * @param suffix 构建URL时视图名称的后缀
	 */
	public GroovyMarkupViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	@Override
	protected Class<?> requiredViewClass() {
		return GroovyMarkupView.class;
	}

	/**
	 * 此解析器支持 i18n, 因此缓存键应包含区域设置.
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName + '_' + locale;
	}

}
