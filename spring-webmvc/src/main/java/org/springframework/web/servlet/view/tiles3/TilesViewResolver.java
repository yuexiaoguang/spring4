package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.request.render.Renderer;

import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * Convenience subclass of {@link UrlBasedViewResolver} that supports
 * {@link TilesView} (i.e. Tiles definitions) and custom subclasses of it.
 */
public class TilesViewResolver extends UrlBasedViewResolver {

	private Renderer renderer;

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
	 * Set the {@link Renderer} to use. If not specified, a default
	 * {@link org.apache.tiles.renderer.DefinitionRenderer} will be used.
	 * @see TilesView#setRenderer(Renderer)
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
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
	protected TilesView buildView(String viewName) throws Exception {
		TilesView view = (TilesView) super.buildView(viewName);
		if (this.renderer != null) {
			view.setRenderer(this.renderer);
		}
		if (this.alwaysInclude != null) {
			view.setAlwaysInclude(this.alwaysInclude);
		}
		return view;
	}

}
