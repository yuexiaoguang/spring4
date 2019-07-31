package org.springframework.remoting.caucho;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.util.NestedServletException;

/**
 * 基于Servlet-API的HTTP请求处理器, 它将指定的服务bean导出为Hessian服务端点, 可通过Hessian代理访问.
 *
 * <p><b>Note:</b> Spring还为Sun的JRE 1.6 HTTP服务器提供了此导出器的替代版本: {@link SimpleHessianServiceExporter}.
 *
 * <p>Hessian是一种轻量级的二进制RPC协议.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>.
 * <b>Note: 从Spring 4.0开始, 这个导出器需要Hessian 4.0或更高版本.</b>
 *
 * <p>任何Hessian客户端都可以访问使用此类导出的Hessian服务, 因为不涉及任何特殊处理.
 */
public class HessianServiceExporter extends HessianExporter implements HttpRequestHandler {

	/**
	 * 处理传入的Hessian请求并创建Hessian响应.
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (!"POST".equals(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(),
					new String[] {"POST"}, "HessianServiceExporter only supports POST requests");
		}

		response.setContentType(CONTENT_TYPE_HESSIAN);
		try {
			invoke(request.getInputStream(), response.getOutputStream());
		}
		catch (Throwable ex) {
			throw new NestedServletException("Hessian skeleton invocation failed", ex);
		}
	}

}
