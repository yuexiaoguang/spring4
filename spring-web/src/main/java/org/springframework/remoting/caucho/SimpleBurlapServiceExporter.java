package org.springframework.remoting.caucho;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.springframework.lang.UsesSunHttpServer;
import org.springframework.util.FileCopyUtils;

/**
 * HTTP请求处理器, 将指定的服务bean导出为Burlap服务端点, 可通过Burlap代理访问.
 * 专为Sun的JRE 1.6 HTTP服务器设计, 实现{@link com.sun.net.httpserver.HttpHandler}接口.
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
@UsesSunHttpServer
public class SimpleBurlapServiceExporter extends BurlapExporter implements HttpHandler {

	/**
	 * Processes the incoming Burlap request and creates a Burlap response.
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
			logger.error("Burlap skeleton invocation failed", ex);
		}

		exchange.sendResponseHeaders(200, output.size());
		FileCopyUtils.copy(output.toByteArray(), exchange.getResponseBody());
	}

}
