package org.springframework.web.servlet.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * 解析包含版本字符串的请求路径, 该版本字符串可用作HTTP缓存策略的一部分,
 * 其中资源将长期 (e.g. 1 年) 缓存, 直到版本(即URL)更改.
 *
 * <p>存在不同的版本控制策略, 并且必须使用一个或多个此类策略以及路径映射配置此解析器, 以指示哪个策略适用于哪些资源.
 *
 * <p>{@code ContentVersionStrategy}是一个很好的默认选择, 除非它无法使用.
 * 最值得注意的是, {@code ContentVersionStrategy}无法与JavaScript模块加载器结合使用.
 * 对于这种情况, {@code FixedVersionStrategy}是更好的选择.
 *
 * <p>请注意, 使用此解析器提供CSS文件意味着还应使用{@link CssLinkResourceTransformer}来修改CSS文件中的链接,
 * 以包含此解析器生成的相应版本.
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	private AntPathMatcher pathMatcher = new AntPathMatcher();

	/** 路径模式 -> VersionStrategy */
	private final Map<String, VersionStrategy> versionStrategyMap = new LinkedHashMap<String, VersionStrategy>();


	/**
	 * 将URL路径作为键, 将{@code VersionStrategy}作为值.
	 * <p>支持直接URL匹配和Ant样式模式匹配.
	 * 有关语法详细信息, 请参阅{@link org.springframework.util.AntPathMatcher} javadoc.
	 * 
	 * @param map 将URL作为键, 将版本策略作为值
	 */
	public void setStrategyMap(Map<String, VersionStrategy> map) {
		this.versionStrategyMap.clear();
		this.versionStrategyMap.putAll(map);
	}

	/**
	 * 路径模式作为键, 版本策略作为值.
	 */
	public Map<String, VersionStrategy> getStrategyMap() {
		return this.versionStrategyMap;
	}

	/**
	 * 在与给定路径模式匹配的资源URL中插入基于内容的版本.
	 * 版本是根据文件的内容计算的, e.g. {@code "css/main-e36d2e05253c6c7085a91522ce43a0b4.css"}.
	 * 这是一个很好的默认策略, 除非它不能, 例如在使用JavaScript模块加载器时,
	 * 使用{@link #addFixedVersionStrategy}代替服务JavaScript文件.
	 * 
	 * @param pathPatterns 一个或多个资源URL路径模式, 相对于使用资源处理器配置的模式
	 * 
	 * @return 链式方法调用的当前实例
	 */
	public VersionResourceResolver addContentVersionStrategy(String... pathPatterns) {
		addVersionStrategy(new ContentVersionStrategy(), pathPatterns);
		return this;
	}

	/**
	 * 与给定路径模式匹配的资源URL中插入固定的基于前缀的版本, 例如: <code>"{version}/js/main.js"</code>.
	 * 使用JavaScript模块加载器时, 这很有用 (与基于内容的版本相比).
	 * <p>版本可以是随机数, 当前日期, 或从git commit sha, 属性文件或环境变量获取的值,
	 * 并在配置中使用SpEL表达式设置 (e.g. 请参阅Java配置中的{@code @Value}).
	 * <p>如果尚未完成, 还将配置给定{@code pathPatterns}的变体, 前缀为{{@code version}.
	 * 例如, 添加{@code "/js/**"}路径模式也会使用{@code "v1.0.0"}自动配置{@code "/v1.0.0/js/**"},
	 * {@code version}字符串作为参数给出.
	 * 
	 * @param version 版本字符串
	 * @param pathPatterns 一个或多个资源URL路径模式, 相对于使用资源处理器配置的模式
	 * 
	 * @return 链式方法调用的当前实例
	 */
	public VersionResourceResolver addFixedVersionStrategy(String version, String... pathPatterns) {
		List<String> patternsList = Arrays.asList(pathPatterns);
		List<String> prefixedPatterns = new ArrayList<String>(pathPatterns.length);
		String versionPrefix = "/" + version;
		for (String pattern : patternsList) {
			prefixedPatterns.add(pattern);
			if (!pattern.startsWith(versionPrefix) && !patternsList.contains(versionPrefix + pattern)) {
				prefixedPatterns.add(versionPrefix + pattern);
			}
		}
		return addVersionStrategy(new FixedVersionStrategy(version), StringUtils.toStringArray(prefixedPatterns));
	}

	/**
	 * 注册自定义VersionStrategy以应用于与给定路径模式匹配的资源URL.
	 * 
	 * @param strategy 自定义策略
	 * @param pathPatterns 一个或多个资源URL路径模式, 相对于使用资源处理器配置的模式
	 * 
	 * @return 链式方法调用的当前实例
	 */
	public VersionResourceResolver addVersionStrategy(VersionStrategy strategy, String... pathPatterns) {
		for (String pattern : pathPatterns) {
			getStrategyMap().put(pattern, strategy);
		}
		return this;
	}


	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved != null) {
			return resolved;
		}

		VersionStrategy versionStrategy = getStrategyForPath(requestPath);
		if (versionStrategy == null) {
			return null;
		}

		String candidateVersion = versionStrategy.extractVersion(requestPath);
		if (StringUtils.isEmpty(candidateVersion)) {
			if (logger.isTraceEnabled()) {
				logger.trace("No version found in path \"" + requestPath + "\"");
			}
			return null;
		}

		String simplePath = versionStrategy.removeVersion(requestPath, candidateVersion);
		if (logger.isTraceEnabled()) {
			logger.trace("Extracted version from path, re-resolving without version: \"" + simplePath + "\"");
		}

		Resource baseResource = chain.resolveResource(request, simplePath, locations);
		if (baseResource == null) {
			return null;
		}

		String actualVersion = versionStrategy.getResourceVersion(baseResource);
		if (candidateVersion.equals(actualVersion)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Resource matches extracted version [" + candidateVersion + "]");
			}
			return new FileNameVersionedResource(baseResource, candidateVersion);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Potential resource found for \"" + requestPath + "\", but version [" +
						candidateVersion + "] does not match");
			}
			return null;
		}
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
		String baseUrl = chain.resolveUrlPath(resourceUrlPath, locations);
		if (StringUtils.hasText(baseUrl)) {
			VersionStrategy versionStrategy = getStrategyForPath(resourceUrlPath);
			if (versionStrategy == null) {
				return baseUrl;
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Getting the original resource to determine version for path \"" + resourceUrlPath + "\"");
			}
			Resource resource = chain.resolveResource(null, baseUrl, locations);
			String version = versionStrategy.getResourceVersion(resource);
			if (logger.isTraceEnabled()) {
				logger.trace("Determined version [" + version + "] for " + resource);
			}
			return versionStrategy.addVersion(baseUrl, version);
		}
		return baseUrl;
	}

	/**
	 * 查找用于所请求资源的请求路径的{@code VersionStrategy}.
	 * 
	 * @return {@code VersionStrategy}的实例, 如果没有匹配该请求路径, 则返回null
	 */
	protected VersionStrategy getStrategyForPath(String requestPath) {
		String path = "/".concat(requestPath);
		List<String> matchingPatterns = new ArrayList<String>();
		for (String pattern : this.versionStrategyMap.keySet()) {
			if (this.pathMatcher.match(pattern, path)) {
				matchingPatterns.add(pattern);
			}
		}
		if (!matchingPatterns.isEmpty()) {
			Comparator<String> comparator = this.pathMatcher.getPatternComparator(path);
			Collections.sort(matchingPatterns, comparator);
			return this.versionStrategyMap.get(matchingPatterns.get(0));
		}
		return null;
	}


	private class FileNameVersionedResource extends AbstractResource implements VersionedResource {

		private final Resource original;

		private final String version;

		public FileNameVersionedResource(Resource original, String version) {
			this.original = original;
			this.version = version;
		}

		@Override
		public boolean exists() {
			return this.original.exists();
		}

		@Override
		public boolean isReadable() {
			return this.original.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.original.isOpen();
		}

		@Override
		public URL getURL() throws IOException {
			return this.original.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.original.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.original.getFile();
		}

		@Override
		public String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public long contentLength() throws IOException {
			return this.original.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.original.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.original.createRelative(relativePath);
		}

		@Override
		public String getDescription() {
			return this.original.getDescription();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.original.getInputStream();
		}

		@Override
		public String getVersion() {
			return this.version;
		}
	}
}
