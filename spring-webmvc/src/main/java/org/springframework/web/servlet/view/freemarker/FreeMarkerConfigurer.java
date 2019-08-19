package org.springframework.web.servlet.view.freemarker;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.web.context.ServletContextAware;

/**
 * 配置FreeMarker以进行Web使用的JavaBean,
 * 通过"configLocation"和/或"freemarkerSettings"和/或"templateLoaderPath"属性.
 * 使用此类的最简单方法是仅指定"templateLoaderPath"; 那么不需要任何进一步的配置.
 *
 * <pre class="code">
 * &lt;bean id="freemarkerConfig" class="org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer"&gt;
 *   &lt;property name="templateLoaderPath"&gt;&lt;value&gt;/WEB-INF/freemarker/&lt;/value>&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 这个bean必须包含在任何使用Spring的FreeMarkerView作为web MVC的应用程序的应用程序上下文中.
 * 它纯粹是为了配置FreeMarker而存在.
 * 它不是由应用程序组件引用, 而是由FreeMarkerView内部引用.
 * 实现FreeMarkerConfig可由FreeMarkerView找到, 而不依赖于configurer的bean名称.
 * 如果需要, 每个DispatcherServlet都可以定义自己的FreeMarkerConfigurer.
 *
 * <p>请注意, 还可以通过"configuration" 属性引用预配置的FreeMarker Configuration实例,
 * 例如FreeMarkerConfigurationFactoryBean设置的实例.
 * 例如, 这允许共享用于Web和电子邮件使用的FreeMarker Configuration.
 *
 * <p>这个配置器注册了这个包的模板加载器, 允许引用"spring.ftl"宏库 (包含在这个包中, 因此在spring.jar中), 就像这样:
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * age is ${spring.status.value}</pre>
 *
 * Note: Spring的FreeMarker支持需要FreeMarker 2.3或更高版本.
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

	private Configuration configuration;

	private TaglibFactory taglibFactory;


	/**
	 * 设置用于FreeMarker Web配置的预配置Configuration,
	 * e.g. 用于Web和电子邮件使用的共享配置, 通过FreeMarkerConfigurationFactoryBean设置.
	 * 如果未设置, 则必须指定FreeMarkerConfigurationFactory的属性 (由此类继承).
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 为给定的ServletContext初始化{@link TaglibFactory}.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.taglibFactory = new TaglibFactory(servletContext);
	}


	/**
	 * 如果没有被预先配置的FreeMarker Configuration覆盖, 则初始化FreeMarkerConfigurationFactory的Configuration.
	 * <p>设置用于加载Spring宏的ClassTemplateLoader.
	 */
	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		if (this.configuration == null) {
			this.configuration = createConfiguration();
		}
	}

	/**
	 * 此实现为Spring提供的宏注册了一个额外的ClassTemplateLoader, 添加到列表的末尾.
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
		logger.info("ClassTemplateLoader for Spring macros added to FreeMarker configuration");
	}


	/**
	 * 返回此bean包装的Configuration对象.
	 */
	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * 返回此bean包装的TaglibFactory对象.
	 */
	@Override
	public TaglibFactory getTaglibFactory() {
		return this.taglibFactory;
	}

}
