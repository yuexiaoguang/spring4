package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor},
 * 它将初始化和销毁​​回调应用于实现{@link javax.servlet.Servlet}接口的bean.
 *
 * <p>初始化bean实例后, 将使用ServletConfig调用Servlet {@code init}方法,
 * 该ServletConfig包含Servlet的bean名称和运行它的ServletContext.
 *
 * <p>在销毁bean实例之前, 将调用Servlet {@code destroy}.
 *
 * <p><b>请注意, 此后处理器不支持Servlet初始化参数.</b>
 * 实现Servlet接口的Bean实例应该像任何其他Spring bean一样配置, 即通过构造函数参数或bean属性.
 *
 * <p>为了在普通的Servlet容器中重用Servlet实现, 并作为Spring上下文中的bean,
 * 考虑从Spring的{@link org.springframework.web.servlet.HttpServletBean}基类派生,
 * 该基类将Servlet初始化参数应用为bean属性, 同时支持标准的Servlet和Spring bean初始化样式.
 *
 * <p><b>或者, 考虑使用Spring的{@link org.springframework.web.servlet.mvc.ServletWrappingController}包装一个Servlet.</b>
 * 这特别适用于现有的Servlet类, 允许指定Servlet初始化参数等.
 */
public class SimpleServletPostProcessor implements
		DestructionAwareBeanPostProcessor, ServletContextAware, ServletConfigAware {

	private boolean useSharedServletConfig = true;

	private ServletContext servletContext;

	private ServletConfig servletConfig;


	/**
	 * 设置是否使用通过{@code setServletConfig}传入的共享ServletConfig对象.
	 * <p>默认为"true".
	 * 设置为"false"以传入模拟ServletConfig对象, 其中bean名称为servlet名称, 保存当前ServletContext.
	 */
	public void setUseSharedServletConfig(boolean useSharedServletConfig) {
		this.useSharedServletConfig = useSharedServletConfig;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			ServletConfig config = this.servletConfig;
			if (config == null || !this.useSharedServletConfig) {
				config = new DelegatingServletConfig(beanName, this.servletContext);
			}
			try {
				((Servlet) bean).init(config);
			}
			catch (ServletException ex) {
				throw new BeanInitializationException("Servlet.init threw exception", ex);
			}
		}
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			((Servlet) bean).destroy();
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return (bean instanceof Servlet);
	}


	/**
	 * {@link ServletConfig}接口的内部实现, 将传递给包装的servlet.
	 */
	private static class DelegatingServletConfig implements ServletConfig {

		private final String servletName;

		private final ServletContext servletContext;

		public DelegatingServletConfig(String servletName, ServletContext servletContext) {
			this.servletName = servletName;
			this.servletContext = servletContext;
		}

		@Override
		public String getServletName() {
			return this.servletName;
		}

		@Override
		public ServletContext getServletContext() {
			return this.servletContext;
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
