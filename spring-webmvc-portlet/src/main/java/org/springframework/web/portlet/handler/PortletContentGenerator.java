package org.springframework.web.portlet.handler;

import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.springframework.web.portlet.context.PortletApplicationObjectSupport;

/**
 * 适用于任何类型的Web内容生成器的便捷超类, 如{@link org.springframework.web.portlet.mvc.AbstractController}.
 * 也可以用于拥有自己的{@link org.springframework.web.portlet.HandlerAdapter}的自定义处理器.
 *
 * <p>支持portlet缓存控制选项.
 */
public abstract class PortletContentGenerator extends PortletApplicationObjectSupport {

	private boolean requireSession = false;

	private int cacheSeconds = -1;


	/**
	 * 设置是否需要会话来处理请求.
	 */
	public final void setRequireSession(boolean requireSession) {
		this.requireSession = requireSession;
	}

	/**
	 * 返回是否需要会话来处理请求.
	 */
	public final boolean isRequireSession() {
		return this.requireSession;
	}

	/**
	 * 缓存内容给定的秒数.
	 * 默认为-1, 表示不覆盖portlet内容缓存.
	 * <p>仅当此设置为 0 (无缓存) 或正值 (缓存此秒数)时, 此类才会覆盖portlet设置.
	 * <p>在生成内容之前, 子类可以覆盖缓存设置.
	 */
	public final void setCacheSeconds(int seconds) {
		this.cacheSeconds = seconds;
	}

	/**
	 * 返回缓存内容的秒数.
	 */
	public final int getCacheSeconds() {
		return this.cacheSeconds;
	}


	/**
	 * 根据此生成器的设置检查并准备给定的请求和响应.
	 * 检查所需的会话, 并应用为此生成器配置的缓存秒数 (如果它是渲染请求/响应).
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * 
	 * @throws PortletException 如果由于检查失败而无法处理请求
	 */
	protected final void check(PortletRequest request, PortletResponse response) throws PortletException {
		if (this.requireSession) {
			if (request.getPortletSession(false) == null) {
				throw new PortletSessionRequiredException("Pre-existing session required but none found");
			}
		}
	}

	/**
	 * 根据此生成器的设置检查并准备给定的请求和响应.
	 * 检查所需的会话, 并应用为此生成器配置的缓存秒数 (如果它是渲染请求/响应).
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * 
	 * @throws PortletException 如果由于检查失败而无法处理请求
	 */
	protected final void checkAndPrepare(PortletRequest request, MimeResponse response)
			throws PortletException {

		checkAndPrepare(request, response, this.cacheSeconds);
	}

	/**
	 * 根据此生成器的设置检查并准备给定的请求和响应.
	 * 检查所需的会话, 并应用给定的缓存秒数 (如果它是渲染请求/响应).
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * @param cacheSeconds 缓存响应的秒数, 0 防止缓存
	 * 
	 * @throws PortletException 如果由于检查失败而无法处理请求
	 */
	protected final void checkAndPrepare(PortletRequest request, MimeResponse response, int cacheSeconds)
			throws PortletException {

		check(request, response);
		applyCacheSeconds(response, cacheSeconds);
	}

	/**
	 * 防止缓存渲染响应.
	 */
	protected final void preventCaching(MimeResponse response) {
		cacheForSeconds(response, 0);
	}

	/**
	 * 设置portlet响应以允许缓存给定的秒数.
	 * 
	 * @param response 当前portlet渲染响应
	 * @param seconds 响应应该缓存的秒数
	 */
	protected final void cacheForSeconds(MimeResponse response, int seconds) {
		response.setProperty(MimeResponse.EXPIRATION_CACHE, Integer.toString(seconds));
	}

	/**
	 * 将给定的缓存秒应用于渲染响应
	 * 
	 * @param response 当前portlet渲染响应
	 * @param seconds 缓存响应的秒数, 0 以防止缓存
	 */
	protected final void applyCacheSeconds(MimeResponse response, int seconds) {
		if (seconds > 0) {
			cacheForSeconds(response, seconds);
		}
		else if (seconds == 0) {
			preventCaching(response);
		}
		// 不过, 将缓存留给portlet配置.
	}

}
