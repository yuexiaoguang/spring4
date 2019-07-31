package org.springframework.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;

/**
 * 在{@link ClientHttpRequest}上操作的代码的回调接口.
 * 允许操作请求header, 并写入请求正文.
 *
 * <p>由{@link RestTemplate}在内部使用, 但对应用程序代码也很有用.
 */
public interface RequestCallback {

	/**
	 * 使用打开的{@code ClientHttpRequest}由{@link RestTemplate#execute}调用.
	 * 不需要关心关闭请求或处理错误:
	 * 这将由{@code RestTemplate}处理.
	 * 
	 * @param request 活动的HTTP请求
	 * 
	 * @throws IOException
	 */
	void doWithRequest(ClientHttpRequest request) throws IOException;

}
