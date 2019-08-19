package org.springframework.web.servlet.mvc.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.util.WebUtils;

/**
 * {@link RequestMappingInfo}定义请求和处理器方法之间的映射.
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	private static final Method HTTP_OPTIONS_HANDLE_METHOD;

	static {
		try {
			HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
		}
		catch (NoSuchMethodException ex) {
			// Should never happen
			throw new IllegalStateException("Failed to retrieve internal handler method for HTTP OPTIONS", ex);
		}
	}


	protected RequestMappingInfoHandlerMapping() {
		setHandlerMethodMappingNamingStrategy(new RequestMappingInfoHandlerMethodMappingNamingStrategy());
	}


	/**
	 * 获取与此{@link RequestMappingInfo}关联的URL路径模式.
	 */
	@Override
	protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
		return info.getPatternsCondition().getPatterns();
	}

	/**
	 * 检查给定的RequestMappingInfo是否与当前请求匹配,
	 * 并返回具有与当前请求匹配的条件的 (可能是新的)实例 -- 例如, 使用URL模式的子集.
	 * 
	 * @return 匹配时的信息; 或者{@code null}.
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
		return info.getMatchingCondition(request);
	}

	/**
	 * 提供比较器以对与请求匹配的RequestMappingInfos进行排序.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final HttpServletRequest request) {
		return new Comparator<RequestMappingInfo>() {
			@Override
			public int compare(RequestMappingInfo info1, RequestMappingInfo info2) {
				return info1.compareTo(info2, request);
			}
		};
	}

	/**
	 * 在请求中公开URI模板变量, 矩阵变量, 和可生成的媒体类型.
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
		super.handleMatch(info, lookupPath, request);

		String bestPattern;
		Map<String, String> uriVariables;
		Map<String, String> decodedUriVariables;

		Set<String> patterns = info.getPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			bestPattern = lookupPath;
			uriVariables = Collections.emptyMap();
			decodedUriVariables = Collections.emptyMap();
		}
		else {
			bestPattern = patterns.iterator().next();
			uriVariables = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
			decodedUriVariables = getUrlPathHelper().decodePathVariables(request, uriVariables);
		}

		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodedUriVariables);

		if (isMatrixVariableContentAvailable()) {
			Map<String, MultiValueMap<String, String>> matrixVars = extractMatrixVariables(request, uriVariables);
			request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, matrixVars);
		}

		if (!info.getProducesCondition().getProducibleMediaTypes().isEmpty()) {
			Set<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
			request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
		}
	}

	private boolean isMatrixVariableContentAvailable() {
		return !getUrlPathHelper().shouldRemoveSemicolonContent();
	}

	private Map<String, MultiValueMap<String, String>> extractMatrixVariables(
			HttpServletRequest request, Map<String, String> uriVariables) {

		Map<String, MultiValueMap<String, String>> result = new LinkedHashMap<String, MultiValueMap<String, String>>();
		for (Map.Entry<String, String> uriVar : uriVariables.entrySet()) {
			String uriVarValue = uriVar.getValue();

			int equalsIndex = uriVarValue.indexOf('=');
			if (equalsIndex == -1) {
				continue;
			}

			String matrixVariables;

			int semicolonIndex = uriVarValue.indexOf(';');
			if ((semicolonIndex == -1) || (semicolonIndex == 0) || (equalsIndex < semicolonIndex)) {
				matrixVariables = uriVarValue;
			}
			else {
				matrixVariables = uriVarValue.substring(semicolonIndex + 1);
				uriVariables.put(uriVar.getKey(), uriVarValue.substring(0, semicolonIndex));
			}

			MultiValueMap<String, String> vars = WebUtils.parseMatrixVariables(matrixVariables);
			result.put(uriVar.getKey(), getUrlPathHelper().decodeMatrixVariables(request, vars));
		}
		return result;
	}

	/**
	 * 再次迭代所有RequestMappingInfo, 查看按URL是否匹配, 并根据不匹配的内容引发异常.
	 * 
	 * @throws HttpRequestMethodNotSupportedException 如果有匹配的URL, 但HTTP方法不匹配
	 * @throws HttpMediaTypeNotAcceptableException 如果有URL匹配, 但不符合消耗/生产的媒体类型
	 */
	@Override
	protected HandlerMethod handleNoMatch(
			Set<RequestMappingInfo> infos, String lookupPath, HttpServletRequest request) throws ServletException {

		PartialMatchHelper helper = new PartialMatchHelper(infos, request);
		if (helper.isEmpty()) {
			return null;
		}

		if (helper.hasMethodsMismatch()) {
			Set<String> methods = helper.getAllowedMethods();
			if (HttpMethod.OPTIONS.matches(request.getMethod())) {
				HttpOptionsHandler handler = new HttpOptionsHandler(methods);
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), methods);
		}

		if (helper.hasConsumesMismatch()) {
			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
			MediaType contentType = null;
			if (StringUtils.hasLength(request.getContentType())) {
				try {
					contentType = MediaType.parseMediaType(request.getContentType());
				}
				catch (InvalidMediaTypeException ex) {
					throw new HttpMediaTypeNotSupportedException(ex.getMessage());
				}
			}
			throw new HttpMediaTypeNotSupportedException(contentType, new ArrayList<MediaType>(mediaTypes));
		}

		if (helper.hasProducesMismatch()) {
			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
			throw new HttpMediaTypeNotAcceptableException(new ArrayList<MediaType>(mediaTypes));
		}

		if (helper.hasParamsMismatch()) {
			List<String[]> conditions = helper.getParamConditions();
			throw new UnsatisfiedServletRequestParameterException(conditions, request.getParameterMap());
		}

		return null;
	}


	/**
	 * 聚合所有部分匹配并公开检查它们的方法.
	 */
	private static class PartialMatchHelper {

		private final List<PartialMatch> partialMatches = new ArrayList<PartialMatch>();

		public PartialMatchHelper(Set<RequestMappingInfo> infos, HttpServletRequest request) {
			for (RequestMappingInfo info : infos) {
				if (info.getPatternsCondition().getMatchingCondition(request) != null) {
					this.partialMatches.add(new PartialMatch(info, request));
				}
			}
		}

		/**
		 * 是否有部分匹配.
		 */
		public boolean isEmpty() {
			return this.partialMatches.isEmpty();
		}

		/**
		 * "methods"的任何部分匹配?
		 */
		public boolean hasMethodsMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasMethodsMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * "methods" 和 "consumes"的任何部分匹配?
		 */
		public boolean hasConsumesMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasConsumesMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * "methods", "consumes", 和 "produces"的任何部分匹配?
		 */
		public boolean hasProducesMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasProducesMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * "methods", "consumes", "produces", 和 "params"的任何部分匹配?
		 */
		public boolean hasParamsMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasParamsMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * 返回声明的HTTP方法.
		 */
		public Set<String> getAllowedMethods() {
			Set<String> result = new LinkedHashSet<String>();
			for (PartialMatch match : this.partialMatches) {
				for (RequestMethod method : match.getInfo().getMethodsCondition().getMethods()) {
					result.add(method.name());
				}
			}
			return result;
		}

		/**
		 * 返回声明的"consumable"类型, 但仅限于那些也符合"methods"条件的类型.
		 */
		public Set<MediaType> getConsumableMediaTypes() {
			Set<MediaType> result = new LinkedHashSet<MediaType>();
			for (PartialMatch match : this.partialMatches) {
				if (match.hasMethodsMatch()) {
					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
				}
			}
			return result;
		}

		/**
		 * 返回声明的"producible"类型, 但仅限于那些也符合"methods" 和 "consumes"条件的类型.
		 */
		public Set<MediaType> getProducibleMediaTypes() {
			Set<MediaType> result = new LinkedHashSet<MediaType>();
			for (PartialMatch match : this.partialMatches) {
				if (match.hasConsumesMatch()) {
					result.addAll(match.getInfo().getProducesCondition().getProducibleMediaTypes());
				}
			}
			return result;
		}

		/**
		 * 返回声明的"params"条件, 但仅限于那些也符合"methods", "consumes", 和"params"条件的条件.
		 */
		public List<String[]> getParamConditions() {
			List<String[]> result = new ArrayList<String[]>();
			for (PartialMatch match : this.partialMatches) {
				if (match.hasProducesMatch()) {
					Set<NameValueExpression<String>> set = match.getInfo().getParamsCondition().getExpressions();
					if (!CollectionUtils.isEmpty(set)) {
						int i = 0;
						String[] array = new String[set.size()];
						for (NameValueExpression<String> expression : set) {
							array[i++] = expression.toString();
						}
						result.add(array);
					}
				}
			}
			return result;
		}


		/**
		 * RequestMappingInfo的容器, 至少与URL路径匹配.
		 */
		private static class PartialMatch {

			private final RequestMappingInfo info;

			private final boolean methodsMatch;

			private final boolean consumesMatch;

			private final boolean producesMatch;

			private final boolean paramsMatch;

			/**
			 * @param info 与URL路径匹配的RequestMappingInfo
			 * @param request 当前的请求
			 */
			public PartialMatch(RequestMappingInfo info, HttpServletRequest request) {
				this.info = info;
				this.methodsMatch = (info.getMethodsCondition().getMatchingCondition(request) != null);
				this.consumesMatch = (info.getConsumesCondition().getMatchingCondition(request) != null);
				this.producesMatch = (info.getProducesCondition().getMatchingCondition(request) != null);
				this.paramsMatch = (info.getParamsCondition().getMatchingCondition(request) != null);
			}

			public RequestMappingInfo getInfo() {
				return this.info;
			}

			public boolean hasMethodsMatch() {
				return this.methodsMatch;
			}

			public boolean hasConsumesMatch() {
				return (hasMethodsMatch() && this.consumesMatch);
			}

			public boolean hasProducesMatch() {
				return (hasConsumesMatch() && this.producesMatch);
			}

			public boolean hasParamsMatch() {
				return (hasProducesMatch() && this.paramsMatch);
			}

			@Override
			public String toString() {
				return this.info.toString();
			}
		}
	}


	/**
	 * HTTP OPTIONS的默认处理器.
	 */
	private static class HttpOptionsHandler {

		private final HttpHeaders headers = new HttpHeaders();

		public HttpOptionsHandler(Set<String> declaredMethods) {
			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
		}

		private static Set<HttpMethod> initAllowedHttpMethods(Set<String> declaredMethods) {
			Set<HttpMethod> result = new LinkedHashSet<HttpMethod>(declaredMethods.size());
			if (declaredMethods.isEmpty()) {
				for (HttpMethod method : HttpMethod.values()) {
					if (method != HttpMethod.TRACE) {
						result.add(method);
					}
				}
			}
			else {
				for (String method : declaredMethods) {
					HttpMethod httpMethod = HttpMethod.valueOf(method);
					result.add(httpMethod);
					if (httpMethod == HttpMethod.GET) {
						result.add(HttpMethod.HEAD);
					}
				}
			}
			return result;
		}

		public HttpHeaders handle() {
			return this.headers;
		}
	}
}
