package org.springframework.web.servlet.mvc.multiaction;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.web.util.UrlPathHelper;

/**
 * 基于URL的{@link MethodNameResolver}实现的抽象基类.
 *
 * <p>提供将处理器映射到URL和可配置URL查找的基础结构.
 * For information on the latter, see the
 * {@link #setAlwaysUseFullPath} "alwaysUseFullPath"}
 * and {@link #setUrlDecode "urlDecode"} properties.
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
 */
@Deprecated
public abstract class AbstractUrlMethodNameResolver implements MethodNameResolver {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * Set if URL lookup should always use full path within current servlet
	 * context. Else, the path within the current servlet mapping is used
	 * if applicable (i.e. in the case of a ".../*" servlet mapping in web.xml).
	 * Default is "false".
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Set if context path and request URI should be URL-decoded.
	 * Both are returned <i>undecoded</i> by the Servlet API,
	 * in contrast to the servlet path.
	 * <p>Uses either the request encoding or the default encoding according
	 * to the Servlet spec (ISO-8859-1).
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple MethodNameResolvers
	 * and HandlerMappings.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}


	/**
	 * Retrieves the URL path to use for lookup and delegates to
	 * {@code getHandlerMethodNameForUrlPath}.
	 * Converts {@code null} values to NoSuchRequestHandlingMethodExceptions.
	 * @see #getHandlerMethodNameForUrlPath
	 */
	@Override
	public final String getHandlerMethodName(HttpServletRequest request)
			throws NoSuchRequestHandlingMethodException {

		String urlPath = this.urlPathHelper.getLookupPathForRequest(request);
		String name = getHandlerMethodNameForUrlPath(urlPath);
		if (name == null) {
			throw new NoSuchRequestHandlingMethodException(urlPath, request.getMethod(), request.getParameterMap());
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Returning handler method name '" + name + "' for lookup path: " + urlPath);
		}
		return name;
	}

	/**
	 * Return a method name that can handle this request, based on the
	 * given lookup path. Called by {@code getHandlerMethodName}.
	 * @param urlPath the URL path to use for lookup,
	 * according to the settings in this class
	 * @return a method name that can handle this request.
	 * Should return null if no matching method found.
	 * @see #getHandlerMethodName
	 * @see #setAlwaysUseFullPath
	 * @see #setUrlDecode
	 */
	protected abstract String getHandlerMethodNameForUrlPath(String urlPath);

}
