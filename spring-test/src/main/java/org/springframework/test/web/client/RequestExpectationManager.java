package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 封装实现{@link MockRestServiceServer}所需的行为, 包括其public API (创建期望 + 验证/重置),
 * 以及验证实际请求的额外方法.
 *
 * <p>此约定不直接在应用程序中使用, 但自定义实现可以通过{@code MockRestServiceServer}构建器
 * {@link org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder#build(RequestExpectationManager) 插入}.
 */
public interface RequestExpectationManager {

	/**
	 * 设置新的请求期望. 返回的{@link ResponseActions}用于添加更多期望并定义响应.
	 * <p>这是{@link MockRestServiceServer#expect(ExpectedCount, RequestMatcher)}的委托.
	 *
	 * @param requestMatcher 请求期望
	 * 
	 * @return 建立进一步的期望并定义响应
	 */
	ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher);

	/**
	 * 确认已满足所有期望.
	 * <p>这是{@link MockRestServiceServer#verify()}的委托.
	 * 
	 * @throws AssertionError 当一些期望没有得到满足时
	 */
	void verify();

	/**
	 * 重置内部状态, 删除所有期望和记录的请求.
	 * <p>这是{@link MockRestServiceServer#reset()}的委托.
	 */
	void reset();


	/**
	 * 根据声明的期望验证给定的实际请求.
	 * 成功返回模拟响应或引发错误.
	 * <p>这在{@link MockRestServiceServer}中用于实际请求.
	 * 
	 * @param request 请求
	 * 
	 * @return 如果请求已经过验证, 则返回响应.
	 * @throws AssertionError 当一些期望没有得到满足时
	 * @throws IOException 如果有任何验证错误
	 */
	ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException;

}
