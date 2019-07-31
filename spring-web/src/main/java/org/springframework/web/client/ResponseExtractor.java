package org.springframework.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link RestTemplate}检索方法使用的通用回调接口.
 * 此接口的实现实际从{@link ClientHttpResponse}中提取数据, 但不需要担心异常处理或关闭资源.
 *
 * <p>由{@link RestTemplate}在内部使用, 但对应用程序代码也很有用.
 */
public interface ResponseExtractor<T> {

	/**
	 * 从给定的{@code ClientHttpResponse}中提取数据并将其返回.
	 * 
	 * @param response HTTP响应
	 * 
	 * @return 提取的数据
	 * @throws IOException
	 */
	T extractData(ClientHttpResponse response) throws IOException;

}
