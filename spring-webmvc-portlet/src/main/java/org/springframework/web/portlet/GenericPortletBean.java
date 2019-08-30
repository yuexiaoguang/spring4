package org.springframework.web.portlet;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;

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
import org.springframework.util.StringUtils;
import org.springframework.web.portlet.context.PortletContextResourceLoader;
import org.springframework.web.portlet.context.StandardPortletEnvironment;

/**
 * {@code javax.portlet.GenericPortlet}的简单扩展, 将其配置参数视为bean属性.
 *
 * <p>适用于任何类型的portlet的非常方便的超类. 类型转换是自动的.
 * 子类也可以指定必需的属性.
 *
 * <p>此portlet将请求处理留给子类, 继承GenericPortlet的默认行为 ({@code doDispatch}, {@code processAction}, etc).
 *
 * <p>与加载其自身上下文的FrameworkPortlet类相比, 此portlet超类不依赖于Spring应用程序上下文.
 */
public abstract class GenericPortletBean extends GenericPortlet
		implements EnvironmentCapable, EnvironmentAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 必需的属性 (Strings), 必须作为配置参数提供给此portlet.
	 */
	private final Set<String> requiredProperties = new HashSet<String>();

	private ConfigurableEnvironment environment;


	/**
	 * 子类可以调用此方法来指定此属性 (必须与它们公开的JavaBean属性匹配) 是必需的, 并且必须作为config参数提供.
	 * 通常从子类构造函数调用此方法.
	 * 
	 * @param property 必需的属性的名称
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * 将配置参数映射到此portlet的bean属性, 并调用子类初始化.
	 * 
	 * @throws PortletException 如果bean属性无效 (或缺少必需的属性), 或者子类初始化失败.
	 */
	@Override
	public final void init() throws PortletException {
		if (logger.isInfoEnabled()) {
			logger.info("Initializing portlet '" + getPortletName() + "'");
		}

		// 从init参数设置bean属性.
		try {
			PropertyValues pvs = new PortletConfigPropertyValues(getPortletConfig(), this.requiredProperties);
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
			ResourceLoader resourceLoader = new PortletContextResourceLoader(getPortletContext());
			bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
			initBeanWrapper(bw);
			bw.setPropertyValues(pvs, true);
		}
		catch (BeansException ex) {
			logger.error("Failed to set bean properties on portlet '" + getPortletName() + "'", ex);
			throw ex;
		}

		// 让子类自定义初始化
		initPortletBean();

		if (logger.isInfoEnabled()) {
			logger.info("Portlet '" + getPortletName() + "' configured successfully");
		}
	}

	/**
	 * 初始化此GenericPortletBean的BeanWrapper, 可能使用自定义编辑器.
	 * 
	 * @param bw 要初始化的BeanWrapper
	 * 
	 * @throws BeansException 如果由BeanWrapper方法抛出
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}


	/**
	 * 当没有设置PortletConfig时, 重写的方法只返回{@code null}.
	 */
	@Override
	public final String getPortletName() {
		return (getPortletConfig() != null ? getPortletConfig().getPortletName() : null);
	}

	/**
	 * 当没有设置PortletConfig时, 重写的方法只返回{@code null}.
	 */
	@Override
	public final PortletContext getPortletContext() {
		return (getPortletConfig() != null ? getPortletConfig().getPortletContext() : null);
	}


	/**
	 * 子类可以重写此操作以执行自定义初始化.
	 * 在调用此方法之前, 将设置此portlet的所有bean属性. 默认实现不执行任何操作.
	 * 
	 * @throws PortletException 如果子类初始化失败
	 */
	protected void initPortletBean() throws PortletException {
	}

	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException 如果环境不能分配给{@code ConfigurableEnvironment}.
	 */
	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
		this.environment = (ConfigurableEnvironment)environment;
	}

	/**
	 * {@inheritDoc}
	 * <p>如果为{@code null}, 将通过{@link #createEnvironment()}初始化新环境.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = this.createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回新的{@link StandardPortletEnvironment}.
	 * 子类可以覆盖以配置环境或细化返回的环境类型.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardPortletEnvironment();
	}


	/**
	 * 从PortletConfig init参数创建的PropertyValues实现.
	 */
	@SuppressWarnings("serial")
	private static class PortletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * @param config 从中获取PropertyValues的PortletConfig
		 * @param requiredProperties 需要的一组属性名称, 不能接受默认值
		 * 
		 * @throws PortletException 如果缺少任何必需的属性
		 */
		private PortletConfigPropertyValues(PortletConfig config, Set<String> requiredProperties)
			throws PortletException {

			Set<String> missingProps = (requiredProperties != null && !requiredProperties.isEmpty()) ?
					new HashSet<String>(requiredProperties) : null;

			Enumeration<String> en = config.getInitParameterNames();
			while (en.hasMoreElements()) {
				String property = en.nextElement();
				Object value = config.getInitParameter(property);
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// fail if we are still missing properties
			if (missingProps != null && missingProps.size() > 0) {
				throw new PortletException(
					"Initialization from PortletConfig for portlet '" + config.getPortletName() +
					"' failed; the following required properties were missing: " +
					StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
