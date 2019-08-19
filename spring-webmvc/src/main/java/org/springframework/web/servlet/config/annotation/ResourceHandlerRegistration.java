package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.http.CacheControl;
import org.springframework.util.Assert;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * 封装创建资源处理器所需的信息.
 */
public class ResourceHandlerRegistration {

	private final String[] pathPatterns;

	private final List<String> locationValues = new ArrayList<String>();

	private Integer cachePeriod;

	private CacheControl cacheControl;

	private ResourceChainRegistration resourceChainRegistration;


	/**
	 * @param pathPatterns 一个或多个资源URL路径模式
	 */
	public ResourceHandlerRegistration(String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
		this.pathPatterns = pathPatterns;
	}


	/**
	 * 添加一个或多个资源位置以从中提供静态内容.
	 * 每个位置都必须指向一个有效的目录.
	 * 可以将多个位置指定为以逗号分隔的列表, 并按指定的顺序检查给定资源的位置.
	 * <p>例如, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}}
	 * 允许从Web应用程序根目录和类路径上的任何JAR提供资源,
	 * 该JAR包含{@code /META-INF/public-web-resources/}目录, 其中Web应用程序根目录中的资源优先.
	 * <p>对于{@link org.springframework.core.io.UrlResource 基于URL的资源} (e.g. files, HTTP URLs, etc),
	 * 此方法支持一个特殊的前缀来指示与URL关联的字符集, 以便附加到它的相对路径可以正确编码,
	 * e.g. {@code [charset=Windows-31J]http://example.org/path}.
	 * 
	 * @return 相同的{@link ResourceHandlerRegistration}实例, 用于链式方法调用
	 */
	public ResourceHandlerRegistration addResourceLocations(String... resourceLocations) {
		this.locationValues.addAll(Arrays.asList(resourceLocations));
		return this;
	}

	/**
	 * 指定资源处理器所服务资源的缓存周期, 以秒为单位.
	 * 默认不发送任何缓存header, 而是仅依赖于last-modified时间戳.
	 * 设置为0以发送禁止缓存的缓存header, 或设置为正数秒以发送具有给定max-age值的缓存header.
	 * 
	 * @param cachePeriod 缓存资源的时间, 以秒为单位
	 * 
	 * @return 相同的{@link ResourceHandlerRegistration}实例, 用于链式方法调用
	 */
	public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
		return this;
	}

	/**
	 * 指定资源处理器应使用的{@link org.springframework.http.CacheControl}.
	 * <p>在此处设置自定义值将覆盖{@link #setCachePeriod}的配置集.
	 * 
	 * @param cacheControl 要使用的CacheControl配置
	 * 
	 * @return 相同的{@link ResourceHandlerRegistration}实例, 用于链式方法调用
	 */
	public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
		return this;
	}

	/**
	 * 配置要使用的资源解析器和转换器链.
	 * 例如, 这可以用于将版本策略应用于资源URL.
	 * <p>如果未调用此方法, 则默认仅使用简单的{@link PathResourceResolver}以匹配配置位置下的资源的URL路径.
	 * 
	 * @param cacheResources 是否缓存资源解析的结果;
	 * 建议生产环境中设置为"true" ("false"用于开发, 尤其是在应用版本策略时)
	 * 
	 * @return 相同的{@link ResourceHandlerRegistration}实例, 用于链式方法调用
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
		return this.resourceChainRegistration;
	}

	/**
	 * 配置要使用的资源解析器和转换器链.
	 * 例如, 这可以用于将版本策略应用于资源URL.
	 * <p>如果未调用此方法, 则默认情况下仅使用简单的{@link PathResourceResolver}以匹配配置位置下的资源的URL路径.
	 * 
	 * @param cacheResources 是否缓存资源解析的结果;
	 * 建议生产环境中设置为"true" ("false"用于开发, 尤其是在应用版本策略时)
	 * @param cache 用于存储已解析和已转换资源的缓存;
	 * 默认使用{@link org.springframework.cache.concurrent.ConcurrentMapCache}.
	 * 由于Resource不可序列化并且可以依赖于应用程序主机, 因此不应使用分布式缓存, 而应使用内存缓存.
	 * 
	 * @return 相同的{@link ResourceHandlerRegistration}实例, 用于链式方法调用
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
		return this.resourceChainRegistration;
	}


	/**
	 * 返回资源处理器的URL路径模式.
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	/**
	 * 返回{@link ResourceHttpRequestHandler}实例.
	 */
	protected ResourceHttpRequestHandler getRequestHandler() {
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		if (this.resourceChainRegistration != null) {
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}
		handler.setLocationValues(this.locationValues);
		if (this.cacheControl != null) {
			handler.setCacheControl(this.cacheControl);
		}
		else if (this.cachePeriod != null) {
			handler.setCacheSeconds(this.cachePeriod);
		}
		return handler;
	}

}
