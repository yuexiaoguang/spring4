package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * 用于异步请求处理的控制器方法返回值类型, 其中一个或多个对象被写入响应.
 *
 * <p>虽然{@link org.springframework.web.context.request.async.DeferredResult}用于生成单个结果,
 * 但{@code ResponseBodyEmitter}可用于发送多个对象, 其中每个对象都使用兼容的
 * {@link org.springframework.http.converter.HttpMessageConverter}写入.
 *
 * <p>作为返回类型支持, 并且在{@link org.springframework.http.ResponseEntity}内支持.
 *
 * <pre>
 * &#064;RequestMapping(value="/stream", method=RequestMethod.GET)
 * public ResponseBodyEmitter handle() {
 * 	   ResponseBodyEmitter emitter = new ResponseBodyEmitter();
 * 	   // Pass the emitter to another component...
 * 	   return emitter;
 * }
 *
 * // in another thread
 * emitter.send(foo1);
 *
 * // and again
 * emitter.send(foo2);
 *
 * // and done
 * emitter.complete();
 * </pre>
 */
public class ResponseBodyEmitter {

	private final Long timeout;

	private final Set<DataWithMediaType> earlySendAttempts = new LinkedHashSet<DataWithMediaType>(8);

	private Handler handler;

	private boolean complete;

	private Throwable failure;

	private final DefaultCallback timeoutCallback = new DefaultCallback();

	private final DefaultCallback completionCallback = new DefaultCallback();


	public ResponseBodyEmitter() {
		this.timeout = null;
	}

	/**
	 * <p>默认情况下不设置, 使用MVC Java Config或MVC命名空间中配置的默认值,
	 * 或者如果未设置, 则超时取决于底层服务器的默认值.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public ResponseBodyEmitter(Long timeout) {
		this.timeout = timeout;
	}


	/**
	 * 返回配置的超时值.
	 */
	public Long getTimeout() {
		return this.timeout;
	}


	synchronized void initialize(Handler handler) throws IOException {
		this.handler = handler;

		for (DataWithMediaType sendAttempt : this.earlySendAttempts) {
			sendInternal(sendAttempt.getData(), sendAttempt.getMediaType());
		}
		this.earlySendAttempts.clear();

		if (this.complete) {
			if (this.failure != null) {
				this.handler.completeWithError(this.failure);
			}
			else {
				this.handler.complete();
			}
		}
		else {
			this.handler.onTimeout(this.timeoutCallback);
			this.handler.onCompletion(this.completionCallback);
		}
	}

	/**
	 * 使用状态码和header更新响应后调用, 如果ResponseBodyEmitter包含在ResponseEntity中,
	 * 但在响应提交之前, i.e. 在响应主体写入之前.
	 * <p>默认实现为空.
	 */
	protected void extendResponse(ServerHttpResponse outputMessage) {
	}

	/**
	 * 将给定对象写入响应.
	 * <p>如果发生任何异常, 则会将调度返回到应用服务器, 其中Spring MVC将通过其异常处理机制传递异常.
	 * 
	 * @param object 要写入的对象
	 * 
	 * @throws IOException
	 * @throws java.lang.IllegalStateException 用于包装其它错误
	 */
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * 使用MediaType提示将给定对象写入响应.
	 * <p>如果发生任何异常, 则会将调度返回到应用服务器, 其中Spring MVC将通过其异常处理机制传递异常.
	 * 
	 * @param object 要写入的对象
	 * @param mediaType 用于选择HttpMessageConverter的MediaType提示
	 * 
	 * @throws IOException
	 * @throws java.lang.IllegalStateException 用于包装其它错误
	 */
	public synchronized void send(Object object, MediaType mediaType) throws IOException {
		Assert.state(!this.complete, "ResponseBodyEmitter is already set complete");
		sendInternal(object, mediaType);
	}

	private void sendInternal(Object object, MediaType mediaType) throws IOException {
		if (object != null) {
			if (this.handler != null) {
				try {
					this.handler.send(object, mediaType);
				}
				catch (IOException ex) {
					throw ex;
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to send " + object, ex);
				}
			}
			else {
				this.earlySendAttempts.add(new DataWithMediaType(object, mediaType));
			}
		}
	}

	/**
	 * 完成请求处理.
	 * <p>在Spring MVC完成异步请求处理的应用服务器中进行调度.
	 * <p><strong>Note:</strong> 在任何{@code send}方法的{@link IOException}之后不需要调用此方法.
	 * Servlet容器将生成一个错误通知, Spring MVC将通过异常解析器机制处理, 然后完成.
	 */
	public synchronized void complete() {
		this.complete = true;
		if (this.handler != null) {
			this.handler.complete();
		}
	}

	/**
	 * 使用错误完成请求处理.
	 * <p>调度进入应用服务器, Spring MVC将通过其异常处理机制传递异常.
	 */
	public synchronized void completeWithError(Throwable ex) {
		this.complete = true;
		this.failure = ex;
		if (this.handler != null) {
			this.handler.completeWithError(ex);
		}
	}

	/**
	 * 注册代码以在异步请求超时时调用. 当异步请求超时时, 从容器线程调用此方法.
	 */
	public synchronized void onTimeout(Runnable callback) {
		this.timeoutCallback.setDelegate(callback);
	}

	/**
	 * 注册代码以在异步请求完成时调用.
	 * 当异步请求因任何原因(包括超时和网络错误)完成时, 将从容器线程调用此方法.
	 * 此方法对于检测{@code ResponseBodyEmitter}实例不再可用非常有用.
	 */
	public synchronized void onCompletion(Runnable callback) {
		this.completionCallback.setDelegate(callback);
	}


	/**
	 * 处理发送的对象并完成请求处理.
	 */
	interface Handler {

		void send(Object data, MediaType mediaType) throws IOException;

		void complete();

		void completeWithError(Throwable failure);

		void onTimeout(Runnable callback);

		void onCompletion(Runnable callback);
	}


	/**
	 * 要写入的数据的简单保存器, 以及用于选择消息转换器的MediaType提示.
	 */
	public static class DataWithMediaType {

		private final Object data;

		private final MediaType mediaType;

		public DataWithMediaType(Object data, MediaType mediaType) {
			this.data = data;
			this.mediaType = mediaType;
		}

		public Object getData() {
			return this.data;
		}

		public MediaType getMediaType() {
			return this.mediaType;
		}
	}


	private class DefaultCallback implements Runnable {

		private Runnable delegate;

		public void setDelegate(Runnable delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run() {
			ResponseBodyEmitter.this.complete = true;
			if (this.delegate != null) {
				this.delegate.run();
			}
		}
	}
}
