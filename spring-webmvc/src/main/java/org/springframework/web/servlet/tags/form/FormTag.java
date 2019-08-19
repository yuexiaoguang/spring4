package org.springframework.web.servlet.tags.form;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.beans.PropertyAccessor;
import org.springframework.core.Conventions;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

/**
 * 用于呈现HTML '{@code form}'的数据绑定感知JSP标记, 其内部元素绑定到<em>表单对象</em>上的属性.
 *
 * <p>用户应在填充视图数据时将表单对象放入{@link org.springframework.web.servlet.ModelAndView ModelAndView}.
 * 可以使用{@link #setModelAttribute "modelAttribute"}属性配置此表单对象的名称.
 */
@SuppressWarnings("serial")
public class FormTag extends AbstractHtmlElementTag {

	/** 发送表单值到服务器的默认HTTP方法: "post" */
	private static final String DEFAULT_METHOD = "post";

	/** 默认属性名称: &quot;command&quot; */
	public static final String DEFAULT_COMMAND_NAME = "command";

	/** '{@code modelAttribute}'设置的名称 */
	private static final String MODEL_ATTRIBUTE = "modelAttribute";

	/**
	 * 表单对象名称所在的{@link javax.servlet.jsp.PageContext}属性的名称.
	 */
	public static final String MODEL_ATTRIBUTE_VARIABLE_NAME =
			Conventions.getQualifiedAttributeName(AbstractFormTag.class, MODEL_ATTRIBUTE);

	/** 默认方法参数, i.e. {@code _method}. */
	private static final String DEFAULT_METHOD_PARAM = "_method";

	private static final String FORM_TAG = "form";

	private static final String INPUT_TAG = "input";

	private static final String ACTION_ATTRIBUTE = "action";

	private static final String METHOD_ATTRIBUTE = "method";

	private static final String TARGET_ATTRIBUTE = "target";

	private static final String ENCTYPE_ATTRIBUTE = "enctype";

	private static final String ACCEPT_CHARSET_ATTRIBUTE = "accept-charset";

	private static final String ONSUBMIT_ATTRIBUTE = "onsubmit";

	private static final String ONRESET_ATTRIBUTE = "onreset";

	private static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

	private static final String NAME_ATTRIBUTE = "name";

	private static final String VALUE_ATTRIBUTE = "value";

	private static final String TYPE_ATTRIBUTE = "type";


	private TagWriter tagWriter;

	private String modelAttribute = DEFAULT_COMMAND_NAME;

	private String name;

	private String action;

	private String servletRelativeAction;

	private String method = DEFAULT_METHOD;

	private String target;

	private String enctype;

	private String acceptCharset;

	private String onsubmit;

	private String onreset;

	private String autocomplete;

	private String methodParam = DEFAULT_METHOD_PARAM;

	/** 缓存先前的嵌套路径, 以便可以重置它 */
	private String previousNestedPath;


	/**
	 * 设置模型中表单属性的名称.
	 * <p>可能是运行时表达式.
	 */
	public void setModelAttribute(String modelAttribute) {
		this.modelAttribute = modelAttribute;
	}

	/**
	 * 获取模型中表单属性的名称.
	 */
	protected String getModelAttribute() {
		return this.modelAttribute;
	}

	/**
	 * 在模型中设置表单属性的名称.
	 * <p>可能是运行时表达式.
	 * @deprecated as of Spring 4.3, in favor of {@link #setModelAttribute}
	 */
	@Deprecated
	public void setCommandName(String commandName) {
		this.modelAttribute = commandName;
	}

	/**
	 * 获取模型中表单属性的名称.
	 * @deprecated as of Spring 4.3, in favor of {@link #getModelAttribute}
	 */
	@Deprecated
	protected String getCommandName() {
		return this.modelAttribute;
	}

	/**
	 * 设置'{@code name}'属性的值.
	 * <p>可能是运行时表达式.
	 * <p>名称不是XHTML 1.0上表单的有效属性. 但是, 有时需要向后兼容.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 获取'{@code name}'属性的值.
	 */
	@Override
	protected String getName() throws JspException {
		return this.name;
	}

	/**
	 * 设置'{@code action}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public void setAction(String action) {
		this.action = (action != null ? action : "");
	}

	/**
	 * 获取'{@code action}'属性的值.
	 */
	protected String getAction() {
		return this.action;
	}

	/**
	 * 设置'{@code action}'属性的值, 通过要附加到当前servlet路径的值.
	 * <p>可能是运行时表达式.
	 */
	public void setServletRelativeAction(String servletRelativeAction) {
		this.servletRelativeAction = (servletRelativeAction != null ? servletRelativeAction : "");
	}

	/**
	 * 获取'{@code action}'属性的servlet相对值.
	 */
	protected String getServletRelativeAction() {
		return this.servletRelativeAction;
	}

	/**
	 * 设置'{@code method}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * 获取'{@code method}'属性的值.
	 */
	protected String getMethod() {
		return this.method;
	}

	/**
	 * 设置'{@code target}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * 获取'{@code target}'属性的值.
	 */
	public String getTarget() {
		return this.target;
	}

	/**
	 * 设置'{@code enctype}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public void setEnctype(String enctype) {
		this.enctype = enctype;
	}

	/**
	 * 获取'{@code enctype}'属性的值.
	 */
	protected String getEnctype() {
		return this.enctype;
	}

	/**
	 * 设置'{@code acceptCharset}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public void setAcceptCharset(String acceptCharset) {
		this.acceptCharset = acceptCharset;
	}

	/**
	 * 获取'{@code acceptCharset}'属性的值.
	 */
	protected String getAcceptCharset() {
		return this.acceptCharset;
	}

	/**
	 * 设置'{@code onsubmit}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public void setOnsubmit(String onsubmit) {
		this.onsubmit = onsubmit;
	}

	/**
	 * 获取'{@code onsubmit}'属性的值.
	 */
	protected String getOnsubmit() {
		return this.onsubmit;
	}

	/**
	 * 设置'{@code onreset}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public void setOnreset(String onreset) {
		this.onreset = onreset;
	}

	/**
	 * 获取'{@code onreset}'属性的值.
	 */
	protected String getOnreset() {
		return this.onreset;
	}

	/**
	 * 设置'{@code autocomplete}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	/**
	 * 获取'{@code autocomplete}'属性的值.
	 */
	protected String getAutocomplete() {
		return this.autocomplete;
	}

	/**
	 * 设置非浏览器支持的HTTP方法的请求参数的名称.
	 */
	public void setMethodParam(String methodParam) {
		this.methodParam = methodParam;
	}

	/**
	 * 获取非浏览器支持的HTTP方法的请求参数的名称.
	 */
	@SuppressWarnings("deprecation")
	protected String getMethodParam() {
		return getMethodParameter();
	}

	/**
	 * 获取非浏览器支持的HTTP方法的请求参数的名称.
	 * 
	 * @deprecated as of 4.2.3, in favor of {@link #getMethodParam()} which is a proper pairing for {@link #setMethodParam(String)}
	 */
	@Deprecated
	protected String getMethodParameter() {
		return this.methodParam;
	}

	/**
	 * 确定浏览器是否支持HTTP方法 (i.e. GET 或 POST).
	 */
	protected boolean isMethodBrowserSupported(String method) {
		return ("get".equalsIgnoreCase(method) || "post".equalsIgnoreCase(method));
	}


	/**
	 * 写入块'{@code form}'标记的开头部分, 并在{@link javax.servlet.jsp.PageContext}中公开表单对象名称.
	 * 
	 * @param tagWriter 表单内容要写入的{@link TagWriter}
	 * 
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE}
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		this.tagWriter = tagWriter;

		tagWriter.startTag(FORM_TAG);
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute(ACTION_ATTRIBUTE, resolveAction());
		writeOptionalAttribute(tagWriter, METHOD_ATTRIBUTE, getHttpMethod());
		writeOptionalAttribute(tagWriter, TARGET_ATTRIBUTE, getTarget());
		writeOptionalAttribute(tagWriter, ENCTYPE_ATTRIBUTE, getEnctype());
		writeOptionalAttribute(tagWriter, ACCEPT_CHARSET_ATTRIBUTE, getAcceptCharset());
		writeOptionalAttribute(tagWriter, ONSUBMIT_ATTRIBUTE, getOnsubmit());
		writeOptionalAttribute(tagWriter, ONRESET_ATTRIBUTE, getOnreset());
		writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());

		tagWriter.forceBlock();

		if (!isMethodBrowserSupported(getMethod())) {
			assertHttpMethod(getMethod());
			String inputName = getMethodParam();
			String inputType = "hidden";
			tagWriter.startTag(INPUT_TAG);
			writeOptionalAttribute(tagWriter, TYPE_ATTRIBUTE, inputType);
			writeOptionalAttribute(tagWriter, NAME_ATTRIBUTE, inputName);
			writeOptionalAttribute(tagWriter, VALUE_ATTRIBUTE, processFieldValue(inputName, getMethod(), inputType));
			tagWriter.endTag();
		}

		// 公开嵌套标签的表单对象名称...
		String modelAttribute = resolveModelAttribute();
		this.pageContext.setAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, modelAttribute, PageContext.REQUEST_SCOPE);

		// 保存以前的nestedPath值, 构建并公开当前的nestedPath值.
		// 使用request范围也可以将nestedPath公开给包含的页面.
		this.previousNestedPath =
				(String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME,
				modelAttribute + PropertyAccessor.NESTED_PROPERTY_SEPARATOR, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	private String getHttpMethod() {
		return (isMethodBrowserSupported(getMethod()) ? getMethod() : DEFAULT_METHOD);
	}

	private void assertHttpMethod(String method) {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			if (httpMethod.name().equalsIgnoreCase(method)) {
				return;
			}
		}
		throw new IllegalArgumentException("Invalid HTTP method: " + method);
	}

	/**
	 * 自动生成的ID对应于表单对象名称.
	 */
	@Override
	protected String autogenerateId() throws JspException {
		return resolveModelAttribute();
	}

	/**
	 * {@link #evaluate 解析}并返回表单对象的名称.
	 * 
	 * @throws IllegalArgumentException 如果表单对象解析为{@code null}
	 */
	protected String resolveModelAttribute() throws JspException {
		Object resolvedModelAttribute = evaluate(MODEL_ATTRIBUTE, getModelAttribute());
		if (resolvedModelAttribute == null) {
			throw new IllegalArgumentException(MODEL_ATTRIBUTE + " must not be null");
		}
		return (String) resolvedModelAttribute;
	}

	/**
	 * 解析'{@code action}'属性的值.
	 * <p>如果用户配置了'{@code action}'值, 则使用评估此值的结果.
	 * 如果用户配置了'{@code servletRelativeAction}'值, 则该值将附加上下文和servlet路径, 并使用结果.
	 * 否则, 使用{@link org.springframework.web.servlet.support.RequestContext#getRequestUri() 原始URI}.
	 * 
	 * @return 用于'{@code action}'属性的值
	 */
	protected String resolveAction() throws JspException {
		String action = getAction();
		String servletRelativeAction = getServletRelativeAction();
		if (StringUtils.hasText(action)) {
			action = getDisplayString(evaluate(ACTION_ATTRIBUTE, action));
			return processAction(action);
		}
		else if (StringUtils.hasText(servletRelativeAction)) {
			String pathToServlet = getRequestContext().getPathToServlet();
			if (servletRelativeAction.startsWith("/") &&
					!servletRelativeAction.startsWith(getRequestContext().getContextPath())) {
				servletRelativeAction = pathToServlet + servletRelativeAction;
			}
			servletRelativeAction = getDisplayString(evaluate(ACTION_ATTRIBUTE, servletRelativeAction));
			return processAction(servletRelativeAction);
		}
		else {
			String requestUri = getRequestContext().getRequestUri();
			String encoding = this.pageContext.getResponse().getCharacterEncoding();
			try {
				requestUri = UriUtils.encodePath(requestUri, encoding);
			}
			catch (UnsupportedEncodingException ex) {
				// shouldn't happen - if it does, proceed with requestUri as-is
			}
			ServletResponse response = this.pageContext.getResponse();
			if (response instanceof HttpServletResponse) {
				requestUri = ((HttpServletResponse) response).encodeURL(requestUri);
				String queryString = getRequestContext().getQueryString();
				if (StringUtils.hasText(queryString)) {
					requestUri += "?" + HtmlUtils.htmlEscape(queryString);
				}
			}
			if (StringUtils.hasText(requestUri)) {
				return processAction(requestUri);
			}
			else {
				throw new IllegalArgumentException("Attribute 'action' is required. " +
						"Attempted to resolve against current request URI but request URI was null.");
			}
		}
	}

	/**
	 * 如果已配置了一个实例, 则通过{@link RequestDataValueProcessor}实例处理该操作, 否则返回未修改的操作.
	 */
	private String processAction(String action) {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			action = processor.processAction((HttpServletRequest) request, action, getHttpMethod());
		}
		return action;
	}

	/**
	 * 关闭'{@code form}'块标记, 并从{@link javax.servlet.jsp.PageContext}中删除表单对象名称.
	 */
	@Override
	public int doEndTag() throws JspException {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			writeHiddenFields(processor.getExtraHiddenFields((HttpServletRequest) request));
		}
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * 将给定值写入为隐藏字段.
	 */
	private void writeHiddenFields(Map<String, String> hiddenFields) throws JspException {
		if (!CollectionUtils.isEmpty(hiddenFields)) {
			this.tagWriter.appendValue("<div>\n");
			for (String name : hiddenFields.keySet()) {
				this.tagWriter.appendValue("<input type=\"hidden\" ");
				this.tagWriter.appendValue("name=\"" + name + "\" value=\"" + hiddenFields.get(name) + "\" ");
				this.tagWriter.appendValue("/>\n");
			}
			this.tagWriter.appendValue("</div>");
		}
	}

	/**
	 * 清除保存的{@link TagWriter}.
	 */
	@Override
	public void doFinally() {
		super.doFinally();

		this.pageContext.removeAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		if (this.previousNestedPath != null) {
			// 公开先前的nestedPath值.
			this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		}
		else {
			// 删除公开的nestedPath值.
			this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		this.tagWriter = null;
		this.previousNestedPath = null;
	}


	/**
	 * 覆盖解析CSS类, 因为不支持错误类.
	 */
	@Override
	protected String resolveCssClass() throws JspException {
		return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
	}

	/**
	 * 不支持.
	 * 
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setPath(String path) {
		throw new UnsupportedOperationException("The 'path' attribute is not supported for forms");
	}

	/**
	 * 不支持.
	 * 
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setCssErrorClass(String cssErrorClass) {
		throw new UnsupportedOperationException("The 'cssErrorClass' attribute is not supported for forms");
	}
}
