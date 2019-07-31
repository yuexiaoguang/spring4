package org.springframework.remoting.httpinvoker;

import java.io.IOException;
import java.io.InvalidClassException;
import java.net.ConnectException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * 用于访问HTTP调用器服务的{@link org.aopalliance.intercept.MethodInterceptor}.
 * 服务URL必须是公开HTTP调用程序服务的HTTP URL.
 *
 * <p>序列化远程调用对象, 并反序列化远程调用结果对象.
 * 像RMI一样使用Java序列化, 但提供与Caucho基于HTTP的Hessian和Burlap协议相同的易用性设置.
 *
 * <P>HTTP调用器是一种非常可扩展和可自定义的协议.
 * 它支持RemoteInvocationFactory机制, 如RMI调用器, 允许包含其他调用属性 (例如, 安全上下文).
 * 此外, 它允许通过{@link HttpInvokerRequestExecutor}策略自定义请求执行.
 *
 * <p>可以使用JDK的{@link java.rmi.server.RMIClassLoader} 从给定的{@link #setCodebaseUrl 代码库}加载类,
 * 从远程位置执行按需动态下载代码.
 * 代码库可以包含多个URL, 以空格分隔.
 * 请注意, RMIClassLoader需要设置SecurityManager, 类似于使用标准RMI的动态类下载!
 * (See the RMI documentation for details.)
 *
 * <p><b>WARNING: 请注意由于不安全的Java反序列化导致的漏洞:
 * 在反序列化步骤中, 操作的输入流可能导致服务器上不需要的代码执行.
 * 因此, 不要将HTTP调用器端点暴露给不受信任的客户端, 而只是在自己的服务之间.</b>
 * 通常, 强烈建议使用任何其他消息格式 (e.g. JSON).
 */
public class HttpInvokerClientInterceptor extends RemoteInvocationBasedAccessor
		implements MethodInterceptor, HttpInvokerClientConfiguration {

	private String codebaseUrl;

	private HttpInvokerRequestExecutor httpInvokerRequestExecutor;


	/**
	 * 设置下载类的代码库URL, 如果从本地未找到.
	 * 可以由多个URL组成, 以空格分隔.
	 * <p>遵循RMI的动态类下载代码库约定.
	 * 与RMI相反, 服务器确定类下载的URL (通过"java.rmi.server.codebase"系统属性), 这是确定代码库URL的客户端.
	 * 服务器通常与服务URL相同, 只是指向不同的路径.
	 */
	public void setCodebaseUrl(String codebaseUrl) {
		this.codebaseUrl = codebaseUrl;
	}

	/**
	 * 返回下载类的代码库URL.
	 */
	@Override
	public String getCodebaseUrl() {
		return this.codebaseUrl;
	}

	/**
	 * 设置用于执行远程调用的HttpInvokerRequestExecutor实现.
	 * <p>默认{@link SimpleHttpInvokerRequestExecutor}.
	 * 或者, 考虑使用{@link HttpComponentsHttpInvokerRequestExecutor}来满足更复杂的需求.
	 */
	public void setHttpInvokerRequestExecutor(HttpInvokerRequestExecutor httpInvokerRequestExecutor) {
		this.httpInvokerRequestExecutor = httpInvokerRequestExecutor;
	}

	/**
	 * 返回此远程访问者使用的HttpInvokerRequestExecutor.
	 * <p>如果尚未初始化执行器, 则创建默认的SimpleHttpInvokerRequestExecutor.
	 */
	public HttpInvokerRequestExecutor getHttpInvokerRequestExecutor() {
		if (this.httpInvokerRequestExecutor == null) {
			SimpleHttpInvokerRequestExecutor executor = new SimpleHttpInvokerRequestExecutor();
			executor.setBeanClassLoader(getBeanClassLoader());
			this.httpInvokerRequestExecutor = executor;
		}
		return this.httpInvokerRequestExecutor;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// 实时初始化默认的HttpInvokerRequestExecutor.
		getHttpInvokerRequestExecutor();
	}


	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "HTTP invoker proxy for service URL [" + getServiceUrl() + "]";
		}

		RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result;

		try {
			result = executeRequest(invocation, methodInvocation);
		}
		catch (Throwable ex) {
			RemoteAccessException rae = convertHttpInvokerAccessException(ex);
			throw (rae != null ? rae : ex);
		}

		try {
			return recreateRemoteInvocationResult(result);
		}
		catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			}
			else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
						"] failed in HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
			}
		}
	}

	/**
	 * 通过{@link HttpInvokerRequestExecutor}执行给定的远程调用.
	 * <p>此实现委托给{@link #executeRequest(RemoteInvocation)}.
	 * 可以重写以对特定的原始MethodInvocation作出反应.
	 * 
	 * @param invocation 要执行的RemoteInvocation
	 * @param originalInvocation 原始MethodInvocation (可以被强制转换为ProxyMethodInvocation接口以访问用户属性)
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws Exception
	 */
	protected RemoteInvocationResult executeRequest(
			RemoteInvocation invocation, MethodInvocation originalInvocation) throws Exception {

		return executeRequest(invocation);
	}

	/**
	 * 通过{@link HttpInvokerRequestExecutor}执行给定的远程调用.
	 * <p>可以在子类中重写, 以将不同的配置对象传递给执行器.
	 * 或者, 在此访问器的子类中添加其他配置属性: 默认情况下, 访问者将自身作为配置对象传递给执行器.
	 * 
	 * @param invocation 要执行的RemoteInvocation
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws IOException
	 * @throws ClassNotFoundException 如果在反序列化期间抛出
	 * @throws Exception
	 */
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation) throws Exception {
		return getHttpInvokerRequestExecutor().executeRequest(this, invocation);
	}

	/**
	 * 将给定的HTTP调用器访问异常转换为适当的Spring {@link RemoteAccessException}.
	 * 
	 * @param ex 要转换的异常
	 * 
	 * @return 要抛出的RemoteAccessException, 或{@code null}将原始异常传播给调用者
	 */
	protected RemoteAccessException convertHttpInvokerAccessException(Throwable ex) {
		if (ex instanceof ConnectException) {
			return new RemoteConnectFailureException(
					"Could not connect to HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
		}

		if (ex instanceof ClassNotFoundException || ex instanceof NoClassDefFoundError ||
				ex instanceof InvalidClassException) {
			return new RemoteAccessException(
					"Could not deserialize result from HTTP invoker remote service [" + getServiceUrl() + "]", ex);
		}

		if (ex instanceof Exception) {
			return new RemoteAccessException(
					"Could not access HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
		}

		// 其它Throwable, e.g. OutOfMemoryError: 按原样传播.
		return null;
	}

}
