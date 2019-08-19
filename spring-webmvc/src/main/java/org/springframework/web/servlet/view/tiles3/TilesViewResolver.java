package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.request.render.Renderer;

import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link UrlBasedViewResolver}的便捷子类, 支持{@link TilesView} (i.e. Tiles定义)及其自定义子类.
 */
public class TilesViewResolver extends UrlBasedViewResolver {

	private Renderer renderer;

	private Boolean alwaysInclude;


	public TilesViewResolver() {
		setViewClass(requiredViewClass());
	}


	/**
	 * 此解析器需要{@link TilesView}.
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return TilesView.class;
	}

	/**
	 * 设置要使用的{@link Renderer}.
	 * 如果未指定, 将使用默认的{@link org.apache.tiles.renderer.DefinitionRenderer}.
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * 指定是始终包含视图, 还是转发.
	 * <p>默认为"false". 切换此标志以强制使用Servlet包含, 即使可以转发.
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
