package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@code Controllers}的抽象基类, 它根据请求URL返回视图名称.
 *
 * <p>提供用于从URL和可配置URL查找确定视图名称的基础结构.
 * 有关后者的信息, 请参阅{@code alwaysUseFullPath}和{@code urlDecode}属性.
 */
public abstract class AbstractUrlViewController extends AbstractController {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 设置URL查找是否应始终使用当前servlet上下文中的完整路径.
	 * 否则, 如果适用, 则使用当前servlet映射中的路径 (i.e. 在web.xml中的".../*" servlet映射的情况下).
	 * Default is "false".
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 设置是否应对上下文路径和请求URI进行URL解码.
	 * 与servlet路径相比, Servlet API都返回<i>未解码</i>.
	 * <p>根据Servlet规范 (ISO-8859-1)使用请求编码或默认编码.
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * 设置是否应从请求URI中删除";" (分号)内容.
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * 设置用于查找路径的解析的UrlPathHelper.
	 * <p>使用此选项可以使用自定义子类覆盖默认的UrlPathHelper,
	 * 或者在多个MethodNameResolvers和HandlerMappings之间共享常用的UrlPathHelper设置.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回用于查找路径的解析的UrlPathHelper.
	 */
	protected UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	/**
	 * 检索用于查找和委托给{@link #getViewNameForRequest}的URL路径.
	 * 还将{@link RequestContextUtils#getInputFlashMap}的内容添加到模型中.
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		String viewName = getViewNameForRequest(request);
		if (logger.isDebugEnabled()) {
			logger.debug("Returning view name '" + viewName + "' for lookup path [" + lookupPath + "]");
		}
		return new ModelAndView(viewName, RequestContextUtils.getInputFlashMap(request));
	}

	/**
	 * 根据给定的查找路径, 返回要为此请求呈现的视图的名称. 由{@link #handleRequestInternal}调用.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 请求的视图名称 (never {@code null})
	 */
	protected abstract String getViewNameForRequest(HttpServletRequest request);

}
