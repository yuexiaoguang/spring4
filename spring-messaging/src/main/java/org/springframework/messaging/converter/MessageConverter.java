package org.springframework.messaging.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * 用于将{@link Message}的有效负载从序列化形式转换为类型化对象的转换器, 反之亦然.
 * {@link MessageHeaders#CONTENT_TYPE}消息 header, 用于指定消息内容的媒体类型.
 */
public interface MessageConverter {

	/**
	 * 将{@link Message}的有效负载从序列化形式转换为指定目标类的类型化Object.
	 * {@link MessageHeaders#CONTENT_TYPE} header应指明要转换的MIME类型.
	 * <p>如果转换器不支持指定的媒体类型或无法执行转换, 则应返回{@code null}.
	 * 
	 * @param message 输入消息
	 * @param targetClass 转换的目标类
	 * 
	 * @return 转换的结果, 或{@code null} 如果转换器无法执行转换
	 */
	Object fromMessage(Message<?> message, Class<?> targetClass);

	/**
	 * 创建一个{@link Message}, 其有效负载是给定的有效负载Object转换为序列化形式的结果.
	 * 可选的{@link MessageHeaders}参数可能包含{@link MessageHeaders#CONTENT_TYPE} header,
	 * 用于指定转换的目标媒体类型, 并且可能包含要添加到消息的其他header.
	 * <p>如果转换器不支持指定的媒体类型或无法执行转换, 则应返回{@code null}.
	 * 
	 * @param payload 要转换的对象
	 * @param headers 消息的可选header (may be {@code null})
	 * 
	 * @return 新消息, 或{@code null} 如果转换器不支持对象类型或目标媒体类型
	 */
	Message<?> toMessage(Object payload, MessageHeaders headers);

}
