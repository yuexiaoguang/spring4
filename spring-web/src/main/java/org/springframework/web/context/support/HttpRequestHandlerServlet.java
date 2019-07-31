package org.springframework.web.context.support;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.WebApplicationContext;

/**
 * 简单的HttpServlet, 它委托给Spring的根Web应用程序上下文中定义的{@link HttpRequestHandler} bean.
 * 目标bean名称必须与{@code web.xml}中定义的HttpRequestHandlerServlet servlet-name匹配.
 *
 * <p>根据HttpRequestHandlerServlet定义, 这可以用于公开单个Spring远程导出器,
 * 例如{@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}
 * 或{@link org.springframework.remoting.caucho.HessianServiceExporter}.
 * 这是在DispatcherServlet上下文中将远程导出器定义为bean的最小替代方法 (在那里可以使用高级映射和拦截工具).
 */
@SuppressWarnings("serial")
public class HttpRequestHandlerServlet extends HttpServlet {

	private HttpRequestHandler target;


	@Override
	public void init() throws ServletException {
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		this.target = wac.getBean(getServletName(), HttpRequestHandler.class);
	}


	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		LocaleContextHolder.setLocale(request.getLocale());
		try {
			this.target.handleRequest(request, response);
		}
		catch (HttpRequestMethodNotSupportedException ex) {
			String[] supportedMethods = ex.getSupportedMethods();
			if (supportedMethods != null) {
				response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
			}
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

}
