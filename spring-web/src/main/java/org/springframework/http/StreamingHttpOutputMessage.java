package org.springframework.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 表示允许设置流主体的HTTP输出消息.
 * 请注意, 此类消息通常不支持{@link #getBody()}访问.
 */
public interface StreamingHttpOutputMessage extends HttpOutputMessage {

	/**
	 * 设置此消息的流主体回调.
	 * 
	 * @param body 流主体回调
	 */
	void setBody(Body body);


	/**
	 * 定义可以直接写入{@link OutputStream}的主体的约定.
	 * 对HTTP客户端库很有用, 它通过回调机制提供对{@link OutputStream}的间接访问.
	 */
	interface Body {

		/**
		 * 将此正文写入给定的{@link OutputStream}.
		 * 
		 * @param outputStream 要写入的输出流
		 * 
		 * @throws IOException
		 */
		void writeTo(OutputStream outputStream) throws IOException;
	}

}
