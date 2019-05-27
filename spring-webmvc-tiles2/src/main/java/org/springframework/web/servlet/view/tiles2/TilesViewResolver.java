package org.springframework.web.servlet.view.tiles2;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * Convenience subclass of {@link org.springframework.web.servlet.view.UrlBasedViewResolver}
 * that supports {@link TilesView} (i.e. Tiles definitions) and custom subclasses of it.
 *
 * <p>The view class for all views generated by this resolver can be specified
 * via the "viewClass" property. See UrlBasedViewResolver's javadoc for details.
 *
 * <p><b>Note:</b> When chaining ViewResolvers, a TilesViewResolver will
 * check for the existence of the specified template resources and only return
 * a non-null View object if the template was actually found.
 *
 * <p><b>NOTE: Tiles 2 support is deprecated in favor of Tiles 3 and will be removed
 * as of Spring Framework 5.0.</b>.
 *
 * @deprecated as of Spring 4.2, in favor of Tiles 3
 */
@Deprecated
public class TilesViewResolver extends UrlBasedViewResolver {

	private Boolean alwaysInclude;


	public TilesViewResolver() {
		setViewClass(requiredViewClass());
	}


	/**
	 * This resolver requires {@link TilesView}.
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return TilesView.class;
	}

	/**
	 * Specify whether to always include the view rather than forward to it.
	 * <p>Default is "false". Switch this flag on to enforce the use of a
	 * Servlet include, even if a forward would be possible.
	 * @since 4.1.2
	 * @see TilesView#setAlwaysInclude
	 */
	public void setAlwaysInclude(Boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		TilesView view = (TilesView) super.buildView(viewName);
		if (this.alwaysInclude != null) {
			view.setAlwaysInclude(this.alwaysInclude);
		}
		return view;
	}

}