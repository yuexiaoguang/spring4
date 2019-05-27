package org.springframework.messaging.handler.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * 当方法参数由于{@code @Valid}样式验证而未通过验证时抛出异常, 或者可能是因为它是必需的.
 */
@SuppressWarnings({"serial", "deprecation"})
public class MethodArgumentNotValidException extends AbstractMethodArgumentResolutionException {

	private BindingResult bindingResult;


	/**
	 * 使用无效的{@code MethodParameter}创建新实例.
	 */
	public MethodArgumentNotValidException(Message<?> message, MethodParameter parameter) {
		super(message, parameter);
	}

	/**
	 * 使用无效的{@code MethodParameter}和{@link org.springframework.validation.BindingResult}创建一个新实例.
	 */
	public MethodArgumentNotValidException(Message<?> message, MethodParameter parameter, BindingResult bindingResult) {
		super(message, parameter, getValidationErrorMessage(bindingResult));
		this.bindingResult = bindingResult;
	}


	/**
	 * 如果是与验证相关的失败, 则返回BindingResult; 如果没有, 则返回{@code null}.
	 */
	public final BindingResult getBindingResult() {
		return this.bindingResult;
	}


	private static String getValidationErrorMessage(BindingResult bindingResult) {
		StringBuilder sb = new StringBuilder();
		sb.append(bindingResult.getErrorCount()).append(" error(s): ");
		for (ObjectError error : bindingResult.getAllErrors()) {
			sb.append("[").append(error).append("] ");
		}
		return sb.toString();
	}

}
