package org.springframework.web.servlet.support;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * {@link org.springframework.web.WebApplicationInitializer}实现的基类,
 * 它在servlet上下文中注册{@link DispatcherServlet}.
 *
 * <p>具体实现需要实现 {@link #createServletApplicationContext()}, 以及{@link #getServletMappings()},
 * 这两者都可以从{@link #registerDispatcherServlet(ServletContext)}调用.
 * 可以通过覆盖{@link #customizeRegistration(ServletRegistration.Dynamic)}来实现进一步的自定义.
 *
 * <p>因为此类扩展自{@link AbstractContextLoaderInitializer},
 * 所以具体实现还需要实现{@link #createRootApplicationContext()}以设置父级"<strong>root</strong>"应用程序上下文.
 * 如果不需要根上下文，则实现只需在{@code createRootApplicationContext()}实现中返回{@code null}.
 */
public abstract class AbstractDispatcherServletInitializer extends AbstractContextLoaderInitializer {

	/**
	 * 默认的servlet名称. 可以通过覆盖{@link #getServletName}进行自定义.
	 */
	public static final String DEFAULT_SERVLET_NAME = "dispatcher";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		registerDispatcherServlet(servletContext);
	}

	/**
	 * 针对给定的servlet上下文注册{@link DispatcherServlet}.
	 * <p>此方法将创建一个{@code DispatcherServlet}, 其名称由{@link #getServletName()}返回,
	 * 使用从{@link #createServletApplicationContext()}返回的应用程序上下文初始化它,
	 * 并将其映射到从{@link #getServletMappings()}返回的模式.
	 * <p>可以通过覆盖{@link #customizeRegistration(ServletRegistration.Dynamic)}
	 * 或{@link #createDispatcherServlet(WebApplicationContext)}来实现进一步的自定义.
	 * 
	 * @param servletContext 注册servlet的上下文
	 */
	protected void registerDispatcherServlet(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return empty or null");

		WebApplicationContext servletAppContext = createServletApplicationContext();
		Assert.notNull(servletAppContext,
				"createServletApplicationContext() did not return an application " +
				"context for servlet [" + servletName + "]");

		FrameworkServlet dispatcherServlet = createDispatcherServlet(servletAppContext);
		dispatcherServlet.setContextInitializers(getServletApplicationContextInitializers());

		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
		Assert.notNull(registration,
				"Failed to register servlet with name '" + servletName + "'." +
				"Check if there is another servlet registered under the same name.");

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMappings());
		registration.setAsyncSupported(isAsyncSupported());

		Filter[] filters = getServletFilters();
		if (!ObjectUtils.isEmpty(filters)) {
			for (Filter filter : filters) {
				registerServletFilter(servletContext, filter);
			}
		}

		customizeRegistration(registration);
	}

	/**
	 * 返回将注册{@link DispatcherServlet}的名称.
	 * 默认为{@link #DEFAULT_SERVLET_NAME}.
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * 创建要提供给{@code DispatcherServlet}的servlet应用程序上下文.
	 * <p>返回的上下文被委托给Spring的
	 * {@link DispatcherServlet#DispatcherServlet(WebApplicationContext)}.
	 * 因此, 它通常包含控制器, 视图解析器, 区域设置解析器和其他与Web相关的bean.
	 */
	protected abstract WebApplicationContext createServletApplicationContext();

	/**
	 * 使用指定的{@link WebApplicationContext}创建 {@link DispatcherServlet}
	 * (或其他类型的{@link FrameworkServlet}派生的调度器).
	 * <p>Note: 从4.2.3开始, 允许任何{@link FrameworkServlet}子类.
	 * 以前, 它坚持要返回{@link DispatcherServlet}或其子类.
	 */
	protected FrameworkServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
		return new DispatcherServlet(servletAppContext);
	}

	/**
	 * 指定要应用于正在创建{@code DispatcherServlet}的特定于servlet的应用程序上下文的应用程序上下文初始化器.
	 */
	protected ApplicationContextInitializer<?>[] getServletApplicationContextInitializers() {
		return null;
	}

	/**
	 * 指定{@code DispatcherServlet}的servlet映射 &mdash; 例如 {@code "/"}, {@code "/app"}, etc.
	 */
	protected abstract String[] getServletMappings();

	/**
	 * 指定要添加的并映射到{@code DispatcherServlet}的过滤器.
	 * 
	 * @return 过滤器数组或 {@code null}
	 */
	protected Filter[] getServletFilters() {
		return null;
	}

	/**
	 * 将给定的过滤器添加到ServletContext, 并将其映射到{@code DispatcherServlet}, 如下所示:
	 * <ul>
	 * <li>根据其具体类型选择默认过滤器名称
	 * <li>根据{@link #isAsyncSupported() asyncSupported}的返回值设置{@code asyncSupported}标志
	 * <li>使用调度器类型{@code REQUEST}, {@code FORWARD}, {@code INCLUDE}, 和条件{@code ASYNC}创建过滤器映射,
	 * 具体取决于{@link #isAsyncSupported() asyncSupported}的返回值
	 * </ul>
	 * <p>如果上述默认值不合适或不足, 覆盖此方法并直接使用{@code ServletContext}注册过滤器.
	 * 
	 * @param servletContext 用于注册过滤器的servlet上下文
	 * @param filter 要注册的过滤器
	 * 
	 * @return 过滤器注册
	 */
	protected FilterRegistration.Dynamic registerServletFilter(ServletContext servletContext, Filter filter) {
		String filterName = Conventions.getVariableName(filter);
		Dynamic registration = servletContext.addFilter(filterName, filter);
		if (registration == null) {
			int counter = -1;
			while (counter == -1 || registration == null) {
				counter++;
				registration = servletContext.addFilter(filterName + "#" + counter, filter);
				Assert.isTrue(counter < 100,
						"Failed to register filter '" + filter + "'." +
						"Could the same Filter instance have been registered already?");
			}
		}
		registration.setAsyncSupported(isAsyncSupported());
		registration.addMappingForServletNames(getDispatcherTypes(), false, getServletName());
		return registration;
	}

	private EnumSet<DispatcherType> getDispatcherTypes() {
		return (isAsyncSupported() ?
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC) :
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
	}

	/**
	 * 一个地方可以控制{@code DispatcherServlet}的{@code asyncSupported}标志,
	 * 以及通过{@link #getServletFilters()}添加的所有过滤器.
	 * <p>默认为 "true".
	 */
	protected boolean isAsyncSupported() {
		return true;
	}

	/**
	 * {@link #registerDispatcherServlet(ServletContext)}完成后, 可以选择执行进一步的注册自定义.
	 * 
	 * @param registration 要自定义的{@code DispatcherServlet}注册
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}

}
