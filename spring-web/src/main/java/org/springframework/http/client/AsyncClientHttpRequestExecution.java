package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 表示客户端HTTP请求执行的上下文.
 *
 * <p>用于调用拦截器链中的下一个拦截器, 或者 - 如果调用拦截器是最后一个 - 执行请求本身.
 */
public interface AsyncClientHttpRequestExecution {

    /**
     * 通过调用链中的下一个拦截器或执行对远程服务的请求来恢复请求执行.
     * 
     * @param request HTTP请求，包含HTTP方法和header
     * @param body 请求的主体
     * 
     * @return 相应的Future句柄
     * @throws IOException
     */
    ListenableFuture<ClientHttpResponse> executeAsync(HttpRequest request, byte[] body) throws IOException;

}
