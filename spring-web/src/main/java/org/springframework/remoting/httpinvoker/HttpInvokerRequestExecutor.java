package org.springframework.remoting.httpinvoker;

import java.io.IOException;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * 用于实际执行HTTP调用器请求的策略接口.
 * 由HttpInvokerClientInterceptor及其子类HttpInvokerProxyFactoryBean使用.
 *
 * <p>开箱即用的两种实现方式:
 * <ul>
 * <li><b>{@code SimpleHttpInvokerRequestExecutor}:</b>
 * 使用JDK工具执行POST请求, 不支持HTTP身份验证或高级配置选项.
 * <li><b>{@code HttpComponentsHttpInvokerRequestExecutor}:</b>
 * 使用Apache的Commons HttpClient执行POST请求, 允许使用预配置的HttpClient实例 (可能有身份验证, HTTP连接池等).
 * </ul>
 */
public interface HttpInvokerRequestExecutor {

	/**
	 * 执行发送给定远程调用的请求.
	 * 
	 * @param config HTTP调用器配置, 指定目标服务
	 * @param invocation 要执行的RemoteInvocation
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws IOException 如果由I/O操作抛出
	 * @throws ClassNotFoundException 如果在反序列化期间抛出
	 * @throws Exception 在一般错误的情况下
	 */
	RemoteInvocationResult executeRequest(HttpInvokerClientConfiguration config, RemoteInvocation invocation)
			throws Exception;

}
