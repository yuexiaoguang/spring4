package org.springframework.messaging.converter;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

/**
 * 解析消息的内容类型.
 */
public interface ContentTypeResolver {

	/**
	 * 从给定的MessageHeader确定消息的{@link MimeType}.
	 * 
	 * @param headers 用于解析的header
	 * 
	 * @return 解析后的{@code MimeType}, 或{@code null}
	 * @throws InvalidMimeTypeException 如果内容类型是无法解析的String
	 * @throws IllegalArgumentException 如果有内容类型, 但其类型未知
	 */
	MimeType resolve(MessageHeaders headers) throws InvalidMimeTypeException;

}
