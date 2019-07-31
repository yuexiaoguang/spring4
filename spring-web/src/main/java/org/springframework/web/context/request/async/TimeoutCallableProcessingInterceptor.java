package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 如果响应尚未提交，则在超时的情况下发送503 (SERVICE_UNAVAILABLE).
 * 从4.2.8开始, 这是通过将结果设置为{@link AsyncRequestTimeoutException}来间接完成的,
 * 然后由Spring MVC的默认异常处理处理为503错误.
 *
 * <p>在所有其他拦截器之后注册, 因此仅在没有其他拦截器处理超时时调用.
 *
 * <p>请注意, 根据RFC 7231, 没有'Retry-After' header的503被解释为500错误, 客户端不应重试.
 * 应用程序可以安装自己的拦截器来处理超时, 并在必要时添加'Retry-After' header.
 */
public class TimeoutCallableProcessingInterceptor extends CallableProcessingInterceptorAdapter {

	@Override
	public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
		return new AsyncRequestTimeoutException();
	}

}
