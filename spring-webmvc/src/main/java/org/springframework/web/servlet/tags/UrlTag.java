package org.springframework.web.servlet.tags;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;
import org.springframework.web.util.UriUtils;

/**
 * 用于创建URL的JSP标记. 在JSTL c:url标记之后建模, 并考虑了向后兼容性.
 *
 * <p>增强JSTL功能, 包括:
 * <ul>
 * <li>URL编码的模板URI变量</li>
 * <li>URL的HTML/XML转义</li>
 * <li>URL的JavaScript转义</li>
 * </ul>
 *
 * <p>模板URI变量在{@link #setValue(String) 'value'}属性中指示, 并用大括号'{variableName}'标记.
 * 大括号和属性名称由在url标记正文中使用 spring:param 标记定义的参数的URL编码值替换.
 * 如果没有可用的参数, 则传递文字值. 与模板变量匹配的参数不会添加到查询字符串中.
 *
 * <p>强烈建议使用 spring:param 标记用于URI模板变量, 而不是直接EL替换, 因为值是URL编码的.
 * 无法正确编码URL会使应用程序容易受到XSS和其他注入攻击.
 *
 * <p>通过将 {@link #setHtmlEscape(boolean) 'htmlEscape'}属性设置为'true', 可以对URL进行HTML/XML转义.
 * 检测此标记实例, 页面级别或{@code web.xml}级别的HTML转义设置.
 * 默认为'false'. 将URL值设置为变量时, 建议不要转义.
 *
 * <p>用法示例:
 * <pre class="code">&lt;spring:url value="/url/path/{variableName}"&gt;
 *   &lt;spring:param name="variableName" value="more than JSTL c:url" /&gt;
 * &lt;/spring:url&gt;</pre>
 * 结果:
 * {@code /currentApplicationContext/url/path/more%20than%20JSTL%20c%3Aurl}
 */
@SuppressWarnings("serial")
public class UrlTag extends HtmlEscapingAwareTag implements ParamAware {

	private static final String URL_TEMPLATE_DELIMITER_PREFIX = "{";

	private static final String URL_TEMPLATE_DELIMITER_SUFFIX = "}";

	private static final String URL_TYPE_ABSOLUTE = "://";


	private List<Param> params;

	private Set<String> templateParams;

	private UrlType type;

	private String value;

	private String context;

	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;


	/**
	 * 设置URL的值
	 */
	public void setValue(String value) {
		if (value.contains(URL_TYPE_ABSOLUTE)) {
			this.type = UrlType.ABSOLUTE;
			this.value = value;
		}
		else if (value.startsWith("/")) {
			this.type = UrlType.CONTEXT_RELATIVE;
			this.value = value;
		}
		else {
			this.type = UrlType.RELATIVE;
			this.value = value;
		}
	}

	/**
	 * 设置URL的上下文路径. 默认为当前上下文.
	 */
	public void setContext(String context) {
		if (context.startsWith("/")) {
			this.context = context;
		}
		else {
			this.context = "/" + context;
		}
	}

	/**
	 * 设置要公开URL的变量名称. 默认为将URL呈现给当前的JspWriter
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置将URL变量导出到的范围. 除非还定义了var, 否则此属性没有意义.
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * 设置此标记的JavaScript转义.
	 * 默认为 "false".
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}

	@Override
	public void addParam(Param param) {
		this.params.add(param);
	}


	@Override
	public int doStartTagInternal() throws JspException {
		this.params = new LinkedList<Param>();
		this.templateParams = new HashSet<String>();
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		String url = createUrl();

		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if ((processor != null) && (request instanceof HttpServletRequest)) {
			url = processor.processUrl((HttpServletRequest) request, url);
		}

		if (this.var == null) {
			// print the url to the writer
			try {
				pageContext.getOut().print(url);
			}
			catch (IOException ex) {
				throw new JspException(ex);
			}
		}
		else {
			// store the url as a variable
			pageContext.setAttribute(var, url, scope);
		}
		return EVAL_PAGE;
	}


	/**
	 * 从标记属性和参数构建标记的URL.
	 * 
	 * @return URL值
	 */
	String createUrl() throws JspException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

		StringBuilder url = new StringBuilder();
		if (this.type == UrlType.CONTEXT_RELATIVE) {
			// 将应用程序上下文添加到url
			if (this.context == null) {
				url.append(request.getContextPath());
			}
			else {
				if (this.context.endsWith("/")) {
					url.append(this.context.substring(0, this.context.length() - 1));
				}
				else {
					url.append(this.context);
				}
			}
		}
		if (this.type != UrlType.RELATIVE && this.type != UrlType.ABSOLUTE && !this.value.startsWith("/")) {
			url.append("/");
		}
		url.append(replaceUriTemplateParams(this.value, this.params, this.templateParams));
		url.append(createQueryString(this.params, this.templateParams, (url.indexOf("?") == -1)));

		String urlStr = url.toString();
		if (this.type != UrlType.ABSOLUTE) {
			// 如果需要, 添加会话标识符 (不要将会话标识符嵌入远程链接!)
			urlStr = response.encodeURL(urlStr);
		}

		// HTML and/or JavaScript escape, if demanded.
		urlStr = htmlEscape(urlStr);
		urlStr = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(urlStr) : urlStr);

		return urlStr;
	}

	/**
	 * 从尚未应用为模板参数的可用参数构建查询字符串.
	 * <p>参数的名称和值是URL编码的.
	 * 
	 * @param params 从中构建查询字符串的参数
	 * @param usedParams 已作为模板参数应用的参数名称集
	 * @param includeQueryStringDelimiter true 如果查询字符串应以 '?'开头, 而不是 '&'
	 * 
	 * @return 查询字符串
	 */
	protected String createQueryString(List<Param> params, Set<String> usedParams, boolean includeQueryStringDelimiter)
			throws JspException {

		String encoding = pageContext.getResponse().getCharacterEncoding();
		StringBuilder qs = new StringBuilder();
		for (Param param : params) {
			if (!usedParams.contains(param.getName()) && StringUtils.hasLength(param.getName())) {
				if (includeQueryStringDelimiter && qs.length() == 0) {
					qs.append("?");
				}
				else {
					qs.append("&");
				}
				try {
					qs.append(UriUtils.encodeQueryParam(param.getName(), encoding));
					if (param.getValue() != null) {
						qs.append("=");
						qs.append(UriUtils.encodeQueryParam(param.getValue(), encoding));
					}
				}
				catch (UnsupportedEncodingException ex) {
					throw new JspException(ex);
				}
			}
		}
		return qs.toString();
	}

	/**
	 * 替换匹配可用参数的URL中的模板标记. 匹配参数的名称将添加到使用的参数集中.
	 * <p>参数值是URL编码的.
	 * 
	 * @param uri 带有要替换的模板参数的URL
	 * @param params 用于替换模板标记的参数
	 * @param usedParams 已替换的模板参数名称集
	 * 
	 * @return 替换了模板参数的URL
	 */
	protected String replaceUriTemplateParams(String uri, List<Param> params, Set<String> usedParams)
			throws JspException {

		String encoding = pageContext.getResponse().getCharacterEncoding();
		for (Param param : params) {
			String template = URL_TEMPLATE_DELIMITER_PREFIX + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
			if (uri.contains(template)) {
				usedParams.add(param.getName());
				try {
					uri = uri.replace(template, UriUtils.encodePath(param.getValue(), encoding));
				}
				catch (UnsupportedEncodingException ex) {
					throw new JspException(ex);
				}
			}
			else {
				template = URL_TEMPLATE_DELIMITER_PREFIX + '/' + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
				if (uri.contains(template)) {
					usedParams.add(param.getName());
					try {
						uri = uri.replace(template, UriUtils.encodePathSegment(param.getValue(), encoding));
					}
					catch (UnsupportedEncodingException ex) {
						throw new JspException(ex);
					}
				}
			}
		}
		return uri;
	}


	/**
	 * 按类型对URL​​进行分类的内部枚举.
	 */
	private enum UrlType {
		CONTEXT_RELATIVE, RELATIVE, ABSOLUTE
	}
}
