package org.springframework.web.servlet.view;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.View;

/**
 * {@link org.springframework.web.servlet.ViewResolver}接口的简单实现,
 * 允许直接将符号视图名称解析为URL, 而无需显式映射定义.
 * 如果符号名称直接匹配视图资源的名称 (i.e. 符号名称是资源文件名的唯一部分), 则此选项非常有用, 而无需为每个视图定义专用映射.
 *
 * <p>支持{@link AbstractUrlBasedView}子类, 如{@link InternalResourceView},
 * {@link org.springframework.web.servlet.view.velocity.VelocityView}
 * 和{@link org.springframework.web.servlet.view.freemarker.FreeMarkerView}.
 * 可以通过"viewClass"属性指定此解析器生成的所有视图的视图类.
 *
 * <p>视图名称可以是资源URL本身, 也可以通过指定的前缀和/或后缀进行扩充.
 * 显式支持将包含RequestContext的属性导出到所有视图.
 *
 * <p>示例: prefix="/WEB-INF/jsp/", suffix=".jsp", viewname="test" -> "/WEB-INF/jsp/test.jsp"
 *
 * <p>作为一项特殊功能, 可以通过"redirect:"前缀指定重定向URL.
 * E.g.: "redirect:myAction" 将触发重定向到给定的URL, 而不是作为标准视图名称解析.
 * 这通常用于在完成表单工作流后重定向到控制器URL.
 *
 * <p>此外, 可以通过"forward:"前缀指定转发URL.
 * E.g.: "forward:myAction" 将触发转发到给定的URL, 而不是作为标准视图名称解析.
 * 这通常用于控制器URL; 它不应该用于JSP URL - 在那里使用逻辑视图名称.
 *
 * <p>Note: 此类不支持本地化解析, i.e. 根据当前区域设置将符号视图名称解析为不同的资源.
 *
 * <p><b>Note:</b> 链接ViewResolvers时, UrlBasedViewResolver将检查{@link AbstractUrlBasedView#checkResource 指定的资源是否确实存在}.
 * 但是, 使用{@link InternalResourceView}, 通常无法预先确定目标资源的存在.
 * 在这种情况下，UrlBasedViewResolver将始终为任何给定的视图名称返回View; 因此, 它应该被配置为链中的最后一个ViewResolver.
 */
public class UrlBasedViewResolver extends AbstractCachingViewResolver implements Ordered {

	/**
	 * 指定重定向URL的特殊视图名称的前缀 (通常在提交和处理表单后发送给控制器).
	 * 此类视图名称不会以配置的默认方式解析, 而是被视为特殊快捷方式.
	 */
	public static final String REDIRECT_URL_PREFIX = "redirect:";

	/**
	 * 指定转发URL的特殊视图名称的前缀 (通常在提交和处理表单后发送给控制器).
	 * 此类视图名称不会以配置的默认方式解析, 而是被视为特殊快捷方式.
	 */
	public static final String FORWARD_URL_PREFIX = "forward:";


	private Class<?> viewClass;

	private String prefix = "";

	private String suffix = "";

	private String contentType;

	private boolean redirectContextRelative = true;

	private boolean redirectHttp10Compatible = true;

	private String[] redirectHosts;

	private String requestContextAttribute;

	/** 静态属性, 属性名称(String)作为键 */
	private final Map<String, Object> staticAttributes = new HashMap<String, Object>();

	private Boolean exposePathVariables;

	private Boolean exposeContextBeansAsAttributes;

	private String[] exposedContextBeanNames;

	private String[] viewNames;

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 设置应该用于创建视图的视图类.
	 * 
	 * @param viewClass 可分配给所需视图类的类 (默认为AbstractUrlBasedView)
	 */
	public void setViewClass(Class<?> viewClass) {
		if (viewClass == null || !requiredViewClass().isAssignableFrom(viewClass)) {
			throw new IllegalArgumentException(
					"Given view class [" + (viewClass != null ? viewClass.getName() : null) +
					"] is not of type [" + requiredViewClass().getName() + "]");
		}
		this.viewClass = viewClass;
	}

	/**
	 * 返回用于创建视图的视图类.
	 */
	protected Class<?> getViewClass() {
		return this.viewClass;
	}

	/**
	 * 返回此解析器所需的视图类型.
	 * 此实现返回 AbstractUrlBasedView.
	 */
	protected Class<?> requiredViewClass() {
		return AbstractUrlBasedView.class;
	}

	/**
	 * 设置在构建URL时视图名称的前缀.
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回在构建URL时视图名称的前缀.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置在构建URL时视图名称的后缀.
	 */
	public void setSuffix(String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回在构建URL时视图名称的后缀.
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * 设置所有视图的内容类型.
	 * <p>如果假定视图本身设置内容类型, 则视图类可以忽略, e.g. 在JSP的情况下.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回所有视图的内容类型.
	 */
	protected String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置是否将以斜杠 ("/") 开头的给定重定向URL解释为相对于当前ServletContext, i.e. 相对于Web应用程序根目录.
	 * <p>默认为"true": 以斜杠开头的重定向URL将被解释为相对于Web应用程序根目录, i.e. 上下文路径将被添加到URL之前.
	 * <p><b>可以通过"redirect:"前缀指定重定向URL.</b>
	 * E.g.: "redirect:myAction"
	 */
	public void setRedirectContextRelative(boolean redirectContextRelative) {
		this.redirectContextRelative = redirectContextRelative;
	}

	/**
	 * 返回是否将以斜杠 ("/") 开头的给定重定向URL解释为相对于当前ServletContext, i.e. 相对于Web应用程序根目录.
	 */
	protected boolean isRedirectContextRelative() {
		return this.redirectContextRelative;
	}

	/**
	 * 设置重定向是否应与HTTP 1.0客户端保持兼容.
	 * <p>在默认实现中, 这将在任何情况下强制HTTP状态码302, i.e. 委托给{@code HttpServletResponse.sendRedirect}.
	 * 关闭它将发送HTTP状态代码303, 这是HTTP 1.1客户端的正确代码, 但HTTP 1.0客户端无法理解.
	 * <p>许多HTTP 1.1客户端就像303一样对待302, 没有任何区别.
	 * 但是, 一些客户端在POST请求后重定向时依赖于303; 在这种情况下关闭此标志.
	 * <p><b>可以通过"redirect:"前缀指定重定向URL.</b>
	 * E.g.: "redirect:myAction"
	 */
	public void setRedirectHttp10Compatible(boolean redirectHttp10Compatible) {
		this.redirectHttp10Compatible = redirectHttp10Compatible;
	}

	/**
	 * 返回重定向是否应与HTTP 1.0客户端保持兼容.
	 */
	protected boolean isRedirectHttp10Compatible() {
		return this.redirectHttp10Compatible;
	}

	/**
	 * 配置与应用程序关联的一个或多个主机.
	 * 所有其他主机将被视为外部主机.
	 * <p>实际上, 对于具有主机且该主机未列为已知主机的URL, 此属性提供了一种关闭重定向编码的方法,
	 * 通过 {@link HttpServletResponse#encodeRedirectURL}.
	 * <p>如果未设置 (默认), 则所有URL都通过响应进行编码.
	 * 
	 * @param redirectHosts 一个或多个应用程序主机
	 */
	public void setRedirectHosts(String... redirectHosts) {
		this.redirectHosts = redirectHosts;
	}

	/**
	 * 返回配置的应用程序主机以进行重定向.
	 */
	public String[] getRedirectHosts() {
		return this.redirectHosts;
	}

	/**
	 * 为所有视图设置RequestContext属性的名称.
	 * 
	 * @param requestContextAttribute RequestContext属性的名称
	 */
	public void setRequestContextAttribute(String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回所有视图的RequestContext属性的名称.
	 */
	protected String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 为此解析器返回的所有视图, 从{@code java.util.Properties}对象设置静态属性.
	 * <p>这是设置静态属性最方便的方法.
	 * 请注意, 如果模型中包含具有相同名称的值, 则静态属性可以被动态属性覆盖.
	 * <p>可以使用String "value" (通过PropertiesEditor解析) 或XML bean定义中的"props"元素填充.
	 */
	public void setAttributes(Properties props) {
		CollectionUtils.mergePropertiesIntoMap(props, this.staticAttributes);
	}

	/**
	 * 为此解析器返回的所有视图, 从Map设置静态属性.
	 * 这允许设置任何类型的属性值, 例如bean引用.
	 * <p>可以在XML bean定义中使用"map" 或 "props"元素填充.
	 * 
	 * @param attributes 名称字符串作为键, 属性对象作为值
	 */
	public void setAttributesMap(Map<String, ?> attributes) {
		if (attributes != null) {
			this.staticAttributes.putAll(attributes);
		}
	}

	/**
	 * 允许Map访问此解析器返回的视图的静态属性, 并可添加或覆盖特定条目.
	 * <p>用于直接指定条目, 例如通过 "attributesMap[myKey]".
	 * 这对于在子视图定义中添加或覆盖条目特别有用.
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * 指定此解析器解析的视图是否应将路径变量添加到模型中.
	 * <p>>默认设置是让每个View决定 (see {@link AbstractView#setExposePathVariables}).
	 * 但是, 可以使用此属性来覆盖它.
	 * 
	 * @param exposePathVariables
	 * <ul>
	 * <li>{@code true} - 此解析器解析的所有View将公开路径变量
	 * <li>{@code false} - 此解析器解析的View将不会公开路径变量
	 * <li>{@code null} - View可以自行决定 (默认情况下使用)
	 * </ul>
	 */
	public void setExposePathVariables(Boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * 返回此解析器解析的视图是否应将路径变量添加到模型中.
	 */
	protected Boolean getExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * 设置是否可以将应用程序上下文中的所有Spring bean作为请求属性访问, 一旦访问属性, 通过延迟检查.
	 * <p>这将使所有这些bean在JSP 2.0页面中的普通{@code ${...}}表达式以及JSTL的{@code c:out}值表达式中可访问.
	 * <p>默认为"false".
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	protected Boolean getExposeContextBeansAsAttributes() {
		return this.exposeContextBeansAsAttributes;
	}

	/**
	 * 指定上下文中应该公开的bean的名称.
	 * 如果非null, 则只有指定的bean才有资格作为属性公开.
	 */
	public void setExposedContextBeanNames(String... exposedContextBeanNames) {
		this.exposedContextBeanNames = exposedContextBeanNames;
	}

	protected String[] getExposedContextBeanNames() {
		return this.exposedContextBeanNames;
	}

	/**
	 * 设置可由此 {@link org.springframework.web.servlet.ViewResolver}处理的视图名称 (或名称模式).
	 * View名称可以包含简单的通配符, 因此'my*', '*Report' 和 '*Repo*'都匹配视图名称'myReport'.
	 */
	public void setViewNames(String... viewNames) {
		this.viewNames = viewNames;
	}

	/**
	 * 返回可由此{@link org.springframework.web.servlet.ViewResolver}处理的视图名称 (或名称模式).
	 */
	protected String[] getViewNames() {
		return this.viewNames;
	}

	/**
	 * 指定此ViewResolver bean的顺序值.
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}, 表示无序.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	protected void initApplicationContext() {
		super.initApplicationContext();
		if (getViewClass() == null) {
			throw new IllegalArgumentException("Property 'viewClass' is required");
		}
	}


	/**
	 * 此实现仅返回视图名称, 因为此ViewResolver不支持本地化解析.
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName;
	}

	/**
	 * 重写以实现 "redirect:"前缀的检查.
	 * <p>在{@code loadView}中不可能, 因为子类中被覆盖的{@code loadView}版本可能依赖于超类中一定会创建的所需视图类的实例.
	 */
	@Override
	protected View createView(String viewName, Locale locale) throws Exception {
		// 如果此解析器不应处理给定视图, 则返回null以传递给链中的下一个解析器.
		if (!canHandle(viewName, locale)) {
			return null;
		}

		// 检查特殊的 "redirect:"前缀.
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			RedirectView view = new RedirectView(redirectUrl,
					isRedirectContextRelative(), isRedirectHttp10Compatible());
			view.setHosts(getRedirectHosts());
			return applyLifecycleMethods(REDIRECT_URL_PREFIX, view);
		}

		// 检查特殊的 "forward:"前缀.
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
			String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
			return new InternalResourceView(forwardUrl);
		}

		// 否则回退到超类实现: 调用loadView.
		return super.createView(viewName, locale);
	}

	/**
	 * 指示此{@link org.springframework.web.servlet.ViewResolver}是否可以处理提供的视图名称.
	 * 如果不能, {@link #createView(String, java.util.Locale)} 将返回{@code null}.
	 * 默认实现检查配置的{@link #setViewNames 视图名称}.
	 * 
	 * @param viewName 要检索的视图的名称
	 * @param locale 用于检索视图的Locale
	 * 
	 * @return 此解析器是否适用于指定的视图
	 */
	protected boolean canHandle(String viewName, Locale locale) {
		String[] viewNames = getViewNames();
		return (viewNames == null || PatternMatchUtils.simpleMatch(viewNames, viewName));
	}

	/**
	 * 委托给{@code buildView}创建指定视图类的新实例.
	 * 应用以下Spring生命周期方法 (由通用Spring bean工厂支持):
	 * <ul>
	 * <li>ApplicationContextAware的{@code setApplicationContext}
	 * <li>InitializingBean的{@code afterPropertiesSet}
	 * </ul>
	 * 
	 * @param viewName 要检索的视图的名称
	 * 
	 * @return View实例
	 * @throws Exception 如果视图无法解析
	 */
	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		AbstractUrlBasedView view = buildView(viewName);
		View result = applyLifecycleMethods(viewName, view);
		return (view.checkResource(locale) ? result : null);
	}

	/**
	 * 创建指定视图类的新View实例并进行配置.
	 * <i>不</i>对预定义的View实例执行任何查找.
	 * <p>不必在此处调用bean容器定义的Spring生命周期方法; 这个方法返回后, 将由{@code loadView}方法应用.
	 * <p>子类通常首先调用{@code super.buildView(viewName)}, 然后再设置其他属性.
	 * 然后, {@code loadView}将在此过程结束时应用Spring生命周期方法.
	 * 
	 * @param viewName 要构建的视图的名称
	 * 
	 * @return View实例
	 * @throws Exception 如果视图无法解析
	 */
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		AbstractUrlBasedView view = (AbstractUrlBasedView) BeanUtils.instantiateClass(getViewClass());
		view.setUrl(getPrefix() + viewName + getSuffix());

		String contentType = getContentType();
		if (contentType != null) {
			view.setContentType(contentType);
		}

		view.setRequestContextAttribute(getRequestContextAttribute());
		view.setAttributesMap(getAttributesMap());

		Boolean exposePathVariables = getExposePathVariables();
		if (exposePathVariables != null) {
			view.setExposePathVariables(exposePathVariables);
		}
		Boolean exposeContextBeansAsAttributes = getExposeContextBeansAsAttributes();
		if (exposeContextBeansAsAttributes != null) {
			view.setExposeContextBeansAsAttributes(exposeContextBeansAsAttributes);
		}
		String[] exposedContextBeanNames = getExposedContextBeanNames();
		if (exposedContextBeanNames != null) {
			view.setExposedContextBeanNames(exposedContextBeanNames);
		}

		return view;
	}

	private View applyLifecycleMethods(String viewName, AbstractView view) {
		return (View) getApplicationContext().getAutowireCapableBeanFactory().initializeBean(view, viewName);
	}

}
