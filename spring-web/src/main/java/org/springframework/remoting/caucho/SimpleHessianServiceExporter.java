package org.springframework.remoting.caucho;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.springframework.lang.UsesSunHttpServer;
import org.springframework.util.FileCopyUtils;

/**
 * HTTP请求处理器, 将指定的服务bean导出为Hessian服务端点, 可通过Hessian代理访问.
 * 专为Sun的JRE 1.6 HTTP服务器设计, 实现{@link com.sun.net.httpserver.HttpHandler}接口.
 *
 * <p>Hessian是一种轻量级的二进制RPC协议.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>.
 * <b>Note: 从Spring 4.0开始, 这个导出器需要Hessian 4.0或更高版本.</b>
 *
 * <p>任何Hessian客户端都可以访问使用此类导出的Hessian服务, 因为不涉及任何特殊处理.
 */
@UsesSunHttpServer
public class SimpleHessianServiceExporter extends HessianExporter implements HttpHandler {

	/**
	 * 处理传入的Hessian请求并创建Hessian响应.
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			exchange.getResponseHeaders().set("Allow", "POST");
			exchange.sendResponseHeaders(405, -1);
			return;
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
		try {
			invoke(exchange.getRequestBody(), output);
		}
		catch (Throwable ex) {
			exchange.sendResponseHeaders(500, -1);
			logger.error("Hessian skeleton invocation failed", ex);
			return;
		}

		exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_HESSIAN);
		exchange.sendResponseHeaders(200, output.size());
		FileCopyUtils.copy(output.toByteArray(), exchange.getResponseBody());
	}

}
