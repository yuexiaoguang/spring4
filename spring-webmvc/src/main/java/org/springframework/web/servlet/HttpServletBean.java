package org.springframework.web.servlet;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * {@link javax.servlet.http.HttpServlet}的简单扩展,
 * 它将其配置参数 ({@code web.xml}中的{@code servlet}标签内的{@code init-param}条目)视为bean属性.
 *
 * <p>适用于任何类型servlet的便捷超类.
 * 配置参数的类型转换是自动的, 相应的setter方法将使用转换后的值进行调用.
 * 子类也可以指定必需的属性. 不匹配bean属性setter的参数将被忽略.
 *
 * <p>此servlet将请求处理留给子类, 继承HttpServlet的默认行为 ({@code doGet}, {@code doPost}, etc).
 *
 * <p>这个通用servlet基类不依赖于Spring {@link org.springframework.context.ApplicationContext}概念.
 * 简单的servlet通常不加载它们自己的上下文, 而是从Spring根应用程序上下文访问服务bean,
 * 可以通过过滤器的{@link #getServletContext() ServletContext}访问
 * (see {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 *
 * <p>{@link FrameworkServlet}类是一个更具体的servlet基类, 它加载自己的应用程序上下文.
 * FrameworkServlet是Spring完整成熟{@link DispatcherServlet}的直接基类.
 */
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ConfigurableEnvironment environment;

	private final Set<String> requiredProperties = new HashSet<String>(4);


	/**
	 * 子类可以调用此方法来指定此属性 (必须与它们公开的JavaBean属性匹配) 是必需的, 并且必须作为config参数提供.
	 * 这应该从子类的构造函数中调用.
	 * <p>此方法仅适用于由ServletConfig实例驱动的传统初始化.
	 * 
	 * @param property 所需属性的名称
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * 设置此servlet运行的{@code Environment}.
	 * <p>此处设置的任何环境都将覆盖默认提供的{@link StandardServletEnvironment}.
	 * 
	 * @throws IllegalArgumentException 如果环境不能分配给{@code ConfigurableEnvironment}
	 */
	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * 返回与此servlet关联的{@link Environment}.
	 * <p>如果未指定, 则将通过{@link #createEnvironment()}初始化默认环境.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回一个新的{@link StandardServletEnvironment}.
	 * <p>子类可以覆盖它, 以便配置环境或细化返回的环境类型.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 将配置参数映射到此servlet的bean属性, 并调用子类初始化.
	 * 
	 * @throws ServletException 如果bean属性无效 (或缺少必需的属性), 或者子类初始化失败.
	 */
	@Override
	public final void init() throws ServletException {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing servlet '" + getServletName() + "'");
		}

		// 从init参数设置bean属性.
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// 让子类初始化.
		initServletBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Servlet '" + getServletName() + "' configured successfully");
		}
	}

	/**
	 * 初始化此HttpServletBean的BeanWrapper, 可能使用自定义编辑器.
	 * <p>默认实现为空.
	 * 
	 * @param bw 要初始化的BeanWrapper
	 * 
	 * @throws BeansException 如果由BeanWrapper方法抛出
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * 子类可以重写此操作以执行自定义初始化.
	 * 在调用此方法之前, 将设置此servlet的所有bean属性.
	 * <p>默认实现为空.
	 * 
	 * @throws ServletException 如果子类初始化失败
	 */
	protected void initServletBean() throws ServletException {
	}

	/**
	 * 当没有设置ServletConfig时, 重写的方法只返回{@code null}.
	 */
	@Override
	public final String getServletName() {
		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
	}

	/**
	 * 当没有设置ServletConfig时, 重写的方法只返回{@code null}.
	 */
	@Override
	public final ServletContext getServletContext() {
		return (getServletConfig() != null ? getServletConfig().getServletContext() : null);
	}


	/**
	 * 从ServletConfig init参数创建的PropertyValues实现.
	 */
	private static class ServletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * @param config 将从中获取PropertyValues的ServletConfig
		 * @param requiredProperties 需要的一组属性名称, 不能接受默认值
		 * 
		 * @throws ServletException 如果缺少任何必需的属性
		 */
		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {

			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<String>(requiredProperties) : null);

			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// 如果仍然缺少属性, 则失败.
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}
}
