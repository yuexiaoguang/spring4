package org.springframework.web.servlet.view.tiles3;

import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tiles.TilesContainer;
import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.renderer.DefinitionRenderer;
import org.apache.tiles.request.AbstractRequest;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.apache.tiles.request.servlet.ServletRequest;
import org.apache.tiles.request.servlet.ServletUtil;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * 通过Tiles Request API呈现的{@link org.springframework.web.servlet.View}实现.
 * "url"属性被解释为Tiles定义的名称.
 */
public class TilesView extends AbstractUrlBasedView {

	private Renderer renderer;

	private boolean exposeJstlAttributes = true;

	private boolean alwaysInclude = false;

	private ApplicationContext applicationContext;


	/**
	 * 设置要使用的{@link Renderer}.
	 * 如果未设置, 则默认使用{@link DefinitionRenderer}.
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * 是否公开JSTL属性. 默认为{@code true}.
	 */
	protected void setExposeJstlAttributes(boolean exposeJstlAttributes) {
		this.exposeJstlAttributes = exposeJstlAttributes;
	}

	/**
	 * 指定是始终包含视图, 还是转发.
	 * <p>默认为"false". 切换此标志以强制使用Servlet包含, 即使可以转发.
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		this.applicationContext = ServletUtil.getApplicationContext(getServletContext());
		if (this.renderer == null) {
			TilesContainer container = TilesAccess.getContainer(this.applicationContext);
			this.renderer = new DefinitionRenderer(container);
		}
	}


	@Override
	public boolean checkResource(final Locale locale) throws Exception {
		HttpServletRequest servletRequest = null;
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		}
		Request request = new ServletRequest(this.applicationContext, servletRequest, null) {
			@Override
			public Locale getRequestLocale() {
				return locale;
			}
		};
		return this.renderer.isRenderable(getUrl(), request);
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		exposeModelAsRequestAttributes(model, request);
		if (this.exposeJstlAttributes) {
			JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
		}
		if (this.alwaysInclude) {
			request.setAttribute(AbstractRequest.FORCE_INCLUDE_ATTRIBUTE_NAME, true);
		}

		Request tilesRequest = createTilesRequest(request, response);
		this.renderer.render(getUrl(), tilesRequest);
	}

	/**
	 * 创建Tiles {@link Request}.
	 * <p>此实现创建{@link ServletRequest}.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * 
	 * @return Tiles请求
	 */
	protected Request createTilesRequest(final HttpServletRequest request, HttpServletResponse response) {
		return new ServletRequest(this.applicationContext, request, response) {
			@Override
			public Locale getRequestLocale() {
				return RequestContextUtils.getLocale(request);
			}
		};
	}

}
