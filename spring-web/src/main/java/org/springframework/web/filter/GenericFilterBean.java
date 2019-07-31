package org.springframework.web.filter;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.util.NestedServletException;

/**
 * {@link javax.servlet.Filter}的简单基本实现,
 * 它将其配置参数 ({@code web.xml}中的{@code filter}标记内的{@code init-param}条目) 视为bean属性.
 *
 * <p>适用于任何类型过滤器的便捷超类. 配置参数的类型转换是自动的, 相应的setter方法将使用转换后的值进行调用.
 * 子类也可以指定必需的属性. 不匹配bean属性setter的参数将被忽略.
 *
 * <p>此过滤器将实际过滤留给子类, 子类必须实现{@link javax.servlet.Filter#doFilter}方法.
 *
 * <p>此通用过滤器基类不依赖于Spring {@link org.springframework.context.ApplicationContext}概念.
 * 过滤器通常不加载自己的上下文, 而是从Spring根应用程序上下文访问服务bean,
 * 可通过过滤器的{@link #getServletContext() ServletContext}访问
 * (see {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 */
public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware,
		EnvironmentCapable, ServletContextAware, InitializingBean, DisposableBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private String beanName;

	private Environment environment;

	private ServletContext servletContext;

	private FilterConfig filterConfig;

	private final Set<String> requiredProperties = new HashSet<String>(4);


	/**
	 * 存储Spring bean工厂中定义的bean名称.
	 * <p>仅在初始化为bean的情况下相关, 作为通常由FilterConfig实例提供的过滤器名称的回退的名称.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 设置此过滤器运行的{@code Environment}.
	 * <p>此处设置的任何环境都将覆盖默认提供的{@link StandardServletEnvironment}.
	 * <p>此{@code Environment}对象仅用于解析传递给此过滤器的init参数的资源路径中的占位符.
	 * 如果没有使用init-param, 那么{@code Environment}基本上可以被忽略.
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * 返回与此过滤器关联的{@link Environment}.
	 * <p>如果未指定, 则将通过{@link #createEnvironment()}初始化默认环境.
	 */
	@Override
	public Environment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回{@link StandardServletEnvironment}.
	 * <p>子类可以覆盖它, 以便配置环境或细化返回的环境类型.
	 */
	protected Environment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 存储bean工厂运行的ServletContext.
	 * <p>仅在初始化为bean的情况下相关, 使ServletContext作为通常由FilterConfig实例提供的上下文的回退.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 调用可能包含子类的自定义初始化的{@code initFilterBean()}方法.
	 * <p>仅在初始化为bean的情况下相关, 其中不会调用标准{@code init(FilterConfig)}方法.
	 */
	@Override
	public void afterPropertiesSet() throws ServletException {
		initFilterBean();
	}

	/**
	 * 子类可以重写此操作以执行自定义过滤器关闭.
	 * <p>Note: 将从标准过滤器销毁以及Spring应用程序上下文中的过滤器bean销毁调用此方法.
	 * <p>默认实现为空.
	 */
	@Override
	public void destroy() {
	}


	/**
	 * 子类必须调用此方法来指定此属性 (必须与它们公开的JavaBean属性匹配), 并且必须作为config参数提供.
	 * 这应该从子类的构造函数中调用.
	 * <p>此方法仅适用于由FilterConfig实例驱动的传统初始化.
	 * 
	 * @param property 所需属性的名称
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * 初始化此过滤器的标准方法.
	 * 将配置参数映射到此过滤器的bean属性, 并调用子类初始化.
	 * 
	 * @param filterConfig 此过滤器的配置
	 * 
	 * @throws ServletException 如果bean属性无效 (或缺少必需的属性), 或者子类初始化失败
	 */
	@Override
	public final void init(FilterConfig filterConfig) throws ServletException {
		Assert.notNull(filterConfig, "FilterConfig must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing filter '" + filterConfig.getFilterName() + "'");
		}

		this.filterConfig = filterConfig;

		// 从init参数设置bean属性.
		PropertyValues pvs = new FilterConfigPropertyValues(filterConfig, this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(filterConfig.getServletContext());
				Environment env = this.environment;
				if (env == null) {
					env = new StandardServletEnvironment();
				}
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				String msg = "Failed to set bean properties on filter '" +
					filterConfig.getFilterName() + "': " + ex.getMessage();
				logger.error(msg, ex);
				throw new NestedServletException(msg, ex);
			}
		}

		// Let subclasses do whatever initialization they like.
		initFilterBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Filter '" + filterConfig.getFilterName() + "' configured successfully");
		}
	}

	/**
	 * 为此GenericFilterBean初始化BeanWrapper, 可能使用自定义编辑器.
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
	 * 在调用此方法之前, 将设置此过滤器的所有bean属性.
	 * <p>Note: 将从标准过滤器初始化以及Spring应用程序上下文中的过滤器bean初始化调用此方法.
	 * 两种情况下都可以使用过滤器名称和ServletContext.
	 * <p>默认实现为空.
	 * 
	 * @throws ServletException 如果子类初始化失败
	 */
	protected void initFilterBean() throws ServletException {
	}

	/**
	 * 使此过滤器的FilterConfig可用.
	 * 类似于GenericServlet的{@code getServletConfig()}.
	 * <p>公共类似于WebLogic 6.1附带的Servlet过滤器版本的{@code getFilterConfig()}方法.
	 * 
	 * @return FilterConfig实例, 或{@code null}
	 */
	public final FilterConfig getFilterConfig() {
		return this.filterConfig;
	}

	/**
	 * 使此过滤器的名称可用于子类.
	 * 类似于GenericServlet的{@code getServletName()}.
	 * <p>默认采用FilterConfig的过滤器名称.
	 * 如果在Spring应用程序上下文中初始化为bean, 则它将回退到bean工厂中定义的bean名称.
	 * 
	 * @return 过滤器名称, 或{@code null}
	 */
	protected final String getFilterName() {
		return (this.filterConfig != null ? this.filterConfig.getFilterName() : this.beanName);
	}

	/**
	 * 使此过滤器的ServletContext可用于子类.
	 * 类似于GenericServlet的{@code getServletContext()}.
	 * <p>默认采用FilterConfig的ServletContext.
	 * 如果在Spring应用程序上下文中初始化为bean, 则它将回退到bean工厂运行的ServletContext.
	 * 
	 * @return ServletContext实例, 或{@code null}
	 */
	protected final ServletContext getServletContext() {
		return (this.filterConfig != null ? this.filterConfig.getServletContext() : this.servletContext);
	}


	/**
	 * 从FilterConfig init参数创建的PropertyValues实现.
	 */
	@SuppressWarnings("serial")
	private static class FilterConfigPropertyValues extends MutablePropertyValues {

		/**
		 * @param config 将使用FilterConfig从中获取PropertyValues
		 * @param requiredProperties 需要的一组属性名称, 不能接受默认值
		 * 
		 * @throws ServletException 如果缺少任何必需的属性
		 */
		public FilterConfigPropertyValues(FilterConfig config, Set<String> requiredProperties)
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
						"Initialization from FilterConfig for filter '" + config.getFilterName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}
}
