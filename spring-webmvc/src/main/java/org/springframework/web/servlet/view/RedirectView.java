package org.springframework.web.servlet.view;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

/**
 * 重定向到绝对URL, 上下文相对URL, 或当前请求相对URL的视图.
 * URL可以是URI模板, 在这种情况下, URI模板变量将替换为模型中可用的值.
 * 默认情况下, 所有原始模型属性 (或其集合) 都作为HTTP查询参数公开 (假设它们未被用作URI模板变量),
 * 但可以通过覆盖{@link #isEligibleProperty(String, Object)}方法来更改此行为.
 *
 * <p>此视图的URL应该是HTTP重定向URL, i.e. 适用于HttpServletResponse的{@code sendRedirect}方法,
 * 如果HTTP 1.0标志打开, 或者如果HTTP 1.0兼容性标志关闭, 通过发送回HTTP 303代码, 它进行实际的重定向.
 *
 * <p>请注意, 虽然"contextRelative"标志的默认值关闭, 但您可能希望几乎始终将其设置为true.
 * 关闭标志后, 以"/"开头的URL被认为是相对于Web服务器根目录, 而当标志打开时, 它们被认为是相对于Web应用程序根目录.
 * 由于大多数Web应用程序永远不会知道或关心它们的上下文路径实际是什么,
 * 因此最好将这个标志设置为true, 并提交相对于Web应用程序根目录的路径.
 *
 * <p><b>注意在Portlet环境中使用此重定向视图时:</b> 确保控制器遵守Portlet {@code sendRedirect}约束.
 */
public class RedirectView extends AbstractUrlBasedView implements SmartView {

	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


	private boolean contextRelative = false;

	private boolean http10Compatible = true;

	private boolean exposeModelAttributes = true;

	private String encodingScheme;

	private HttpStatus statusCode;

	private boolean expandUriTemplateVariables = true;

	private boolean propagateQueryParams = false;

	private String[] hosts;


	public RedirectView() {
		setExposePathVariables(false);
	}

	/**
	 * <p>给定的URL将被视为相对于Web服务器, 而不是相对于当前的ServletContext.
	 * 
	 * @param url 要重定向到的URL
	 */
	public RedirectView(String url) {
		super(url);
		setExposePathVariables(false);
	}

	/**
	 * @param url 要重定向到的URL
	 * @param contextRelative 是否将给定的URL解释为相对于当前的ServletContext
	 */
	public RedirectView(String url, boolean contextRelative) {
		super(url);
		this.contextRelative = contextRelative;
		setExposePathVariables(false);
	}

	/**
	 * @param url 要重定向到的URL
	 * @param contextRelative 是否将给定的URL解释为相对于当前的ServletContext
	 * @param http10Compatible 是否与HTTP 1.0客户端保持兼容
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		setExposePathVariables(false);
	}

	/**
	 * @param url 要重定向到的URL
	 * @param contextRelative 是否将给定的URL解释为相对于当前的ServletContext
	 * @param http10Compatible 是否与HTTP 1.0客户端保持兼容
	 * @param exposeModelAttributes 是否应将模型属性公开为查询参数
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible, boolean exposeModelAttributes) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		this.exposeModelAttributes = exposeModelAttributes;
		setExposePathVariables(false);
	}


	/**
	 * 设置是否将以斜杠 ("/") 开头的给定URL解释为相对于当前ServletContext, i.e. 相对于Web应用程序根目录.
	 * <p>默认为"false": 以斜杠开头的URL将被解释为绝对值, i.e. 按原样使用.
	 * 如果为"true", 则在这种情况下, 上下文路径将被添加到URL.
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * 设置是否与HTTP 1.0客户端保持兼容.
	 * <p>在默认实现中, 这将在任何情况下强制HTTP状态码302, i.e. 委托给{@code HttpServletResponse.sendRedirect}.
	 * 关闭它将发送HTTP状态码303, 这是HTTP 1.1客户端的正确代码, 但HTTP 1.0客户端无法理解.
	 * <p>许多HTTP 1.1客户端就像303一样对待302, 没有任何区别.
	 * 但是, 一些客户端在POST请求后重定向时依赖于303; 在这种情况下关闭此标志.
	 */
	public void setHttp10Compatible(boolean http10Compatible) {
		this.http10Compatible = http10Compatible;
	}

	/**
	 * 设置{@code exposeModelAttributes}标志, 该标志表示是否应将模型属性公开为HTTP查询参数.
	 * <p>默认为{@code true}.
	 */
	public void setExposeModelAttributes(final boolean exposeModelAttributes) {
		this.exposeModelAttributes = exposeModelAttributes;
	}

	/**
	 * 设置此视图的编码scheme.
	 * <p>默认是请求的编码方案 (如果没有另外指定, 则为ISO-8859-1).
	 */
	public void setEncodingScheme(String encodingScheme) {
		this.encodingScheme = encodingScheme;
	}

	/**
	 * 设置此视图的状态码.
	 * <p>默认发送 302/303, 取决于{@link #setHttp10Compatible(boolean) http10Compatible}标志的值.
	 */
	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * 是否将重定向URL视为URI模板.
	 * 如果重定向URL包含打开和关闭花括号 "{", "}", 并且不希望它们被解释为URI变量, 将此标志设置为{@code false}.
	 * <p>默认为{@code true}.
	 */
	public void setExpandUriTemplateVariables(boolean expandUriTemplateVariables) {
		this.expandUriTemplateVariables = expandUriTemplateVariables;
	}

	/**
	 * 设置为{@code true}时, 将追加当前URL的查询字符串, 从而传播到重定向的URL.
	 * <p>默认为{@code false}.
	 */
	public void setPropagateQueryParams(boolean propagateQueryParams) {
		this.propagateQueryParams = propagateQueryParams;
	}

	/**
	 * 是否传播当前URL的查询参数.
	 */
	public boolean isPropagateQueryProperties() {
		return this.propagateQueryParams;
	}

	/**
	 * 配置与应用程序关联的一个或多个主机. 所有其他主机将被视为外部主机.
	 * <p>实际上, 此属性提供了一种方法, 通过{@link HttpServletResponse#encodeRedirectURL}
	 * 关闭具有主机且该主机未列为已知主机的URL的编码.
	 * <p>如果未设置 (默认), 则所有URL都通过响应进行编码.
	 * 
	 * @param hosts one or more application hosts
	 */
	public void setHosts(String... hosts) {
		this.hosts = hosts;
	}

	/**
	 * 返回配置的应用程序主机.
	 */
	public String[] getHosts() {
		return this.hosts;
	}

	/**
	 * 返回"true", 表示此视图执行重定向.
	 */
	@Override
	public boolean isRedirectView() {
		return true;
	}

	/**
	 * RedirectView不严格要求ApplicationContext.
	 */
	@Override
	protected boolean isContextRequired() {
		return false;
	}


	/**
	 * 将模型转换为请求参数, 并重定向到给定的URL.
	 */
	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String targetUrl = createTargetUrl(model, request);
		targetUrl = updateTargetUrl(targetUrl, model, request, response);

		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
		if (!CollectionUtils.isEmpty(flashMap)) {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(targetUrl).build();
			flashMap.setTargetRequestPath(uriComponents.getPath());
			flashMap.addTargetRequestParams(uriComponents.getQueryParams());
			FlashMapManager flashMapManager = RequestContextUtils.getFlashMapManager(request);
			if (flashMapManager == null) {
				throw new IllegalStateException("FlashMapManager not found despite output FlashMap having been set");
			}
			flashMapManager.saveOutputFlashMap(flashMap, request, response);
		}

		sendRedirect(request, response, targetUrl, this.http10Compatible);
	}

	/**
	 * 创建目标URL, 首先检查重定向字符串是否为URI模板, 使用给定模型扩展它,
	 * 然后可选地将简单类型模型属性作为查询字符串参数附加.
	 */
	protected final String createTargetUrl(Map<String, Object> model, HttpServletRequest request)
			throws UnsupportedEncodingException {

		// 准备目标URL.
		StringBuilder targetUrl = new StringBuilder();
		if (this.contextRelative && getUrl().startsWith("/")) {
			// 不要将上下文路径应用于相对URL.
			targetUrl.append(getContextPath(request));
		}
		targetUrl.append(getUrl());

		String enc = this.encodingScheme;
		if (enc == null) {
			enc = request.getCharacterEncoding();
		}
		if (enc == null) {
			enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}

		if (this.expandUriTemplateVariables && StringUtils.hasText(targetUrl)) {
			Map<String, String> variables = getCurrentRequestUriVariables(request);
			targetUrl = replaceUriTemplateVariables(targetUrl.toString(), model, variables, enc);
		}
		if (isPropagateQueryProperties()) {
		 	appendCurrentQueryParams(targetUrl, request);
		}
		if (this.exposeModelAttributes) {
			appendQueryProperties(targetUrl, model, enc);
		}

		return targetUrl.toString();
	}

	private String getContextPath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		while (contextPath.startsWith("//")) {
			contextPath = contextPath.substring(1);
		}
		return contextPath;
	}

	/**
	 * 使用当前请求中的编码模型属性或URI变量替换目标URL中的URI模板变量.
	 * URL中引用的模型属性将从模型中删除.
	 * 
	 * @param targetUrl 重定向URL
	 * @param model 包含模型属性的Map
	 * @param currentUriVariables 要使用的当前请求URI变量
	 * @param encodingScheme 要使用的编码方案
	 * 
	 * @throws UnsupportedEncodingException 如果字符串编码失败
	 */
	protected StringBuilder replaceUriTemplateVariables(
			String targetUrl, Map<String, Object> model, Map<String, String> currentUriVariables, String encodingScheme)
			throws UnsupportedEncodingException {

		StringBuilder result = new StringBuilder();
		Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		int endLastMatch = 0;
		while (matcher.find()) {
			String name = matcher.group(1);
			Object value = (model.containsKey(name) ? model.remove(name) : currentUriVariables.get(name));
			if (value == null) {
				throw new IllegalArgumentException("Model has no value for key '" + name + "'");
			}
			result.append(targetUrl.substring(endLastMatch, matcher.start()));
			result.append(UriUtils.encodePathSegment(value.toString(), encodingScheme));
			endLastMatch = matcher.end();
		}
		result.append(targetUrl.substring(endLastMatch, targetUrl.length()));
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getCurrentRequestUriVariables(HttpServletRequest request) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVars = (Map<String, String>) request.getAttribute(name);
		return (uriVars != null) ? uriVars : Collections.<String, String> emptyMap();
	}

	/**
	 * 将当前请求的查询字符串附加到目标重定向URL.
	 * 
	 * @param targetUrl 要添加属性的StringBuilder
	 * @param request 当前的请求
	 */
	protected void appendCurrentQueryParams(StringBuilder targetUrl, HttpServletRequest request) {
		String query = request.getQueryString();
		if (StringUtils.hasText(query)) {
			// Extract anchor fragment, if any.
			String fragment = null;
			int anchorIndex = targetUrl.indexOf("#");
			if (anchorIndex > -1) {
				fragment = targetUrl.substring(anchorIndex);
				targetUrl.delete(anchorIndex, targetUrl.length());
			}

			if (targetUrl.toString().indexOf('?') < 0) {
				targetUrl.append('?').append(query);
			}
			else {
				targetUrl.append('&').append(query);
			}
			// Append anchor fragment, if any, to end of URL.
			if (fragment != null) {
				targetUrl.append(fragment);
			}
		}
	}

	/**
	 * 将查询属性附加到重定向URL.
	 * 对模型属性进行字符串化, URL编码, 和格式化为查询属性.
	 * 
	 * @param targetUrl 要添加属性的StringBuilder
	 * @param model 包含模型属性的Map
	 * @param encodingScheme 要使用的编码方案
	 * 
	 * @throws UnsupportedEncodingException 如果字符串编码失败
	 */
	@SuppressWarnings("unchecked")
	protected void appendQueryProperties(StringBuilder targetUrl, Map<String, Object> model, String encodingScheme)
			throws UnsupportedEncodingException {

		// Extract anchor fragment, if any.
		String fragment = null;
		int anchorIndex = targetUrl.indexOf("#");
		if (anchorIndex > -1) {
			fragment = targetUrl.substring(anchorIndex);
			targetUrl.delete(anchorIndex, targetUrl.length());
		}

		// 如果还没有一些参数, 需要一个 "?".
		boolean first = (targetUrl.toString().indexOf('?') < 0);
		for (Map.Entry<String, Object> entry : queryProperties(model).entrySet()) {
			Object rawValue = entry.getValue();
			Iterator<Object> valueIter;
			if (rawValue != null && rawValue.getClass().isArray()) {
				valueIter = Arrays.asList(ObjectUtils.toObjectArray(rawValue)).iterator();
			}
			else if (rawValue instanceof Collection) {
				valueIter = ((Collection<Object>) rawValue).iterator();
			}
			else {
				valueIter = Collections.singleton(rawValue).iterator();
			}
			while (valueIter.hasNext()) {
				Object value = valueIter.next();
				if (first) {
					targetUrl.append('?');
					first = false;
				}
				else {
					targetUrl.append('&');
				}
				String encodedKey = urlEncode(entry.getKey(), encodingScheme);
				String encodedValue = (value != null ? urlEncode(value.toString(), encodingScheme) : "");
				targetUrl.append(encodedKey).append('=').append(encodedValue);
			}
		}

		// Append anchor fragment, if any, to end of URL.
		if (fragment != null) {
			targetUrl.append(fragment);
		}
	}

	/**
	 * 确定查询字符串的name-value对, 这些字符串将由{@link #appendQueryProperties}进行字符串化, URL编码和格式化.
	 * <p>此实现通过检查每个元素的{@link #isEligibleProperty(String, Object)}来过滤模型,
	 * 默认情况下仅接受字符串, 基本类型和原始类型包装器.
	 * 
	 * @param model 原始模型Map
	 * 
	 * @return 已过滤的符合条件的查询属性的Map
	 */
	protected Map<String, Object> queryProperties(Map<String, Object> model) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			if (isEligibleProperty(entry.getKey(), entry.getValue())) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	/**
	 * 确定是否应将给定的模型元素作为查询属性公开.
	 * <p>默认实现支持字符串和基本类型, 以及具有相应元素的数组和集合/Iterables.
	 * 可以在子类中重写.
	 * 
	 * @param key 模型元素的键
	 * @param value 模型元素的值
	 * 
	 * @return 元素是否可作为查询属性
	 */
	protected boolean isEligibleProperty(String key, Object value) {
		if (value == null) {
			return false;
		}
		if (isEligibleValue(value)) {
			return true;
		}
		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			if (length == 0) {
				return false;
			}
			for (int i = 0; i < length; i++) {
				Object element = Array.get(value, i);
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		}
		if (value instanceof Collection) {
			Collection<?> coll = (Collection<?>) value;
			if (coll.isEmpty()) {
				return false;
			}
			for (Object element : coll) {
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 确定给定的模型元素值是否符合公开条件.
	 * <p>默认实现支持原始类型, String, Number, Date, URI, URL 和 Locale对象.
	 * 可以在子类中重写.
	 * 
	 * @param value 模型元素值
	 * 
	 * @return 元素值是否符合
	 */
	protected boolean isEligibleValue(Object value) {
		return (value != null && BeanUtils.isSimpleValueType(value.getClass()));
	}

	/**
	 * 使用给定的编码方案对给定的输入String进行URL编码.
	 * <p>默认实现使用{@code URLEncoder.encode(input, enc)}.
	 * 
	 * @param input 未编码的输入字符串
	 * @param encodingScheme 编码方案
	 * 
	 * @return 编码后的输出String
	 * @throws UnsupportedEncodingException 如果由JDK URLEncoder抛出
	 */
	protected String urlEncode(String input, String encodingScheme) throws UnsupportedEncodingException {
		return (input != null ? URLEncoder.encode(input, encodingScheme) : null);
	}

	/**
	 * 查找注册的{@link RequestDataValueProcessor}, 并允许它更新重定向目标URL.
	 * 
	 * @param targetUrl 给定的重定向URL
	 * 
	 * @return 更新后的URL, 或与传入的URL相同的URL
	 */
	protected String updateTargetUrl(String targetUrl, Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) {

		WebApplicationContext wac = getWebApplicationContext();
		if (wac == null) {
			wac = RequestContextUtils.findWebApplicationContext(request, getServletContext());
		}

		if (wac != null && wac.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			RequestDataValueProcessor processor = wac.getBean(
					RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
			return processor.processUrl(request, targetUrl);
		}

		return targetUrl;
	}

	/**
	 * 将重定向发送回HTTP客户端
	 * 
	 * @param request 当前的HTTP请求 (允许对请求方法作出反应)
	 * @param response 当前HTTP响应 (用于发送响应header)
	 * @param targetUrl 要重定向到的目标URL
	 * @param http10Compatible 是否与HTTP 1.0客户端保持兼容
	 * 
	 * @throws IOException 如果被响应方法抛出
	 */
	protected void sendRedirect(HttpServletRequest request, HttpServletResponse response,
			String targetUrl, boolean http10Compatible) throws IOException {

		String encodedURL = (isRemoteHost(targetUrl) ? targetUrl : response.encodeRedirectURL(targetUrl));
		if (http10Compatible) {
			HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
			if (this.statusCode != null) {
				response.setStatus(this.statusCode.value());
				response.setHeader("Location", encodedURL);
			}
			else if (attributeStatusCode != null) {
				response.setStatus(attributeStatusCode.value());
				response.setHeader("Location", encodedURL);
			}
			else {
				// 默认发送状态码302.
				response.sendRedirect(encodedURL);
			}
		}
		else {
			HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
			response.setStatus(statusCode.value());
			response.setHeader("Location", encodedURL);
		}
	}

	/**
	 * 给定的targetUrl是否具有作为"foreign"系统的主机, 在这种情况下将不会应用{@link HttpServletResponse#encodeRedirectURL}.
	 * 如果配置了{@link #setHosts(String[])}属性, 并且目标URL具有不匹配的主机, 则此方法返回{@code true}.
	 * 
	 * @param targetUrl 目标重定向 URL
	 * 
	 * @return {@code true} 目标URL有一个远程主机, {@code false} 如果URL没有主机或未配置"host"属性
	 */
	protected boolean isRemoteHost(String targetUrl) {
		if (ObjectUtils.isEmpty(getHosts())) {
			return false;
		}
		String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();
		if (StringUtils.isEmpty(targetHost)) {
			return false;
		}
		for (String host : getHosts()) {
			if (targetHost.equals(host)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 确定用于HTTP 1.1兼容请求的状态码.
	 * <p>如果已设置, 默认实现返回{@link #setStatusCode(HttpStatus) statusCode}属性,
	 * 或{@link #RESPONSE_STATUS_ATTRIBUTE}属性的值.
	 * 如果两者都未设置, 则默认为{@link HttpStatus#SEE_OTHER} (303).
	 * 
	 * @param request 要检查的请求
	 * @param response servlet响应
	 * @param targetUrl 目标URL
	 * 
	 * @return 响应状态
	 */
	protected HttpStatus getHttp11StatusCode(
			HttpServletRequest request, HttpServletResponse response, String targetUrl) {

		if (this.statusCode != null) {
			return this.statusCode;
		}
		HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
		if (attributeStatusCode != null) {
			return attributeStatusCode;
		}
		return HttpStatus.SEE_OTHER;
	}
}
