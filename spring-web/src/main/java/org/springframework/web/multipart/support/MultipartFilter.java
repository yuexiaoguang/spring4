package org.springframework.web.multipart.support;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * Servlet过滤器, 它通过根Web应用程序上下文中的{@link MultipartResolver}解析multipart请求.
 *
 * <p>在Spring的根Web应用程序上下文中查找MultipartResolver.
 * 支持{@code web.xml}中的"multipartResolverBeanName"过滤器init-param; 默认的bean名称是"filterMultipartResolver".
 *
 * <p>如果未找到MultipartResolver bean, 则此过滤器将回退到默认的MultipartResolver:
 * Servlet 3.0的{@link StandardServletMultipartResolver}, 基于{@code web.xml}中的multipart-config部分.
 * 但请注意, 目前Servlet规范仅定义了如何在Servlet上启用multipart配置,
 * 因此, 除非Servlet容器提供诸如Tomcat的"allowCasualMultipartParsing"属性之类的解决方法,
 * 否则可能无法在Filter中进行multipart请求处理.
 *
 * <p>MultipartResolver查找是可自定义的:
 * 覆盖此过滤器的{@code lookupMultipartResolver}方法以使用自定义MultipartResolver实例,
 * 例如, 如果不使用Spring Web应用程序上下文.
 * 请注意, 查找方法不应为每个调用创建新的MultipartResolver实例, 而应返回对预构建实例的引用.
 *
 * <p>Note: 此过滤器是使用DispatcherServlet的MultipartResolver支持的<b>替代</b>,
 * 例如, 对于具有不使用Spring的Web MVC的自定义Web视图的Web应用程序,
 * 或者在Spring MVC DispatcherServlet之前应用的自定义过滤器
 * (e.g. {@link org.springframework.web.filter.HiddenHttpMethodFilter}).
 * 在任何情况下, 此过滤器都不应与特定于servlet的multipart解析结合使用.
 */
public class MultipartFilter extends OncePerRequestFilter {

	public static final String DEFAULT_MULTIPART_RESOLVER_BEAN_NAME = "filterMultipartResolver";

	private final MultipartResolver defaultMultipartResolver = new StandardServletMultipartResolver();

	private String multipartResolverBeanName = DEFAULT_MULTIPART_RESOLVER_BEAN_NAME;


	/**
	 * 设置MultipartResolver的bean名称, 以从Spring的根应用程序上下文中获取.
	 * 默认"filterMultipartResolver".
	 */
	public void setMultipartResolverBeanName(String multipartResolverBeanName) {
		this.multipartResolverBeanName = multipartResolverBeanName;
	}

	/**
	 * 返回MultipartResolver的bean名称, 以从Spring的根应用程序上下文中获取.
	 */
	protected String getMultipartResolverBeanName() {
		return this.multipartResolverBeanName;
	}


	/**
	 * 通过此过滤器的MultipartResolver检查multipart请求,
	 * 并在适当的情况下使用MultipartHttpServletRequest包装原始请求.
	 * <p>过滤器链中的所有后续元素, 最重要的是servlet, 在Multipart情况下受益于正确的参数提取,
	 * 并且如果需要, 可以转换为MultipartHttpServletRequest.
	 */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		MultipartResolver multipartResolver = lookupMultipartResolver(request);

		HttpServletRequest processedRequest = request;
		if (multipartResolver.isMultipart(processedRequest)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving multipart request [" + processedRequest.getRequestURI() +
						"] with MultipartFilter");
			}
			processedRequest = multipartResolver.resolveMultipart(processedRequest);
		}
		else {
			// A regular request...
			if (logger.isDebugEnabled()) {
				logger.debug("Request [" + processedRequest.getRequestURI() + "] is not a multipart request");
			}
		}

		try {
			filterChain.doFilter(processedRequest, response);
		}
		finally {
			if (processedRequest instanceof MultipartHttpServletRequest) {
				multipartResolver.cleanupMultipart((MultipartHttpServletRequest) processedRequest);
			}
		}
	}

	/**
	 * 查找此过滤器应使用的MultipartResolver, 将当前HTTP请求作为参数.
	 * <p>默认实现在没有参数的情况下委托给{@code lookupMultipartResolver}.
	 * 
	 * @return 要使用的MultipartResolver
	 */
	protected MultipartResolver lookupMultipartResolver(HttpServletRequest request) {
		return lookupMultipartResolver();
	}

	/**
	 * 在根Web应用程序上下文中查找MultipartResolver bean.
	 * 支持"multipartResolverBeanName"过滤器init param; 默认的bean名称是"filterMultipartResolver".
	 * <p>可以重写以使用自定义MultipartResolver实例, 例如, 如果不使用Spring Web应用程序上下文.
	 * 
	 * @return MultipartResolver实例, 或{@code null}
	 */
	protected MultipartResolver lookupMultipartResolver() {
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		String beanName = getMultipartResolverBeanName();
		if (wac != null && wac.containsBean(beanName)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver '" + beanName + "' for MultipartFilter");
			}
			return wac.getBean(beanName, MultipartResolver.class);
		}
		else {
			return this.defaultMultipartResolver;
		}
	}
}
