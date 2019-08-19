package org.springframework.web.servlet.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import freemarker.core.ParseException;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * 使用FreeMarker模板引擎的视图.
 *
 * <p>公开以下JavaBean属性:
 * <ul>
 * <li><b>url</b>: 要包装的FreeMarker模板的位置, 相对于FreeMarker模板上下文 (目录).
 * <li><b>encoding</b> (可选, 默认值由FreeMarker配置决定): FreeMarker模板文件的编码
 * </ul>
 *
 * <p>取决于在当前Web应用程序上下文中可以访问的{@link FreeMarkerConfig}对象,
 * 例如{@link FreeMarkerConfigr}, 具有任何bean名称.
 * 或者, 可以将FreeMarker {@link Configuration}对象设置为bean属性.
 * 有关此方法影响的更多详细信息, 请参阅{@link #setConfiguration}.
 *
 * <p>Note: Spring的FreeMarker支持需要FreeMarker 2.3或更高版本.
 */
public class FreeMarkerView extends AbstractTemplateView {

	private String encoding;

	private Configuration configuration;

	private TaglibFactory taglibFactory;

	private ServletContextHashModel servletContextHashModel;


	/**
	 * 设置FreeMarker模板文件的编码.
	 * 默认值由FreeMarker Configuration确定: 如果不指定, 则为"ISO-8859-1".
	 * <p>如果所有模板共享公共编码, 请在FreeMarker Configuration中指定编码, 而不是每个模板.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 返回FreeMarker模板的编码.
	 */
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置此视图使用的FreeMarker Configuration.
	 * <p>如果未设置, 则会发生默认查找: 在当前Web应用程序上下文中预期单个{@link FreeMarkerConfig}, 具有任何bean名称.
	 * <strong>Note:</strong> 使用此方法将导致为每个{@link FreeMarkerView}实例创建{@link TaglibFactory}的新实例.
	 * 就内存和初始CPU使用而言, 这可能非常昂贵.
	 * 在生产环境中, 建议使用{@link FreeMarkerConfig}公开单个共享{@link TaglibFactory}.
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 返回此视图使用的FreeMarker配置.
	 */
	protected Configuration getConfiguration() {
		return this.configuration;
	}


	/**
	 * 在启动时调用.
	 * 查找单个FreeMarkerConfig bean以查找此工厂的相关配置.
	 * <p>检查是否可以找到默认语言环境的模板:
	 * 如果找不到特定于语言环境的模板, FreeMarker将检查非特定于语言环境的模板.
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) throws BeansException {
		if (getConfiguration() != null) {
			this.taglibFactory = new TaglibFactory(servletContext);
		}
		else {
			FreeMarkerConfig config = autodetectConfiguration();
			setConfiguration(config.getConfiguration());
			this.taglibFactory = config.getTaglibFactory();
		}

		GenericServlet servlet = new GenericServletAdapter();
		try {
			servlet.init(new DelegatingServletConfig());
		}
		catch (ServletException ex) {
			throw new BeanInitializationException("Initialization of GenericServlet adapter failed", ex);
		}
		this.servletContextHashModel = new ServletContextHashModel(servlet, getObjectWrapper());
	}

	/**
	 * 通过ApplicationContext自动检测{@link FreeMarkerConfig}对象.
	 * 
	 * @return 用于FreeMarkerViews的Configuration实例
	 * @throws BeansException 如果找不到Configuration实例
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					getApplicationContext(), FreeMarkerConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this web application context " +
					"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.", ex);
		}
	}

	/**
	 * 返回配置的FreeMarker {@link ObjectWrapper}, 如果没有指定, 则返回{@link ObjectWrapper#DEFAULT_WRAPPER 默认包装器}.
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = getConfiguration().getObjectWrapper();
		return (ow != null ? ow :
				new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
	}

	/**
	 * 检查用于此视图的FreeMarker模板是否存在且有效.
	 * <p>可以重写以自定义行为, 例如, 在将多个模板呈现为单个视图的情况下.
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		String url = getUrl();
		try {
			// 检查是否可以获取模板, 即使可能随后再次获取该模板.
			getTemplate(url, locale);
			return true;
		}
		catch (FileNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No FreeMarker view found for URL: " + url);
			}
			return false;
		}
		catch (ParseException ex) {
			throw new ApplicationContextException(
					"Failed to parse FreeMarker template for URL [" + url + "]", ex);
		}
		catch (IOException ex) {
			throw new ApplicationContextException(
					"Could not load FreeMarker template for URL [" + url + "]", ex);
		}
	}


	/**
	 * 通过将模型映射与FreeMarker模板合并来处理模型映射.
	 * 输出重定向到servlet响应.
	 * <p>如果需要自定义行为, 则可以覆盖此方法.
	 */
	@Override
	protected void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(model, request);
		doRender(model, request, response);
	}

	/**
	 * 公开每个渲染操作独有的助手.
	 * 这是必要的, 以便不同的渲染操作不会覆盖彼此的格式等.
	 * <p>由{@code renderMergedTemplateModel}调用.
	 * 默认实现为空. 可以重写此方法以向模型添加自定义助手.
	 * 
	 * @param model 将在合并时传递给模板的模型
	 * @param request 当前的HTTP请求
	 * 
	 * @throws Exception 如果在向上下文添加信息时出现致命错误
	 */
	protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
	}

	/**
	 * 将FreeMarker视图渲染到给定的响应, 使用给定的包含要使用的完整模板模型的模型Map.
	 * <p>默认实现呈现由 "url" bean属性指定的模板, 通过{@code getTemplate}检索.
	 * 它委托给{@code processTemplate}方法将模板实例与给定的模板模型合并.
	 * <p>将标准Freemarker哈希模型添加到模型中: 请求参数, 请求, 会话, 和应用程序 (ServletContext), 以及JSP标记库哈希模型.
	 * <p>可以重写以自定义行为, 例如将多个模板呈现到单个视图中.
	 * 
	 * @param model 用于渲染的模型
	 * @param request 当前的HTTP请求
	 * @param response 当前的servlet响应
	 * 
	 * @throws IOException 如果无法检索模板文件
	 * @throws Exception 如果渲染失败
	 */
	protected void doRender(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 将模型公开给JSP标记 (作为请求属性).
		exposeModelAsRequestAttributes(model, request);
		// 公开所有标准的FreeMarker哈希模型.
		SimpleHash fmModel = buildTemplateModel(model, request, response);

		if (logger.isDebugEnabled()) {
			logger.debug("Rendering FreeMarker template [" + getUrl() + "] in FreeMarkerView '" + getBeanName() + "'");
		}
		// 获取模板的特定于语言环境的版本.
		Locale locale = RequestContextUtils.getLocale(request);
		processTemplate(getTemplate(locale), fmModel, response);
	}

	/**
	 * 为给定的模型Map构建FreeMarker模板模型.
	 * <p>默认实现构建{@link AllHttpScopesHashModel}.
	 * 
	 * @param model 用于渲染的模型
	 * @param request 当前的HTTP请求
	 * @param response 当前的servlet响应
	 * 
	 * @return FreeMarker模板模型, 作为{@link SimpleHash}或其子类
	 */
	protected SimpleHash buildTemplateModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		AllHttpScopesHashModel fmModel = new AllHttpScopesHashModel(getObjectWrapper(), getServletContext(), request);
		fmModel.put(FreemarkerServlet.KEY_JSP_TAGLIBS, this.taglibFactory);
		fmModel.put(FreemarkerServlet.KEY_APPLICATION, this.servletContextHashModel);
		fmModel.put(FreemarkerServlet.KEY_SESSION, buildSessionModel(request, response));
		fmModel.put(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, response, getObjectWrapper()));
		fmModel.put(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * 为给定的请求构建FreeMarker {@link HttpSessionHashModel}, 检测会话是否已存在并做出相应的反应.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的servlet响应
	 * 
	 * @return the FreeMarker HttpSessionHashModel
	 */
	private HttpSessionHashModel buildSessionModel(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			return new HttpSessionHashModel(session, getObjectWrapper());
		}
		else {
			return new HttpSessionHashModel(null, request, response, getObjectWrapper());
		}
	}

	/**
	 * 检索给定语言环境的FreeMarker模板, 以便通过此视图进行渲染.
	 * <p>默认情况下, 将检索由"url" bean属性指定的模板.
	 * 
	 * @param locale 当前的语言环境
	 * 
	 * @return 要呈现的FreeMarker模板
	 * @throws IOException 如果无法检索模板文件
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		return getTemplate(getUrl(), locale);
	}

	/**
	 * 使用"encoding" bean属性指定的编码检索由给定名称指定的FreeMarker模板.
	 * <p>可以由子类调用以检索特定模板, 例如将多个模板呈现到单个视图中.
	 * 
	 * @param name 所需模板的文件名
	 * @param locale 当前的语言环境
	 * 
	 * @return FreeMarker模板
	 * @throws IOException 如果无法检索模板文件
	 */
	protected Template getTemplate(String name, Locale locale) throws IOException {
		return (getEncoding() != null ?
				getConfiguration().getTemplate(name, locale, getEncoding()) :
				getConfiguration().getTemplate(name, locale));
	}

	/**
	 * 将FreeMarker模板处理为servlet响应.
	 * <p>可以重写以自定义行为.
	 * 
	 * @param template 要处理的模板
	 * @param model 模板的模型
	 * @param response servlet响应 (使用它来获取OutputStream或Writer)
	 * 
	 * @throws IOException 如果无法检索模板文件
	 * @throws TemplateException 如果被FreeMarker抛出
	 */
	protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response)
			throws IOException, TemplateException {

		template.process(model, response.getWriter());
	}


	/**
	 * 扩展{@link GenericServlet}的简单适配器类.
	 * 在FreeMarker中需要JSP访问.
	 */
	@SuppressWarnings("serial")
	private static class GenericServletAdapter extends GenericServlet {

		@Override
		public void service(ServletRequest servletRequest, ServletResponse servletResponse) {
			// no-op
		}
	}


	/**
	 * {@link ServletConfig}接口的内部实现, 传递给servlet适配器.
	 */
	private class DelegatingServletConfig implements ServletConfig {

		@Override
		public String getServletName() {
			return FreeMarkerView.this.getBeanName();
		}

		@Override
		public ServletContext getServletContext() {
			return FreeMarkerView.this.getServletContext();
		}

		@Override
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.<String>emptySet());
		}
	}
}
