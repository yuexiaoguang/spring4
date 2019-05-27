package org.springframework.core;

/**
 * 可以通过错误编码的异常等实现的接口.
 * 错误代码是String, 而不是数字, 因此可以为用户提供可读的值, 例如"object.failureDescription".
 *
 * <p>例如, MessageSource可以解析错误代码.
 *
 * @deprecated as of Spring Framework 4.3.6
 */
@Deprecated
public interface ErrorCoded {

	/**
	 * 返回与此失败关联的错误码.
	 * GUI可以用任意方式呈现, 允许本地化等.
	 * 
	 * @return 与此失败关联的字符串错误码, 或{@code null}如果没有错误编码
	 */
	String getErrorCode();

}
