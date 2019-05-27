package org.springframework.web.context.request.async;

/**
 * Exception to be thrown when an async request times out.
 * Alternatively an applications can register a
 * {@link DeferredResultProcessingInterceptor} or a
 * {@link CallableProcessingInterceptor} to handle the timeout through
 * the MVC Java config or the MVC XML namespace or directly through properties
 * of the {@code RequestMappingHandlerAdapter}.
 *
 * <p>By default the exception will be handled as a 503 error.
 */
@SuppressWarnings("serial")
public class AsyncRequestTimeoutException extends RuntimeException {

}
