package org.springframework.context;

/**
 * 适用于{@link MessageSource}中消息解析的对象的接口.
 *
 * <p>Spring自己的验证错误类实现了这个接口.
 */
public interface MessageSourceResolvable {

	/**
	 * 按照应该尝试的顺序, 返回用于解析此消息的代码. 因此, 最后一个代码将是默认代码.
	 * 
	 * @return 与此消息关联的字符串数组
	 */
	String[] getCodes();

	/**
	 * 返回用于解析此消息的参数数组.
	 * 
	 * @return 要用作参数的对象数组, 以替换消息文本中的占位符
	 */
	Object[] getArguments();

	/**
	 * 返回用于解析此消息的默认消息.
	 * 
	 * @return 默认消息, 或{@code null}
	 */
	String getDefaultMessage();

}
