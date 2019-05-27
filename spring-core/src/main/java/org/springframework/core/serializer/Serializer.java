package org.springframework.core.serializer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 用于将对象流式传输到OutputStream的策略接口.
 */
public interface Serializer<T> {

	/**
	 * 将类型为T的对象写入给定的OutputStream.
	 * <p>Note: 实现不应该关闭给定的OutputStream (或该OutputStream的任何装饰器), 而是将其留给调用者.
	 * 
	 * @param object 要序列化的对象
	 * @param outputStream 输出流
	 * 
	 * @throws IOException 如果写入流错误
	 */
	void serialize(T object, OutputStream outputStream) throws IOException;

}
