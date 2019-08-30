package org.springframework.web.portlet.handler;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.web.portlet.context.PortletConfigAware;
import org.springframework.web.portlet.context.PortletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor},
 * 将初始化和销毁​​回调应用于实现{@link javax.portlet.Portlet}接口的bean.
 *
 * <p>在初始化bean实例之后, 将使用PortletConfig调用Portlet {@code init}方法,
 * 该PortletConfig包含Portlet的bean名称和运行它的PortletContext.
 *
 * <p>在销毁bean实例之前, 将调用Portlet {@code destroy}.
 *
 * <p><b>请注意, 此后处理器不支持Portlet初始化参数.</b>
 * 实现Portlet接口的Bean实例应该像任何其他Spring bean一样配置, 即通过构造函数参数或bean属性.
 *
 * <p>要在普通的Portlet容器中重用Portlet实现, 并在Spring上下文中作为bean,
 * 请考虑从Spring的{@link org.springframework.web.portlet.GenericPortletBean}基类派生,
 * 该基类将Portlet初始化参数应用为bean属性, 支持标准Portlet和Spring bean初始化样式.
 *
 * <p><b>或者, 考虑使用Spring的{@link org.springframework.web.portlet.mvc.PortletWrappingController}包装Portlet.</b>
 * 这特别适用于现有的Portlet类, 允许指定Portlet初始化参数等.
 */
public class SimplePortletPostProcessor
	implements DestructionAwareBeanPostProcessor, PortletContextAware, PortletConfigAware {

	private boolean useSharedPortletConfig = true;

	private PortletContext portletContext;

	private PortletConfig portletConfig;


	/**
	 * 设置是否使用通过{@code setPortletConfig}传入的共享PortletConfig对象.
	 * <p>默认为"true".
	 * 将此设置设置为"false"以传入模拟PortletConfig对象, 其中bean名称为portlet名称, 并保存当前的PortletContext.
	 */
	public void setUseSharedPortletConfig(boolean useSharedPortletConfig) {
		this.useSharedPortletConfig = useSharedPortletConfig;
	}

	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}

	@Override
	public void setPortletConfig(PortletConfig portletConfig) {
		this.portletConfig = portletConfig;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Portlet) {
			PortletConfig config = this.portletConfig;
			if (config == null || !this.useSharedPortletConfig) {
				config = new DelegatingPortletConfig(beanName, this.portletContext, this.portletConfig);
			}
			try {
				((Portlet) bean).init(config);
			}
			catch (PortletException ex) {
				throw new BeanInitializationException("Portlet.init threw exception", ex);
			}
		}
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Portlet) {
			((Portlet) bean).destroy();
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return (bean instanceof Portlet);
	}


	/**
	 * PortletConfig接口的内部实现, 传递给包装的servlet.
	 */
	private static class DelegatingPortletConfig implements PortletConfig {

		private final String portletName;

		private final PortletContext portletContext;

		private final PortletConfig portletConfig;

		public DelegatingPortletConfig(String portletName, PortletContext portletContext, PortletConfig portletConfig) {
			this.portletName = portletName;
			this.portletContext = portletContext;
			this.portletConfig = portletConfig;
		}

		@Override
		public String getPortletName() {
			return this.portletName;
		}

		@Override
		public PortletContext getPortletContext() {
			return this.portletContext;
		}

		@Override
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.<String>emptySet());
		}

		@Override
		public ResourceBundle getResourceBundle(Locale locale) {
			return (this.portletConfig != null ? this.portletConfig.getResourceBundle(locale) : null);
		}

		@Override
		public Enumeration<String> getPublicRenderParameterNames() {
			return Collections.enumeration(Collections.<String>emptySet());
		}

		@Override
		public String getDefaultNamespace() {
			return XMLConstants.NULL_NS_URI;
		}

		@Override
		public Enumeration<QName> getPublishingEventQNames() {
			return Collections.enumeration(Collections.<QName>emptySet());
		}

		@Override
		public Enumeration<QName> getProcessingEventQNames() {
			return Collections.enumeration(Collections.<QName>emptySet());
		}

		@Override
		public Enumeration<Locale> getSupportedLocales() {
			return Collections.enumeration(Collections.<Locale>emptySet());
		}

		@Override
		public Map<String, String[]> getContainerRuntimeOptions() {
			return (this.portletConfig != null ? this.portletConfig.getContainerRuntimeOptions() : null);
		}
	}

}
