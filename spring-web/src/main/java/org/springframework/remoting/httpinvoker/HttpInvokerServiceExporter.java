package org.springframework.remoting.httpinvoker;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.remoting.rmi.RemoteInvocationSerializingExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.util.NestedServletException;

/**
 * 基于Servlet-API的HTTP请求处理器, 它将指定的服务bean导出为HTTP调用器服务端点, 可通过HTTP调用器代理访问.
 *
 * <p><b>Note:</b> Spring还为Sun的JRE 1.6 HTTP服务器提供了此导出器的替代版本: {@link SimpleHttpInvokerServiceExporter}.
 *
 * <p>反序列化远程调用对象并序列化远程调用结果对象.
 * 像RMI一样使用Java序列化, 但提供与Caucho基于HTTP的Hessian和Burlap协议相同的易用性设置.
 *
 * <p><b>HTTP调用器是Java-to-Java远程处理的推荐协议.</b>
 * 它比Hessian和Burlap更强大, 更具扩展性, 但却牺牲了与Java的联系.
 * 然而, 它与Hessian和Burlap一样容易设置, 这是它与RMI相比的主要优势.
 *
 * <p><b>WARNING: 请注意由于不安全的Java反序列化导致的漏洞:
 * 在反序列化步骤中, 操作的输入流可能导致服务器上不需要的代码执行.
 * 因此, 不要将HTTP调用器端点暴露给不受信任的客户端, 而只是在自己的服务之间.</b>
 * 通常, 强烈建议使用任何其他消息格式 (e.g. JSON).
 */
public class HttpInvokerServiceExporter extends RemoteInvocationSerializingExporter implements HttpRequestHandler {

	/**
	 * 从请求中读取远程调用, 执行它, 并将远程调用结果写入响应.
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			RemoteInvocation invocation = readRemoteInvocation(request);
			RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
			writeRemoteInvocationResult(request, response, result);
		}
		catch (ClassNotFoundException ex) {
			throw new NestedServletException("Class not found during deserialization", ex);
		}
	}

	/**
	 * 从给定的HTTP请求中读取RemoteInvocation.
	 * <p>委托给{@link #readRemoteInvocation(HttpServletRequest, InputStream)},
	 * 使用{@link HttpServletRequest#getInputStream() servlet请求的输入流}作为参数.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return RemoteInvocation对象
	 * @throws IOException
	 * @throws ClassNotFoundException 如果通过反序列化抛出
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request)
			throws IOException, ClassNotFoundException {

		return readRemoteInvocation(request, request.getInputStream());
	}

	/**
	 * 从给定的InputStream反序列化RemoteInvocation对象.
	 * <p>使用{@link #decorateInputStream}首先装饰流 (例如, 用于自定义加密或压缩).
	 * 创建{@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream}
	 * 并调用{@link #doReadRemoteInvocation}来实际读取对象.
	 * <p>可以重写以进行调用的自定义序列化.
	 * 
	 * @param request 当前的HTTP请求
	 * @param is 要读取的InputStream
	 * 
	 * @return RemoteInvocation对象
	 * @throws IOException
	 * @throws ClassNotFoundException 如果在反序列化期间抛出
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request, InputStream is)
			throws IOException, ClassNotFoundException {

		ObjectInputStream ois = createObjectInputStream(decorateInputStream(request, is));
		try {
			return doReadRemoteInvocation(ois);
		}
		finally {
			ois.close();
		}
	}

	/**
	 * 返回用于读取远程调用的InputStream, 可能会修饰给定的原始InputStream.
	 * <p>默认实现按原样返回给定的流.
	 * 可以覆盖, 例如, 自定义加密或压缩.
	 * 
	 * @param request 当前的HTTP请求
	 * @param is 原始InputStream
	 * 
	 * @return 可能已装饰的InputStream
	 * @throws IOException
	 */
	protected InputStream decorateInputStream(HttpServletRequest request, InputStream is) throws IOException {
		return is;
	}

	/**
	 * 将给定的RemoteInvocationResult写入给定的HTTP响应.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param result RemoteInvocationResult对象
	 * 
	 * @throws IOException
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result)
			throws IOException {

		response.setContentType(getContentType());
		writeRemoteInvocationResult(request, response, result, response.getOutputStream());
	}

	/**
	 * 将给定的RemoteInvocation序列化到给定的OutputStream.
	 * <p>默认实现使用{@link #decorateOutputStream}首先装饰流 (例如, 用于自定义加密或压缩).
	 * 为最终流创建一个{@link java.io.ObjectOutputStream}
	 * 并调用{@link #doWriteRemoteInvocationResult}来实际写入对象.
	 * <p>可以重写以进行调用的自定义序列化.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param result RemoteInvocationResult对象
	 * @param os 要写入的OutputStream
	 * 
	 * @throws IOException
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result, OutputStream os)
			throws IOException {

		ObjectOutputStream oos =
				createObjectOutputStream(new FlushGuardedOutputStream(decorateOutputStream(request, response, os)));
		try {
			doWriteRemoteInvocationResult(result, oos);
		}
		finally {
			oos.close();
		}
	}

	/**
	 * 返回用于写入远程调用结果的OutputStream, 可能会修改给定的原始OutputStream.
	 * <p>默认实现按原样返回给定的流.
	 * 可以覆盖, 例如, 自定义加密或压缩.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param os 原始OutputStream
	 * 
	 * @return 可能已装饰的OutputStream
	 * @throws IOException
	 */
	protected OutputStream decorateOutputStream(
			HttpServletRequest request, HttpServletResponse response, OutputStream os) throws IOException {

		return os;
	}


	/**
	 * 装饰{@code OutputStream}来防范{@code flush()}调用, 这些调用变为no-ops.
	 * <p>因为{@link ObjectOutputStream#close()}实际上会刷新/消耗底层流两次,
	 * 所以{@link FilterOutputStream}将防止单个刷新调用.
	 * 多次刷新调用可能会导致性能问题, 因为写入不会按原样收集.
	 */
	private static class FlushGuardedOutputStream extends FilterOutputStream {

		public FlushGuardedOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void flush() throws IOException {
			// Do nothing on flush
		}
	}

}
