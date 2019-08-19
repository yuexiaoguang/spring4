package org.springframework.web.servlet.view;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link RequestToViewNameTranslator}, 它只是将传入请求的URI转换为视图名称.
 *
 * <p>可以在{@link org.springframework.web.servlet.DispatcherServlet}上下文中明确定义为{@code viewNameTranslator} bean.
 * 否则, 将使用普通的默认实例.
 *
 * <p>默认转换只是删除前导和尾部斜杠以及URI的文件扩展名, 并将结果作为视图名称返回,
 * 并添加配置的{@link #setPrefix 前缀}和{@link #setSuffix 后缀}.
 *
 * <p>可以分别使用{@link #setStripLeadingSlash stripLeadingSlash}和{@link #setStripExtension stripExtension}属性
 * 禁用前导斜杠和文件扩展名的剥离.
 *
 * <p>下面是一些请求到视图名称转换的示例.
 * <ul>
 * <li>{@code http://localhost:8080/gamecast/display.html} &raquo; {@code display}</li>
 * <li>{@code http://localhost:8080/gamecast/displayShoppingCart.html} &raquo; {@code displayShoppingCart}</li>
 * <li>{@code http://localhost:8080/gamecast/admin/index.html} &raquo; {@code admin/index}</li>
 * </ul>
 */
public class DefaultRequestToViewNameTranslator implements RequestToViewNameTranslator {

	private static final String SLASH = "/";


	private String prefix = "";

	private String suffix = "";

	private String separator = SLASH;

	private boolean stripLeadingSlash = true;

	private boolean stripTrailingSlash = true;

	private boolean stripExtension = true;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 设置生成的视图名称的前缀.
	 * 
	 * @param prefix 生成视图名称的前缀
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 设置生成的视图名称的后缀.
	 * 
	 * @param suffix 生成的视图名称的后缀
	 */
	public void setSuffix(String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 设置将替换'{@code /}'作为视图名称中的分隔符的值.
	 * 默认行为只是将 '{@code /}'作为分隔符.
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * 设置生成视图名称时是否应从URI中删除前导斜杠.
	 * 默认为"true".
	 */
	public void setStripLeadingSlash(boolean stripLeadingSlash) {
		this.stripLeadingSlash = stripLeadingSlash;
	}

	/**
	 * 设置生成视图名称时是否应从URI中删除尾部斜杠.
	 * 默认为"true".
	 */
	public void setStripTrailingSlash(boolean stripTrailingSlash) {
		this.stripTrailingSlash = stripTrailingSlash;
	}

	/**
	 * 设置生成视图名称时是否应从URI中删除文件扩展名.
	 * 默认为"true".
	 */
	public void setStripExtension(boolean stripExtension) {
		this.stripExtension = stripExtension;
	}

	/**
	 * 设置URL查找是否应始终使用当前servlet上下文中的完整路径.
	 * 否则, 如果适用, 则使用当前servlet映射中的路径 (i.e. 在web.xml中的".../*" servlet映射的情况下).
	 * 默认为"false".
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
	 * 设置是否应从请求URI中删除 ";" (分号) 内容.
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * 设置用于查找路径的解析的{@link org.springframework.web.util.UrlPathHelper}.
	 * <p>使用此选项可以使用自定义子类覆盖默认UrlPathHelper, 或者跨多个Web组件共享常用UrlPathHelper设置.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}


	/**
	 * 根据配置的参数将传入的{@link HttpServletRequest}的请求URI转换为视图名称.
	 */
	@Override
	public String getViewName(HttpServletRequest request) {
		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		return (this.prefix + transformPath(lookupPath) + this.suffix);
	}

	/**
	 * 转换请求URI (在webapp的上下文中) 剥离斜杠和扩展名, 并根据需要替换分隔符.
	 * 
	 * @param lookupPath 由UrlPathHelper确定的当前请求的查找路径
	 * 
	 * @return 转换后的路径, 如果需要, 可以删除斜杠和扩展名
	 */
	protected String transformPath(String lookupPath) {
		String path = lookupPath;
		if (this.stripLeadingSlash && path.startsWith(SLASH)) {
			path = path.substring(1);
		}
		if (this.stripTrailingSlash && path.endsWith(SLASH)) {
			path = path.substring(0, path.length() - 1);
		}
		if (this.stripExtension) {
			path = StringUtils.stripFilenameExtension(path);
		}
		if (!SLASH.equals(this.separator)) {
			path = StringUtils.replace(path, SLASH, this.separator);
		}
		return path;
	}

}
