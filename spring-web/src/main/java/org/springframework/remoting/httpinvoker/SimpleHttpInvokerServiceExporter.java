package org.springframework.remoting.httpinvoker;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.springframework.lang.UsesSunHttpServer;
import org.springframework.remoting.rmi.RemoteInvocationSerializingExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * HTTP请求处理器, 它将指定的服务bean导出为HTTP调用器服务端点, 可通过HTTP调用器代理访问.
 * 专为Sun的JRE 1.6 HTTP服务器设计, 实现{@link com.sun.net.httpserver.HttpHandler}接口.
 *
 * <p>反序列化远程调用对象, 并序列化远程调用结果对象.
 * 像RMI一样使用Java序列化, 但提供与Caucho基于HTTP的Hessian和Burlap协议相同的易用性设置.
 *
 * <p><b>HTTP调用器是Java-to-Java远程处理的推荐协议.</b>
 * 它比Hessian和Burlap更强大, 更具扩展性, 但却牺牲了与Java的联系.
 * 然而, 它与Hessian和Burlap一样容易设置, 这是它与RMI相比的主要优势.
 *
 * <p><b>WARNING: 请注意由于不安全的Java反序列化导致的漏洞:
 * 在反序列化步骤中, 操作的输入流可能导致服务器上不需要的代码执行.
 * 因此, 不要将HTTP调用器端点暴露给不受信任的客户端, 而只是在您自己的服务之间.</b>
 * 通常, 强烈建议使用任何其他消息格式 (e.g. JSON).
 */
@UsesSunHttpServer
public class SimpleHttpInvokerServiceExporter extends RemoteInvocationSerializingExporter implements HttpHandler {

	/**
	 * 从请求中读取远程调用, 执行它, 并将远程调用结果写入响应.
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			RemoteInvocation invocation = readRemoteInvocation(exchange);
			RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
			writeRemoteInvocationResult(exchange, result);
			exchange.close();
		}
		catch (ClassNotFoundException ex) {
			exchange.sendResponseHeaders(500, -1);
			logger.error("Class not found during deserialization", ex);
		}
	}

	/**
	 * 从给定的HTTP请求中读取RemoteInvocation.
	 * <p>使用{@link HttpExchange#getRequestBody()}请求的输入流,
	 * 委托给{@link #readRemoteInvocation(HttpExchange, InputStream)}.
	 * 
	 * @param exchange 当前的HTTP请求/响应
	 * 
	 * @return the RemoteInvocation object
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException 反序列化错误
	 */
	protected RemoteInvocation readRemoteInvocation(HttpExchange exchange)
			throws IOException, ClassNotFoundException {

		return readRemoteInvocation(exchange, exchange.getRequestBody());
	}

	/**
	 * 从给定的InputStream反序列化RemoteInvocation对象.
	 * <p>{@link #decorateInputStream}首先装饰流 (例如, 用于自定义加密或压缩).
	 * 创建{@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream}
	 * 并调用{@link #doReadRemoteInvocation}实际读取对象.
	 * <p>可以重写以进行调用的自定义序列化.
	 * 
	 * @param exchange 当前的HTTP请求/响应
	 * @param is 要读取的InputStream
	 * 
	 * @return RemoteInvocation对象
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException 反序列化错误
	 */
	protected RemoteInvocation readRemoteInvocation(HttpExchange exchange, InputStream is)
			throws IOException, ClassNotFoundException {

		ObjectInputStream ois = createObjectInputStream(decorateInputStream(exchange, is));
		return doReadRemoteInvocation(ois);
	}

	/**
	 * 返回用于读取远程调用的InputStream, 可能会修饰给定的原始InputStream.
	 * <p>默认实现按原样返回给定的流.
	 * 可以覆盖, 例如, 自定义加密或压缩.
	 * 
	 * @param exchange 当前的HTTP请求/响应
	 * @param is 原始InputStream
	 * 
	 * @return 可能已装饰的InputStream
	 * @throws java.io.IOException
	 */
	protected InputStream decorateInputStream(HttpExchange exchange, InputStream is) throws IOException {
		return is;
	}

	/**
	 * 将给定的RemoteInvocationResult写入给定的HTTP响应.
	 * 
	 * @param exchange 当前的HTTP请求/响应
	 * @param result RemoteInvocationResult对象
	 * 
	 * @throws java.io.IOException
	 */
	protected void writeRemoteInvocationResult(HttpExchange exchange, RemoteInvocationResult result)
			throws IOException {

		exchange.getResponseHeaders().set("Content-Type", getContentType());
		exchange.sendResponseHeaders(200, 0);
		writeRemoteInvocationResult(exchange, result, exchange.getResponseBody());
	}

	/**
	 * 将给定的RemoteInvocation序列化到给定的OutputStream.
	 * <p>默认实现使用{@link #decorateOutputStream}首先装饰流 (例如, 用于自定义加密或压缩).
	 * 为最终流创建一个{@link java.io.ObjectOutputStream}并调用{@link #doWriteRemoteInvocationResult}来实际写入对象.
	 * <p>可以重写以进行调用的自定义序列化.
	 * 
	 * @param exchange 当前的HTTP请求/响应
	 * @param result RemoteInvocationResult对象
	 * @param os 要写入的OutputStream
	 * 
	 * @throws java.io.IOException
	 */
	protected void writeRemoteInvocationResult(
			HttpExchange exchange, RemoteInvocationResult result, OutputStream os) throws IOException {

		ObjectOutputStream oos = createObjectOutputStream(decorateOutputStream(exchange, os));
		doWriteRemoteInvocationResult(result, oos);
		oos.flush();
	}

	/**
	 * 返回用于写入远程调用结果的OutputStream, 可能会修改给定的原始OutputStream.
	 * <p>默认实现按原样返回给定的流.
	 * 可以覆盖, 例如, 自定义加密或压缩.
	 * 
	 * @param exchange 当前的HTTP请求/响应
	 * @param os 原始的OutputStream
	 * 
	 * @return 可能已装饰的OutputStream
	 * @throws java.io.IOException
	 */
	protected OutputStream decorateOutputStream(HttpExchange exchange, OutputStream os) throws IOException {
		return os;
	}

}
