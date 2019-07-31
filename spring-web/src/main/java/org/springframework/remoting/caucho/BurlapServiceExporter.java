package org.springframework.remoting.caucho;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.util.NestedServletException;

/**
 * 基于Servlet-API的HTTP请求处理器, 它将指定的服务bean导出为Burlap服务端点, 可通过Burlap代理访问.
 *
 * <p><b>Note:</b> Spring还为Sun的JRE 1.6 HTTP服务器提供了此导出器的替代版本: {@link SimpleBurlapServiceExporter}.
 *
 * <p>Burlap是一种基于XML的轻量级RPC协议.
 * For information on Burlap, see the
 * <a href="http://www.caucho.com/burlap">Burlap website</a>.
 * This exporter requires Burlap 3.x.
 *
 * <p>Note: 使用此类导出的Burlap服务可以由任何Burlap客户端访问, 因为不涉及任何特殊处理.
 *
 * @deprecated 从Spring 4.0开始, 由于Burlap几年没有进展 (与其兄弟Hessian形成鲜明对比)
 */
@Deprecated
public class BurlapServiceExporter extends BurlapExporter implements HttpRequestHandler {

	/**
	 * Processes the incoming Burlap request and creates a Burlap response.
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (!"POST".equals(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(),
					new String[] {"POST"}, "BurlapServiceExporter only supports POST requests");
		}

		try {
		  invoke(request.getInputStream(), response.getOutputStream());
		}
		catch (Throwable ex) {
		  throw new NestedServletException("Burlap skeleton invocation failed", ex);
		}
	}

}
