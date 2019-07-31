package org.springframework.http.converter;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

/**
 * 策略接口, 指定可以从HTTP请求和响应转换的转换器.
 */
public interface HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定的类.
	 * 
	 * @param clazz 要测试可读性的类
	 * @param mediaType 要读取的媒体类型 (如果未指定, 可以是{@code null});
	 * 通常是{@code Content-Type} header的值.
	 * 
	 * @return {@code true}如果可读; 否则{@code false}
	 */
	boolean canRead(Class<?> clazz, MediaType mediaType);

	/**
	 * 指示此转换器是否可以写入给定的类.
	 * 
	 * @param clazz 用于测试可写性的类
	 * @param mediaType 要写入的媒体类型 (如果未指定, 可以是{@code null});
	 * 通常是{@code Accept} header的值.
	 * 
	 * @return {@code true} 如果可写; 否则{@code false}
	 */
	boolean canWrite(Class<?> clazz, MediaType mediaType);

	/**
	 * 返回此转换器支持的{@link MediaType}对象列表.
	 * 
	 * @return 支持的媒体类型列表
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * 从给定的输入消息中读取给定类型的对象, 并返回它.
	 * 
	 * @param clazz 要返回的对象的类型. 此类型必须已经经过此接口的{@link #canRead canRead}方法的检查, 并且该方法必须返回{@code true}.
	 * @param inputMessage 要读取的HTTP输入消息
	 * 
	 * @return 转换后的对象
	 * @throws IOException
	 * @throws HttpMessageNotReadableException 如果转换错误
	 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 将给定对象写入给定的输出消息.
	 * 
	 * @param t 要写入输出消息的对象.
	 * 此对象的类型必须已经经过此接口的{@link #canWrite canWrite}方法的检查, 并且该方法必须返回{@code true}.
	 * @param contentType 写入时要使用的内容类型.
	 * 可以是{@code null}以指示必须使用转换器的默认内容类型.
	 * 如果不是{@code null}, 则此媒体类型必须已经经过此接口的{@link #canWrite canWrite}方法的检查, 并且该方法必须返回{@code true}.
	 * @param outputMessage 要写入的消息
	 * 
	 * @throws IOException
	 * @throws HttpMessageNotWritableException 如果转换错误
	 */
	void write(T t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
