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
 * JavaBean to configure FreeMarker for web usage, via the "configLocation"
 * and/or "freemarkerSettings" and/or "templateLoaderPath" properties.
 * The simplest way to use this class is to specify just a "templateLoaderPath";
 * you do not need any further configuration then.
 *
 * <pre class="code">
 * &lt;bean id="freemarkerConfig" class="org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer"&gt;
 *   &lt;property name="templateLoaderPath"&gt;&lt;value&gt;/WEB-INF/freemarker/&lt;/value>&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * This bean must be included in the application context of any application
 * using Spring's FreeMarkerView for web MVC. It exists purely to configure FreeMarker.
 * It is not meant to be referenced by application components but just internally
 * by FreeMarkerView. Implements FreeMarkerConfig to be found by FreeMarkerView without
 * depending on the bean name the configurer. Each DispatcherServlet can define its
 * own FreeMarkerConfigurer if desired.
 *
 * <p>Note that you can also refer to a preconfigured FreeMarker Configuration
 * instance, for example one set up by FreeMarkerConfigurationFactoryBean, via
 * the "configuration" property. This allows to share a FreeMarker Configuration
 * for web and email usage, for example.
 *
 * <p>This configurer registers a template loader for this package, allowing to
 * reference the "spring.ftl" macro library (contained in this package and thus
 * in spring.jar) like this:
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * age is ${spring.status.value}</pre>
 *
 * Note: Spring's FreeMarker support requires FreeMarker 2.3 or higher.
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

	private Configuration configuration;

	private TaglibFactory taglibFactory;


	/**
	 * Set a preconfigured Configuration to use for the FreeMarker web config, e.g. a
	 * shared one for web and email usage, set up via FreeMarkerConfigurationFactoryBean.
	 * If this is not set, FreeMarkerConfigurationFactory's properties (inherited by
	 * this class) have to be specified.
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Initialize the {@link TaglibFactory} for the given ServletContext.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.taglibFactory = new TaglibFactory(servletContext);
	}


	/**
	 * Initialize FreeMarkerConfigurationFactory's Configuration
	 * if not overridden by a preconfigured FreeMarker Configuation.
	 * <p>Sets up a ClassTemplateLoader to use for loading Spring macros.
	 */
	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		if (this.configuration == null) {
			this.configuration = createConfiguration();
		}
	}

	/**
	 * This implementation registers an additional ClassTemplateLoader
	 * for the Spring-provided macros, added to the end of the list.
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
		logger.info("ClassTemplateLoader for Spring macros added to FreeMarker configuration");
	}


	/**
	 * Return the Configuration object wrapped by this bean.
	 */
	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Return the TaglibFactory object wrapped by this bean.
	 */
	@Override
	public TaglibFactory getTaglibFactory() {
		return this.taglibFactory;
	}

}
