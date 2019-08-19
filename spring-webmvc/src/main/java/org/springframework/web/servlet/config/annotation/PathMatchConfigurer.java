package org.springframework.web.servlet.config.annotation;

import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

/**
 * 帮助配置HandlerMappings路径匹配选项, 例如尾部斜杠匹配, 后缀注册, 路径匹配器和路径帮助器.
 *
 * <p>共享配置的路径匹配器和路径帮助器实例:
 * <ul>
 * <li>RequestMappings</li>
 * <li>ViewControllerMappings</li>
 * <li>ResourcesMappings</li>
 * </ul>
 */
public class PathMatchConfigurer {

	private Boolean suffixPatternMatch;

	private Boolean trailingSlashMatch;

	private Boolean registeredSuffixPatternMatch;

	private UrlPathHelper urlPathHelper;

	private PathMatcher pathMatcher;


	/**
	 * 在将模式与请求匹配时是否使用后缀模式匹配 (".*").
	 * 如果启用, 映射到"/users"的方法也匹配"/users.*".
	 * <p>默认为{@code true}.
	 */
	public PathMatchConfigurer setUseSuffixPatternMatch(Boolean suffixPatternMatch) {
		this.suffixPatternMatch = suffixPatternMatch;
		return this;
	}

	/**
	 * 是否匹配URL而不管是否存在尾部斜杠.
	 * 如果启用, 映射到"/users"的方法也匹配"/users/".
	 * <p>默认为{@code true}.
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * 后缀模式匹配是否仅适用于
	 * {@link WebMvcConfigurer#configureContentNegotiation 配置内容协商}时显式注册的路径扩展.
	 * 通常建议这样做以减少歧义, 并避免诸如路径中的"."之类的问题.
	 * <p>默认为"false".
	 */
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(Boolean registeredSuffixPatternMatch) {
		this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		return this;
	}

	/**
	 * 设置用于查找路径的解析的UrlPathHelper.
	 * <p>使用此选项可以使用自定义子类覆盖默认的UrlPathHelper,
	 * 或者在多个HandlerMappings和MethodNameResolvers之间共享常用的UrlPathHelper设置.
	 */
	public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
		return this;
	}

	/**
	 * 设置PathMatcher实现, 用于使用注册的URL模式匹配URL路径. 默认为AntPathMatcher.
	 */
	public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}


	public Boolean isUseSuffixPatternMatch() {
		return this.suffixPatternMatch;
	}

	public Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	public Boolean isUseRegisteredSuffixPatternMatch() {
		return this.registeredSuffixPatternMatch;
	}

	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}
}
