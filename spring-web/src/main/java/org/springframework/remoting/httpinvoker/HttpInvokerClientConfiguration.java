package org.springframework.remoting.httpinvoker;

/**
 * 用于执行HTTP调用器请求的配置接口.
 */
public interface HttpInvokerClientConfiguration {

	/**
	 * 返回目标服务的HTTP URL.
	 */
	String getServiceUrl();

	/**
	 * 如果在本地找不到, 则返回代码库URL以下载类.
	 * 可以包含多个URL, 以空格分隔.
	 * 
	 * @return 代码库URL, 或{@code null}
	 */
	String getCodebaseUrl();

}
