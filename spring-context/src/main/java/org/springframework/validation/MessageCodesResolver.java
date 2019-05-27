package org.springframework.validation;

/**
 * 用于根据验证错误代码构建消息代码的策略接口.
 * 由DataBinder使用以构建ObjectErrors和FieldErrors的代码列表.
 *
 * <p>生成的消息代码对应于MessageSourceResolvable的代码 (由ObjectError和FieldError实现).
 */
public interface MessageCodesResolver {

	/**
	 * 为给定的错误代码和对象名构建消息代码.
	 * 用于构建ObjectError的代码列表.
	 * 
	 * @param errorCode 用于拒绝对象的错误代码
	 * @param objectName 对象的名称
	 * 
	 * @return 要使用的消息代码
	 */
	String[] resolveMessageCodes(String errorCode, String objectName);

	/**
	 * 为给定的错误代码和字段规范构建消息代码.
	 * 用于构建FieldError的代码列表.
	 * 
	 * @param errorCode 用于拒绝该值的错误代码
	 * @param objectName 对象的名称
	 * @param field 字段名
	 * @param fieldType 字段类型(如果不可确定, 可能是{@code null})
	 * 
	 * @return 要使用的消息代码
	 */
	String[] resolveMessageCodes(String errorCode, String objectName, String field, Class<?> fieldType);

}
