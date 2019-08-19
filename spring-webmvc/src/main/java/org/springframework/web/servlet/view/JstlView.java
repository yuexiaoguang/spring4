package org.springframework.web.servlet.view;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * 专门用于JSTL页面的{@link InternalResourceView}, i.e. 使用JSP标准标记库的JSP页面.
 *
 * <p>使用Spring的语言环境和{@link org.springframework.context.MessageSource}公开JSTL特定的请求属性,
 * 为JSTL的格式和消息标记指定语言环境和资源包.
 *
 * <p>从DispatcherServlet上下文定义的角度看, {@link InternalResourceViewResolver}的典型用法如下所示:
 *
 * <pre class="code">
 * &lt;bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource"&gt;
 *   &lt;property name="basename" value="messages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 从处理器返回的每个视图名称都将转换为JSP资源 (例如: "myView" -> "/WEB-INF/jsp/myView.jsp"),
 * 使用此视图类启用显式JSTL支持.
 *
 * <p>指定的MessageSource从类路径中的"messages.properties"等文件加载消息.
 * 这将自动作为JSTL fmt标记 (消息等)将使用的JSTL本地化上下文暴露给视图.
 * 考虑使用Spring的 ReloadableResourceBundleMessageSource,
 * 而不是标准的 Res​​ourceBundleMessageSource 来提高复杂度.
 * 当然, 任何其他Spring组件都可以共享相同的MessageSource.
 *
 * <p>这是一个单独的类, 主要是为了避免{@link InternalResourceView}本身的JSTL依赖.
 * 在J2EE 1.4之前, JSTL还没有成为标准J2EE的一部分, 所以不能假设JSTL API jar在类路径上可用.
 *
 * <p>Hint: 将{@link #setExposeContextBeansAsAttributes}标志设置为 "true",
 * 以便在JSTL表达式中访问应用程序上下文中的所有Spring bean (e.g. 在{@code c:out}值表达式中).
 * 这也将使所有这些bean在JSP 2.0页面中的普通{@code ${...}}表达式中可访问.
 */
public class JstlView extends InternalResourceView {

	private MessageSource messageSource;


	public JstlView() {
	}

	/**
	 * @param url 转发到的URL
	 */
	public JstlView(String url) {
		super(url);
	}

	/**
	 * @param url 转发到的URL
	 * @param messageSource 要向JSTL标记公开的MessageSource
	 * (将使用知道JSTL的{@code javax.servlet.jsp.jstl.fmt.localizationContext} context-param的JSTL感知的MessageSource包装)
	 */
	public JstlView(String url, MessageSource messageSource) {
		this(url);
		this.messageSource = messageSource;
	}


	/**
	 * 使用知道JSTL的 {@code javax.servlet.jsp.jstl.fmt.localizationContext} context-param的JSTL感知的MessageSource包装MessageSource.
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.messageSource != null) {
			this.messageSource = JstlUtils.getJstlAwareMessageSource(servletContext, this.messageSource);
		}
		super.initServletContext(servletContext);
	}

	/**
	 * 公开Spring的语言环境和MessageSource的JSTL LocalizationContext.
	 */
	@Override
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
		if (this.messageSource != null) {
			JstlUtils.exposeLocalizationContext(request, this.messageSource);
		}
		else {
			JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
		}
	}
}
