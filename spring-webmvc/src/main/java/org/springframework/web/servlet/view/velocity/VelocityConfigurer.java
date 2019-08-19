package org.springframework.web.servlet.view.velocity;

import java.io.IOException;
import javax.servlet.ServletContext;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.web.context.ServletContextAware;

/**
 * 通过"configLocation"和/或"velocityProperties"和/或"resourceLoaderPath" bean属性为Web使用配置Velocity的JavaBean.
 * 使用此类的最简单方法是仅指定"resourceLoaderPath"; 不需要任何进一步的配置.
 *
 * <pre class="code">
 * &lt;bean id="velocityConfig" class="org.springframework.web.servlet.view.velocity.VelocityConfigurer"&gt;
 *   &lt;property name="resourceLoaderPath">&lt;value&gt;/WEB-INF/velocity/&lt;/value>&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * This bean must be included in the application context of any application
 * using Spring's {@link VelocityView} for web MVC. It exists purely to configure
 * Velocity; it is not meant to be referenced by application components (just
 * internally by VelocityView). This class implements {@link VelocityConfig}
 * in order to be found by VelocityView without depending on the bean name of
 * this configurer. Each DispatcherServlet may define its own VelocityConfigurer
 * if desired, potentially with different template loader paths.
 *
 * <p>Note that you can also refer to a pre-configured VelocityEngine
 * instance via the "velocityEngine" property, e.g. set up by
 * {@link org.springframework.ui.velocity.VelocityEngineFactoryBean},
 * This allows to share a VelocityEngine for web and email usage, for example.
 *
 * <p>This configurer registers the "spring.vm" Velocimacro library for web views
 * (contained in this package and thus in {@code spring.jar}), which makes
 * all of Spring's default Velocity macros available to the views.
 * This allows for using the Spring-provided macros such as follows:
 *
 * <pre class="code">
 * #springBind("person.age")
 * age is ${status.value}</pre>
 *
 * @deprecated 从Spring 4.3开始, 支持FreeMarker
 */
@Deprecated
public class VelocityConfigurer extends org.springframework.ui.velocity.VelocityEngineFactory
		implements VelocityConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

	/** the name of the resource loader for Spring's bind macros */
	private static final String SPRING_MACRO_RESOURCE_LOADER_NAME = "springMacro";

	/** the key for the class of Spring's bind macro resource loader */
	private static final String SPRING_MACRO_RESOURCE_LOADER_CLASS = "springMacro.resource.loader.class";

	/** the name of Spring's default bind macro library */
	private static final String SPRING_MACRO_LIBRARY = "org/springframework/web/servlet/view/velocity/spring.vm";


	private VelocityEngine velocityEngine;

	private ServletContext servletContext;


	/**
	 * Set a pre-configured VelocityEngine to use for the Velocity web
	 * configuration: e.g. a shared one for web and email usage, set up via
	 * {@link org.springframework.ui.velocity.VelocityEngineFactoryBean}.
	 * <p>Note that the Spring macros will <i>not</i> be enabled automatically in
	 * case of an external VelocityEngine passed in here. Make sure to include
	 * {@code spring.vm} in your template loader path in such a scenario
	 * (if there is an actual need to use those macros).
	 * <p>If this is not set, VelocityEngineFactory's properties
	 * (inherited by this class) have to be specified.
	 */
	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Initialize VelocityEngineFactory's VelocityEngine
	 * if not overridden by a pre-configured VelocityEngine.
	 * @see #createVelocityEngine
	 * @see #setVelocityEngine
	 */
	@Override
	public void afterPropertiesSet() throws IOException, VelocityException {
		if (this.velocityEngine == null) {
			this.velocityEngine = createVelocityEngine();
		}
	}

	/**
	 * Provides a ClasspathResourceLoader in addition to any default or user-defined
	 * loader in order to load the spring Velocity macros from the class path.
	 * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
	 */
	@Override
	protected void postProcessVelocityEngine(VelocityEngine velocityEngine) {
		velocityEngine.setApplicationAttribute(ServletContext.class.getName(), this.servletContext);
		velocityEngine.setProperty(
				SPRING_MACRO_RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
		velocityEngine.addProperty(
				VelocityEngine.RESOURCE_LOADER, SPRING_MACRO_RESOURCE_LOADER_NAME);
		velocityEngine.addProperty(
				VelocityEngine.VM_LIBRARY, SPRING_MACRO_LIBRARY);

		if (logger.isInfoEnabled()) {
			logger.info("ClasspathResourceLoader with name '" + SPRING_MACRO_RESOURCE_LOADER_NAME +
					"' added to configured VelocityEngine");
		}
	}

	@Override
	public VelocityEngine getVelocityEngine() {
		return this.velocityEngine;
	}

}
