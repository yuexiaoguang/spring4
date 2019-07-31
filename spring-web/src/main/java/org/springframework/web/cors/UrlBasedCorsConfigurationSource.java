package org.springframework.web.cors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

/**
 * 根据路径模式上映射的{@link CorsConfiguration}集合提供每个请求{@link CorsConfiguration}实例.
 *
 * <p>支持精确路径映射URI (例如{@code "/admin"})以及Ant样式路径模式 (例如{@code "/admin/**"}).
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {

	private final Map<String, CorsConfiguration> corsConfigurations = new LinkedHashMap<String, CorsConfiguration>();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 设置使用已注册的URL模式匹配URL路径的PathMatcher实现. 默认AntPathMatcher.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 设置URL查找是否应始终使用当前servlet上下文中的完整路径.
	 * 否则, 如果适用, 则使用当前servlet映射中的路径 (即, 在web.xml中的".../*"映射的情况下).
	 * <p>默认"false".
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 设置是否应对上下文路径和请求URI进行URL解码.
	 * 与servlet路径相比, Servlet API都返回<i>未解码的</i>.
	 * <p>根据Servlet规范 (ISO-8859-1)使用请求编码或默认编码.
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * 设置是否应从请求URI中删除";" (分号)内容.
	 * <p>默认{@code true}.
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * 设置用于解析查找路径的UrlPathHelper.
	 * <p>使用此选项可以使用自定义子类覆盖默认的UrlPathHelper.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 根据URL模式设置CORS配置.
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		this.corsConfigurations.clear();
		if (corsConfigurations != null) {
			this.corsConfigurations.putAll(corsConfigurations);
		}
	}

	/**
	 * 获取CORS配置.
	 */
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		return Collections.unmodifiableMap(this.corsConfigurations);
	}

	/**
	 * 为指定的路径模式注册{@link CorsConfiguration}.
	 */
	public void registerCorsConfiguration(String path, CorsConfiguration config) {
		this.corsConfigurations.put(path, config);
	}


	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		for (Map.Entry<String, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (this.pathMatcher.match(entry.getKey(), lookupPath)) {
				return entry.getValue();
			}
		}
		return null;
	}

}
