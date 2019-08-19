package org.springframework.web.servlet.mvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 简单的{@code Controller}实现, 它将URL的虚拟路径转换为视图名称, 并返回该视图.
 *
 * <p>可以选择预先添加{@link #setPrefix 前缀}和/或附加{@link #setSuffix 后缀}来构建URL文件名中的视图名称.
 *
 * <p>示例:
 * <ol>
 * <li>{@code "/index" -> "index"}</li>
 * <li>{@code "/index.html" -> "index"}</li>
 * <li>{@code "/index.html"} + prefix {@code "pre_"} and suffix {@code "_suf" -> "pre_index_suf"}</li>
 * <li>{@code "/products/view.html" -> "products/view"}</li>
 * </ol>
 *
 * <p>Thanks to David Barri for suggesting prefix/suffix support!
 */
public class UrlFilenameViewController extends AbstractUrlViewController {

	private String prefix = "";

	private String suffix = "";

	/** 请求URL路径字符串 --> 视图名称字符串 */
	private final Map<String, String> viewNameCache = new ConcurrentHashMap<String, String>(256);


	/**
	 * 设置请求URL文件名的前缀, 以构建视图名称.
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回添加到请求URL文件名的前缀.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置请求URL文件名的后缀, 以构建视图名称.
	 */
	public void setSuffix(String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回请求URL文件名的后缀.
	 */
	protected String getSuffix() {
		return this.suffix;
	}


	/**
	 * 根据URL文件名返回视图名称, 并在适当时应用前缀/后缀.
	 */
	@Override
	protected String getViewNameForRequest(HttpServletRequest request) {
		String uri = extractOperableUrl(request);
		return getViewNameForUrlPath(uri);
	}

	/**
	 * 从给定请求中提取URL路径, 适用于视图名称提取.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 用于视图名称提取的URL
	 */
	protected String extractOperableUrl(HttpServletRequest request) {
		String urlPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (!StringUtils.hasText(urlPath)) {
			urlPath = getUrlPathHelper().getLookupPathForRequest(request);
		}
		return urlPath;
	}

	/**
	 * 根据URL文件名返回视图名称, 并在适当时应用前缀/后缀.
	 * 
	 * @param uri 请求URI; 例如{@code "/index.html"}
	 * 
	 * @return 提取的URI文件名; 例如{@code "index"}
	 */
	protected String getViewNameForUrlPath(String uri) {
		String viewName = this.viewNameCache.get(uri);
		if (viewName == null) {
			viewName = extractViewNameFromUrlPath(uri);
			viewName = postProcessViewName(viewName);
			this.viewNameCache.put(uri, viewName);
		}
		return viewName;
	}

	/**
	 * 从给定的请求URI中提取URL文件名.
	 * 
	 * @param uri 请求URI; 例如{@code "/index.html"}
	 * 
	 * @return 提取的URI文件名; 例如{@code "index"}
	 */
	protected String extractViewNameFromUrlPath(String uri) {
		int start = (uri.charAt(0) == '/' ? 1 : 0);
		int lastIndex = uri.lastIndexOf('.');
		int end = (lastIndex < 0 ? uri.length() : lastIndex);
		return uri.substring(start, end);
	}

	/**
	 * 根据URL路径指示的给定视图名称构建完整视图名称.
	 * <p>默认实现只是应用前缀和后缀.
	 * 这可以被覆盖, 例如, 操纵大写/小写等.
	 * 
	 * @param viewName 原始视图名称, 由URL路径指示
	 * 
	 * @return 要使用的完整视图名称
	 */
	protected String postProcessViewName(String viewName) {
		return getPrefix() + viewName + getSuffix();
	}

}
