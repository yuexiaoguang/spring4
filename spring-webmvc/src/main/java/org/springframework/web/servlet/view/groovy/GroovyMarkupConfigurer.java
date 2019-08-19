package org.springframework.web.servlet.view.groovy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Groovy的{@link groovy.text.markup.TemplateConfiguration}的扩展和Spring MVC的{@link GroovyMarkupConfig}的实现,
 * 用于创建在Web应用程序中使用的{@code MarkupTemplateEngine}.
 * 配置此类的最基本方法是设置"resourceLoaderPath". 例如:
 *
 * <pre class="code">
 *
 * // 将以下内容添加到 &#64;Configuration 类
 *
 * &#64;Bean
 * public GroovyMarkupConfig groovyMarkupConfigurer() {
 *     GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
 *     configurer.setResourceLoaderPath("classpath:/WEB-INF/groovymarkup/");
 *     return configurer;
 * }
 * </pre>
 *
 * 默认情况下, 此bean将创建一个{@link MarkupTemplateEngine}, 使用:
 * <ul>
 * <li>父级ClassLoader, 用于加载带有它们的引用的Groovy模板
 * <li>基类{@link TemplateConfiguration}中的默认配置
 * <li>用于解析模板文件的{@link groovy.text.markup.TemplateResolver}
 * </ul>
 *
 * 可以直接向此bean提供{@link MarkupTemplateEngine}实例, 在这种情况下, 所有其他属性都不会被有效忽略.
 *
 * <p>这个bean必须包含在使用Spring MVC {@link GroovyMarkupView}进行渲染的任何应用程序的应用程序上下文中.
 * 它的存在纯粹是为了配置Groovy的标记模板. 它并不意味着直接由应用程序组件引用.
 * 它实现了GroovyMarkupConfig, 可以在不依赖bean名称的情况下找到GroovyMarkupView.
 * 如果需要, 每个DispatcherServlet都可以定义自己的GroovyMarkupConfigurer.
 *
 * <p>请注意, {@link MarkupTemplateEngine}默认启用资源缓存.
 * 使用{@link #setCacheTemplates(boolean)}根据需要配置它.

 * <p>Spring的Groovy Markup模板支持需要Groovy 2.3.1或更高版本.
 */
public class GroovyMarkupConfigurer extends TemplateConfiguration
		implements GroovyMarkupConfig, ApplicationContextAware, InitializingBean {

	private String resourceLoaderPath = "classpath:";

	private MarkupTemplateEngine templateEngine;

	private ApplicationContext applicationContext;


	/**
	 * 通过Spring资源位置设置Groovy标记模板资源加载器路径.
	 * 接受逗号分隔的路径列表.
	 * Spring的{@link org.springframework.core.io.ResourceLoader}理解支持标准URL, 如"file:" 和 "classpath:"以及伪URL.
	 * 在ApplicationContext中运行时允许相对路径.
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

	/**
	 * 设置预配置的MarkupTemplateEngine, 以用于Groovy标记模板Web配置.
	 * <p>请注意, 必须手动配置此引擎实例, 因为此configurer的所有其他bean属性都将被忽略.
	 */
	public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	public MarkupTemplateEngine getTemplateEngine() {
		return this.templateEngine;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 不应使用此方法, 因为考虑到解析模板的区域设置是当前HTTP请求的区域设置.
	 */
	@Override
	public void setLocale(Locale locale) {
		super.setLocale(locale);
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.templateEngine == null) {
			this.templateEngine = createTemplateEngine();
		}
	}

	protected MarkupTemplateEngine createTemplateEngine() throws IOException {
		if (this.templateEngine == null) {
			ClassLoader templateClassLoader = createTemplateClassLoader();
			this.templateEngine = new MarkupTemplateEngine(templateClassLoader, this, new LocaleTemplateResolver());
		}
		return this.templateEngine;
	}

	/**
	 * 在加载和编译模板时, 为Groovy创建父级C​​lassLoader.
	 */
	protected ClassLoader createTemplateClassLoader() throws IOException {
		String[] paths = StringUtils.commaDelimitedListToStringArray(getResourceLoaderPath());
		List<URL> urls = new ArrayList<URL>();
		for (String path : paths) {
			Resource[] resources = getApplicationContext().getResources(path);
			if (resources.length > 0) {
				for (Resource resource : resources) {
					if (resource.exists()) {
						urls.add(resource.getURL());
					}
				}
			}
		}
		ClassLoader classLoader = getApplicationContext().getClassLoader();
		return (urls.size() > 0 ? new URLClassLoader(urls.toArray(new URL[urls.size()]), classLoader) : classLoader);
	}

	/**
	 * 从给定的模板路径解析模板.
	 * <p>默认实现使用与当前请求关联的Locale,
	 * 通过{@link org.springframework.context.i18n.LocaleContextHolder LocaleContextHolder}获取, 以查找模板文件.
	 * 实际上, 忽略了在引擎级别配置的语言环境.
	 */
	protected URL resolveTemplate(ClassLoader classLoader, String templatePath) throws IOException {
		MarkupTemplateEngine.TemplateResource resource = MarkupTemplateEngine.TemplateResource.parse(templatePath);
		Locale locale = LocaleContextHolder.getLocale();
		URL url = classLoader.getResource(resource.withLocale(locale.toString().replace("-", "_")).toString());
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(locale.getLanguage()).toString());
		}
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(null).toString());
		}
		if (url == null) {
			throw new IOException("Unable to load template:" + templatePath);
		}
		return url;
	}


	/**
	 * 自定义{@link TemplateResolver 模板解析器}, 它只是委托给{@link #resolveTemplate(ClassLoader, String)}.
	 */
	private class LocaleTemplateResolver implements TemplateResolver {

		private ClassLoader classLoader;

		@Override
		public void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
			this.classLoader = templateClassLoader;
		}

		@Override
		public URL resolveTemplate(String templatePath) throws IOException {
			return GroovyMarkupConfigurer.this.resolveTemplate(this.classLoader, templatePath);
		}
	}

}
