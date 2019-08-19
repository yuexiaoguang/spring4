package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;

/**
 * {@link ResponseBodyEmitter}的细化, 用于发送
 * <a href="http://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
 */
public class SseEmitter extends ResponseBodyEmitter {

	static final MediaType TEXT_PLAIN = new MediaType("text", "plain", Charset.forName("UTF-8"));

	static final MediaType UTF8_TEXT_EVENTSTREAM = new MediaType("text", "event-stream", Charset.forName("UTF-8"));


	public SseEmitter() {
		super();
	}

	/**
	 * <p>默认情况下不设置, 使用MVC Java Config或MVC命名空间中配置的默认值,
	 * 或者如果未设置, 则超时取决于底层服务器的默认值.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public SseEmitter(Long timeout) {
		super(timeout);
	}


	@Override
	protected void extendResponse(ServerHttpResponse outputMessage) {
		super.extendResponse(outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		if (headers.getContentType() == null) {
			headers.setContentType(UTF8_TEXT_EVENTSTREAM);
		}
	}

	/**
	 * 发送格式化为单个SSE "data"行的对象. 相当于:
	 * <pre>
	 * // static import of SseEmitter.*
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject));
	 * </pre>
	 * 
	 * @param object 要写入的对象
	 * 
	 * @throws IOException
	 * @throws java.lang.IllegalStateException 包装其它错误
	 */
	@Override
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * 发送格式化为单个SSE "data"行的对象. 相当于:
	 * <pre>
	 * // static import of SseEmitter.*
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject, MediaType.APPLICATION_JSON));
	 * </pre>
	 * 
	 * @param object 要写入的对象
	 * @param mediaType 用于选择HttpMessageConverter的MediaType提示
	 * 
	 * @throws IOException
	 */
	@Override
	public void send(Object object, MediaType mediaType) throws IOException {
		if (object != null) {
			send(event().data(object, mediaType));
		}
	}

	/**
	 * 发送使用给定构建器准备的SSE事件. 例如:
	 * <pre>
	 * // static import of SseEmitter
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().name("update").id("1").data(myObject));
	 * </pre>
	 * 
	 * @param builder SSE格式化事件的构建器.
	 * 
	 * @throws IOException
	 */
	public void send(SseEventBuilder builder) throws IOException {
		Set<DataWithMediaType> dataToSend = builder.build();
		synchronized (this) {
			for (DataWithMediaType entry : dataToSend) {
				super.send(entry.getData(), entry.getMediaType());
			}
		}
	}


	public static SseEventBuilder event() {
		return new SseEventBuilderImpl();
	}


	/**
	 * SSE事件的构建器.
	 */
	public interface SseEventBuilder {

		/**
		 * 添加SSE "comment"行.
		 */
		SseEventBuilder comment(String comment);

		/**
		 * 添加SSE "event"行.
		 */
		SseEventBuilder name(String eventName);

		/**
		 * 添加SSE "id"行.
		 */
		SseEventBuilder id(String id);

		/**
		 * 添加SSE "event"行.
		 */
		SseEventBuilder reconnectTime(long reconnectTimeMillis);

		/**
		 * 添加SSE "data"行.
		 */
		SseEventBuilder data(Object object);

		/**
		 * 添加SSE "data"行.
		 */
		SseEventBuilder data(Object object, MediaType mediaType);

		/**
		 * 返回一个或多个Object-MediaType对以通过{@link #send(Object, MediaType)}进行写入.
		 */
		Set<DataWithMediaType> build();
	}


	/**
	 * SseEventBuilder的默认实现.
	 */
	private static class SseEventBuilderImpl implements SseEventBuilder {

		private final Set<DataWithMediaType> dataToSend = new LinkedHashSet<DataWithMediaType>(4);

		private StringBuilder sb;

		@Override
		public SseEventBuilder comment(String comment) {
			append(":").append(comment != null ? comment : "").append("\n");
			return this;
		}

		@Override
		public SseEventBuilder name(String name) {
			append("event:").append(name != null ? name : "").append("\n");
			return this;
		}

		@Override
		public SseEventBuilder id(String id) {
			append("id:").append(id != null ? id : "").append("\n");
			return this;
		}

		@Override
		public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
			append("retry:").append(String.valueOf(reconnectTimeMillis)).append("\n");
			return this;
		}

		@Override
		public SseEventBuilder data(Object object) {
			return data(object, null);
		}

		@Override
		public SseEventBuilder data(Object object, MediaType mediaType) {
			append("data:");
			saveAppendedText();
			this.dataToSend.add(new DataWithMediaType(object, mediaType));
			append("\n");
			return this;
		}

		SseEventBuilderImpl append(String text) {
			if (this.sb == null) {
				this.sb = new StringBuilder();
			}
			this.sb.append(text);
			return this;
		}

		@Override
		public Set<DataWithMediaType> build() {
			if (!StringUtils.hasLength(this.sb) && this.dataToSend.isEmpty()) {
				return Collections.<DataWithMediaType>emptySet();
			}
			append("\n");
			saveAppendedText();
			return this.dataToSend;
		}

		private void saveAppendedText() {
			if (this.sb != null) {
				this.dataToSend.add(new DataWithMediaType(this.sb.toString(), TEXT_PLAIN));
				this.sb = null;
			}
		}
	}
}
