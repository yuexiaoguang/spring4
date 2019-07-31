package org.springframework.web.context.request.async;

/**
 * 异步请求超时时抛出的异常.
 * 或者, 应用程序可以注册{@link DeferredResultProcessingInterceptor}
 * 或{@link CallableProcessingInterceptor}处理超时,
 * 通过MVC Java配置或MVC XML命名空间或直接通过{@code RequestMappingHandlerAdapter}的属性.
 *
 * <p>默认情况下, 异常将作为503错误处理.
 */
@SuppressWarnings("serial")
public class AsyncRequestTimeoutException extends RuntimeException {

}
