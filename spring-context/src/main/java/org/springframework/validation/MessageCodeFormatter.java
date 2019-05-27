package org.springframework.validation;

/**
 * 用于格式化消息代码的策略接口.
 */
public interface MessageCodeFormatter {

	/**
	 * 构建并返回由给定字段组成的消息代码, 通常由{@link DefaultMessageCodesResolver#CODE_SEPARATOR}分隔.
	 * 
	 * @param errorCode e.g.: "typeMismatch"
	 * @param objectName e.g.: "user"
	 * @param field e.g. "age"
	 * 
	 * @return 连接的消息代码, e.g.: "typeMismatch.user.age"
	 */
	String format(String errorCode, String objectName, String field);

}
