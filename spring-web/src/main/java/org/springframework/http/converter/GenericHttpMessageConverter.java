package org.springframework.http.converter;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

/**
 * {@link HttpMessageConverter}的细化, 可以将HTTP请求转换为指定泛型类型的目标对象,
 * 并将指定泛型类型的源对象转换为HTTP响应.
 */
public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定类型.
	 * 此方法应执行与{@link HttpMessageConverter#canRead(Class, MediaType)}相同的检查, 以及与泛型类型相关的其他检查.
	 * 
	 * @param type 要测试可读性的类型 (可能是通用的)
	 * @param contextClass 目标类型的上下文类, 例如目标类型出现在方法签名中的类 (can be {@code null})
	 * @param mediaType 要读取的媒体类型, 或{@code null}.
	 * 通常是{@code Content-Type} header的值.
	 * 
	 * @return {@code true}如果可读; 否则{@code false}
	 */
	boolean canRead(Type type, Class<?> contextClass, MediaType mediaType);

	/**
	 * 从给定的输入消息中读取给定类型的对象, 并返回它.
	 * 
	 * @param type 要返回的(可能是通用的)对象类型.
	 * 此类型必须已经过此接口的{@link #canRead canRead}方法验证, 并且该方法返回{@code true}.
	 * @param contextClass 目标类型的上下文类, 例如目标类型出现在方法签名中的类 (can be {@code null})
	 * @param inputMessage 要读取的HTTP输入消息
	 * 
	 * @return 转换后的对象
	 * @throws IOException
	 * @throws HttpMessageNotReadableException 如果转换错误
	 */
	T read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 指示此转换器是否可以写入给定的类.
	 * <p>此方法应执行与{@link HttpMessageConverter#canWrite(Class, MediaType)}相同的检查, 以及与泛型类型相关的其他检查.
	 * 
	 * @param type 用于测试可写性的 (可能是通用的)类型 (可以是{@code null})
	 * @param clazz 要测试可写性的源对象类
	 * @param mediaType 要写入的媒体类型 (can be {@code null}); 通常是{@code Accept} header的值.
	 * 
	 * @return {@code true}如果可写; 否则{@code false}
	 */
	boolean canWrite(Type type, Class<?> clazz, MediaType mediaType);

	/**
	 * 将给定对象写入给定的输出消息.
	 * 
	 * @param t 要写入输出消息的对象.
	 * 此对象的类型必须已经过此接口的{@link #canWrite canWrite}方法验证, 并且该方法返回{@code true}.
	 * @param type 要写入的(可能是通用的)对象类型.
	 * 此类型必须已经过此接口的{@link #canWrite canWrite}方法验证, 并且该方法返回{@code true}. 可以是{@code null}.
	 * @param contentType 写入时要使用的内容类型.
	 * 可以是{@code null}以指示必须使用转换器的默认内容类型.
	 * 如果不是{@code null}, 则此媒体类型必须已经过此接口的{@link #canWrite canWrite}验证, 并且该方法返回{@code true}.
	 * @param outputMessage 要写入的消息
	 * 
	 * @throws IOException
	 * @throws HttpMessageNotWritableException 如果转换错误
	 */
	void write(T t, Type type, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
