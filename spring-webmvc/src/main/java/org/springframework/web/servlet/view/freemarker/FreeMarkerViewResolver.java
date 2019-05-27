package org.springframework.web.servlet.view.freemarker;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * Convenience subclass of {@link org.springframework.web.servlet.view.UrlBasedViewResolver}
 * that supports {@link FreeMarkerView} (i.e. FreeMarker templates) and custom subclasses of it.
 *
 * <p>The view class for all views generated by this resolver can be specified
 * via the "viewClass" property. See UrlBasedViewResolver's javadoc for details.
 *
 * <p><b>Note:</b> When chaining ViewResolvers, a FreeMarkerViewResolver will
 * check for the existence of the specified template resources and only return
 * a non-null View object if the template was actually found.
 */
public class FreeMarkerViewResolver extends AbstractTemplateViewResolver {

	/**
	 * Sets the default {@link #setViewClass view class} to {@link #requiredViewClass}:
	 * by default {@link FreeMarkerView}.
	 */
	public FreeMarkerViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * A convenience constructor that allows for specifying {@link #setPrefix prefix}
	 * and {@link #setSuffix suffix} as constructor arguments.
	 * @param prefix the prefix that gets prepended to view names when building a URL
	 * @param suffix the suffix that gets appended to view names when building a URL
	 * @since 4.3
	 */
	public FreeMarkerViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	/**
	 * Requires {@link FreeMarkerView}.
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return FreeMarkerView.class;
	}

}