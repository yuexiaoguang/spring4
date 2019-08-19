package org.springframework.web.servlet.view.freemarker;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * {@link org.springframework.web.servlet.view.UrlBasedViewResolver}的便捷子类,
 * 支持{@link FreeMarkerView} (i.e. FreeMarker模板)及其自定义子类.
 *
 * <p>可以通过"viewClass"属性指定此解析器生成的所有视图的视图类. 有关详细信息, 请参阅UrlBasedViewResolver的 javadoc.
 *
 * <p><b>Note:</b> 链接ViewResolvers时, FreeMarkerViewResolver将检查是否存在指定的模板资源,
 * 且只有在实际找到模板时才返回非null的View对象.
 */
public class FreeMarkerViewResolver extends AbstractTemplateViewResolver {

	/**
	 * 将默认的{@link #setViewClass 视图类}设置为{@link #requiredViewClass}: 默认为{@link FreeMarkerView}.
	 */
	public FreeMarkerViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 允许指定{@link #setPrefix 前缀} 和 {@link #setSuffix 后缀}.
	 * 
	 * @param prefix 构建URL时视图名称的前缀
	 * @param suffix 构建URL时视图名称的后缀
	 */
	public FreeMarkerViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	/**
	 * 需要{@link FreeMarkerView}.
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return FreeMarkerView.class;
	}

}
