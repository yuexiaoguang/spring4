package org.springframework.web.servlet.mvc.method;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link RequestCondition}包含以下其他条件:
 * <ol>
 * <li>{@link PatternsRequestCondition}
 * <li>{@link RequestMethodsRequestCondition}
 * <li>{@link ParamsRequestCondition}
 * <li>{@link HeadersRequestCondition}
 * <li>{@link ConsumesRequestCondition}
 * <li>{@link ProducesRequestCondition}
 * <li>{@code RequestCondition} (可选的自定义请求条件)
 * </ol>
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	private final String name;

	private final PatternsRequestCondition patternsCondition;

	private final RequestMethodsRequestCondition methodsCondition;

	private final ParamsRequestCondition paramsCondition;

	private final HeadersRequestCondition headersCondition;

	private final ConsumesRequestCondition consumesCondition;

	private final ProducesRequestCondition producesCondition;

	private final RequestConditionHolder customConditionHolder;


	public RequestMappingInfo(String name, PatternsRequestCondition patterns, RequestMethodsRequestCondition methods,
			ParamsRequestCondition params, HeadersRequestCondition headers, ConsumesRequestCondition consumes,
			ProducesRequestCondition produces, RequestCondition<?> custom) {

		this.name = (StringUtils.hasText(name) ? name : null);
		this.patternsCondition = (patterns != null ? patterns : new PatternsRequestCondition());
		this.methodsCondition = (methods != null ? methods : new RequestMethodsRequestCondition());
		this.paramsCondition = (params != null ? params : new ParamsRequestCondition());
		this.headersCondition = (headers != null ? headers : new HeadersRequestCondition());
		this.consumesCondition = (consumes != null ? consumes : new ConsumesRequestCondition());
		this.producesCondition = (produces != null ? produces : new ProducesRequestCondition());
		this.customConditionHolder = new RequestConditionHolder(custom);
	}

	/**
	 * 使用给定的请求条件创建新实例.
	 */
	public RequestMappingInfo(PatternsRequestCondition patterns, RequestMethodsRequestCondition methods,
			ParamsRequestCondition params, HeadersRequestCondition headers, ConsumesRequestCondition consumes,
			ProducesRequestCondition produces, RequestCondition<?> custom) {

		this(null, patterns, methods, params, headers, consumes, produces, custom);
	}

	/**
	 * 使用给定的自定义请求条件重新创建RequestMappingInfo.
	 */
	public RequestMappingInfo(RequestMappingInfo info, RequestCondition<?> customRequestCondition) {
		this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition,
				info.consumesCondition, info.producesCondition, customRequestCondition);
	}


	/**
	 * 返回此映射的名称, 或{@code null}.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回此{@link RequestMappingInfo}的URL模式; 或具有0个模式的实例 (never {@code null}).
	 */
	public PatternsRequestCondition getPatternsCondition() {
		return this.patternsCondition;
	}

	/**
	 * 返回此{@link RequestMappingInfo}的HTTP请求方法; 或具有0个请求方法的实例 (never {@code null}).
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return this.methodsCondition;
	}

	/**
	 * 返回此{@link RequestMappingInfo}的"parameters"条件; 或具有0个参数表达式的实例 (never {@code null}).
	 */
	public ParamsRequestCondition getParamsCondition() {
		return this.paramsCondition;
	}

	/**
	 * 返回此{@link RequestMappingInfo}的"headers"条件; 或者具有0个header表达式的实例 (never {@code null}).
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return this.headersCondition;
	}

	/**
	 * 返回此{@link RequestMappingInfo}的"consumes"条件; 或者具有0个consumes表达式的实例 (never {@code null}).
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return this.consumesCondition;
	}

	/**
	 * 返回此{@link RequestMappingInfo}的"produces"条件; 或者具有0个produces表达式的实例 (never {@code null}).
	 */
	public ProducesRequestCondition getProducesCondition() {
		return this.producesCondition;
	}

	/**
	 * 返回此{@link RequestMappingInfo}的"custom"条件, 或{@code null}.
	 */
	public RequestCondition<?> getCustomCondition() {
		return this.customConditionHolder.getCondition();
	}


	/**
	 * 将"this"请求映射信息 (i.e. 当前实例) 与另一个请求映射信息实例相结合.
	 * <p>Example: 组合类型和方法级别的请求映射.
	 * 
	 * @return 一个新的请求映射信息实例; never {@code null}
	 */
	@Override
	public RequestMappingInfo combine(RequestMappingInfo other) {
		String name = combineNames(other);
		PatternsRequestCondition patterns = this.patternsCondition.combine(other.patternsCondition);
		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		return new RequestMappingInfo(name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition());
	}

	private String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			String separator = RequestMappingInfoHandlerMethodMappingNamingStrategy.SEPARATOR;
			return this.name + separator + other.name;
		}
		else if (this.name != null) {
			return this.name;
		}
		else {
			return other.name;
		}
	}

	/**
	 * 检查此请求映射信息中的所有条件是否与提供的请求匹配, 并返回具有针对当前请求定制的条件的潜在新请求映射信息.
	 * <p>例如, 返回的实例可能包含与当前请求匹配的URL模式子集, 并且最佳匹配模式排在最上面.
	 * 
	 * @return 在所有条件匹配的情况下的新实例; 或{@code null}
	 */
	@Override
	public RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(request);
		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(request);
		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(request);
		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(request);
		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(request);

		if (methods == null || params == null || headers == null || consumes == null || produces == null) {
			return null;
		}

		PatternsRequestCondition patterns = this.patternsCondition.getMatchingCondition(request);
		if (patterns == null) {
			return null;
		}

		RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(request);
		if (custom == null) {
			return null;
		}

		return new RequestMappingInfo(this.name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition());
	}

	/**
	 * 将"this"信息 (i.e. 当前实例) 与请求上下文中的另一个信息进行比较.
	 * <p>Note: 假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的,
	 * 以确保它们具有与当前请求相关的内容的条件.
	 */
	@Override
	public int compareTo(RequestMappingInfo other, HttpServletRequest request) {
		int result;
		// 自动 vs 显式的HTTP HEAD映射
		if (HttpMethod.HEAD.matches(request.getMethod())) {
			result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
			if (result != 0) {
				return result;
			}
		}
		result = this.patternsCondition.compareTo(other.getPatternsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.paramsCondition.compareTo(other.getParamsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.headersCondition.compareTo(other.getHeadersCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.consumesCondition.compareTo(other.getConsumesCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.producesCondition.compareTo(other.getProducesCondition(), request);
		if (result != 0) {
			return result;
		}
		// 隐式 (无方法) vs 显式HTTP方法映射
		result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.customConditionHolder.compareTo(other.customConditionHolder, request);
		if (result != 0) {
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RequestMappingInfo)) {
			return false;
		}
		RequestMappingInfo otherInfo = (RequestMappingInfo) other;
		return (this.patternsCondition.equals(otherInfo.patternsCondition) &&
				this.methodsCondition.equals(otherInfo.methodsCondition) &&
				this.paramsCondition.equals(otherInfo.paramsCondition) &&
				this.headersCondition.equals(otherInfo.headersCondition) &&
				this.consumesCondition.equals(otherInfo.consumesCondition) &&
				this.producesCondition.equals(otherInfo.producesCondition) &&
				this.customConditionHolder.equals(otherInfo.customConditionHolder));
	}

	@Override
	public int hashCode() {
		return (this.patternsCondition.hashCode() * 31 +  // primary differentiation
				this.methodsCondition.hashCode() + this.paramsCondition.hashCode() +
				this.headersCondition.hashCode() + this.consumesCondition.hashCode() +
				this.producesCondition.hashCode() + this.customConditionHolder.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		builder.append(this.patternsCondition);
		if (!this.methodsCondition.isEmpty()) {
			builder.append(",methods=").append(this.methodsCondition);
		}
		if (!this.paramsCondition.isEmpty()) {
			builder.append(",params=").append(this.paramsCondition);
		}
		if (!this.headersCondition.isEmpty()) {
			builder.append(",headers=").append(this.headersCondition);
		}
		if (!this.consumesCondition.isEmpty()) {
			builder.append(",consumes=").append(this.consumesCondition);
		}
		if (!this.producesCondition.isEmpty()) {
			builder.append(",produces=").append(this.producesCondition);
		}
		if (!this.customConditionHolder.isEmpty()) {
			builder.append(",custom=").append(this.customConditionHolder);
		}
		builder.append('}');
		return builder.toString();
	}


	/**
	 * 使用给定路径创建新的{@code RequestMappingInfo.Builder}.
	 * 
	 * @param paths 要使用的路径
	 */
	public static Builder paths(String... paths) {
		return new DefaultBuilder(paths);
	}


	/**
	 * 定义用于创建RequestMappingInfo的构建器.
	 */
	public interface Builder {

		/**
		 * 设置路径模式.
		 */
		Builder paths(String... paths);

		/**
		 * 设置请求方法条件.
		 */
		Builder methods(RequestMethod... methods);

		/**
		 * 设置请求参数条件.
		 */
		Builder params(String... params);

		/**
		 * 设置header条件.
		 * <p>默认未设置.
		 */
		Builder headers(String... headers);

		/**
		 * 设置 consumes条件.
		 */
		Builder consumes(String... consumes);

		/**
		 * 设置 produces条件.
		 */
		Builder produces(String... produces);

		/**
		 * 设置映射名称.
		 */
		Builder mappingName(String name);

		/**
		 * 设置要使用的自定义条件.
		 */
		Builder customCondition(RequestCondition<?> condition);

		/**
		 * 提供请求映射所需的其他配置.
		 */
		Builder options(BuilderConfiguration options);

		/**
		 * 构建RequestMappingInfo.
		 */
		RequestMappingInfo build();
	}


	private static class DefaultBuilder implements Builder {

		private String[] paths;

		private RequestMethod[] methods;

		private String[] params;

		private String[] headers;

		private String[] consumes;

		private String[] produces;

		private String mappingName;

		private RequestCondition<?> customCondition;

		private BuilderConfiguration options = new BuilderConfiguration();

		public DefaultBuilder(String... paths) {
			this.paths = paths;
		}

		@Override
		public Builder paths(String... paths) {
			this.paths = paths;
			return this;
		}

		@Override
		public DefaultBuilder methods(RequestMethod... methods) {
			this.methods = methods;
			return this;
		}

		@Override
		public DefaultBuilder params(String... params) {
			this.params = params;
			return this;
		}

		@Override
		public DefaultBuilder headers(String... headers) {
			this.headers = headers;
			return this;
		}

		@Override
		public DefaultBuilder consumes(String... consumes) {
			this.consumes = consumes;
			return this;
		}

		@Override
		public DefaultBuilder produces(String... produces) {
			this.produces = produces;
			return this;
		}

		@Override
		public DefaultBuilder mappingName(String name) {
			this.mappingName = name;
			return this;
		}

		@Override
		public DefaultBuilder customCondition(RequestCondition<?> condition) {
			this.customCondition = condition;
			return this;
		}

		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		@Override
		public RequestMappingInfo build() {
			ContentNegotiationManager manager = this.options.getContentNegotiationManager();

			PatternsRequestCondition patternsCondition = new PatternsRequestCondition(
					this.paths, this.options.getUrlPathHelper(), this.options.getPathMatcher(),
					this.options.useSuffixPatternMatch(), this.options.useTrailingSlashMatch(),
					this.options.getFileExtensions());

			return new RequestMappingInfo(this.mappingName, patternsCondition,
					new RequestMethodsRequestCondition(methods),
					new ParamsRequestCondition(this.params),
					new HeadersRequestCondition(this.headers),
					new ConsumesRequestCondition(this.consumes, this.headers),
					new ProducesRequestCondition(this.produces, this.headers, manager),
					this.customCondition);
		}
	}


	/**
	 * 用于请求映射目的的配置选项的容器.
	 * 创建RequestMappingInfo实例需要这样的配置, 但通常在所有RequestMappingInfo实例中使用.
	 */
	public static class BuilderConfiguration {

		private UrlPathHelper urlPathHelper;

		private PathMatcher pathMatcher;

		private boolean trailingSlashMatch = true;

		private boolean suffixPatternMatch = true;

		private boolean registeredSuffixPatternMatch = false;

		private ContentNegotiationManager contentNegotiationManager;

		/**
		 * @deprecated as of Spring 4.2.8, in favor of {@link #setUrlPathHelper}
		 */
		@Deprecated
		public void setPathHelper(UrlPathHelper pathHelper) {
			this.urlPathHelper = pathHelper;
		}

		/**
		 * 设置自定义UrlPathHelper以用于PatternsRequestCondition.
		 * <p>默认未设置.
		 */
		public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
			this.urlPathHelper = urlPathHelper;
		}

		/**
		 * 返回自定义UrlPathHelper以用于PatternsRequestCondition.
		 */
		public UrlPathHelper getUrlPathHelper() {
			return this.urlPathHelper;
		}

		/**
		 * 设置自定义PathMatcher以用于PatternsRequestCondition.
		 * <p>默认情况下, 此设置未设置.
		 */
		public void setPathMatcher(PathMatcher pathMatcher) {
			this.pathMatcher = pathMatcher;
		}

		/**
		 * 返回自定义PathMatcher以用于PatternsRequestCondition.
		 */
		public PathMatcher getPathMatcher() {
			return this.pathMatcher;
		}

		/**
		 * 设置是否在PatternsRequestCondition中应用尾部斜杠匹配.
		 * <p>默认为'true'.
		 */
		public void setTrailingSlashMatch(boolean trailingSlashMatch) {
			this.trailingSlashMatch = trailingSlashMatch;
		}

		/**
		 * 返回是否在PatternsRequestCondition中应用尾部斜杠匹配.
		 */
		public boolean useTrailingSlashMatch() {
			return this.trailingSlashMatch;
		}

		/**
		 * 设置是否在PatternsRequestCondition中应用后缀模式匹配.
		 * <p>默认为'true'.
		 */
		public void setSuffixPatternMatch(boolean suffixPatternMatch) {
			this.suffixPatternMatch = suffixPatternMatch;
		}

		/**
		 * 返回是否在PatternsRequestCondition中应用后缀模式匹配.
		 */
		public boolean useSuffixPatternMatch() {
			return this.suffixPatternMatch;
		}

		/**
		 * 设置后缀模式匹配是否应仅限于已注册的文件扩展名.
		 * 设置此属性还会设置{@code suffixPatternMatch=true},
		 * 并且还需要配置{@link #setContentNegotiationManager}以获取已注册的文件扩展名.
		 */
		public void setRegisteredSuffixPatternMatch(boolean registeredSuffixPatternMatch) {
			this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
			this.suffixPatternMatch = (registeredSuffixPatternMatch || this.suffixPatternMatch);
		}

		/**
		 * 返回后缀模式匹配是否应仅限于已注册的文件扩展名.
		 */
		public boolean useRegisteredSuffixPatternMatch() {
			return this.registeredSuffixPatternMatch;
		}

		/**
		 * 返回用于后缀模式匹配的文件扩展名.
		 * 如果{@code registeredSuffixPatternMatch=true}, 则从配置的{@code contentNegotiationManager}获取扩展名.
		 */
		public List<String> getFileExtensions() {
			if (useRegisteredSuffixPatternMatch() && this.contentNegotiationManager != null) {
				return this.contentNegotiationManager.getAllFileExtensions();
			}
			return null;
		}

		/**
		 * 设置ContentNegotiationManager以用于ProducesRequestCondition.
		 * <p>默认未设置.
		 */
		public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
			this.contentNegotiationManager = contentNegotiationManager;
		}

		/**
		 * 返回ContentNegotiationManager以用于ProducesRequestCondition.
		 */
		public ContentNegotiationManager getContentNegotiationManager() {
			return this.contentNegotiationManager;
		}
	}
}
