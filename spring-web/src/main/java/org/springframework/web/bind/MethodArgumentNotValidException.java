package org.springframework.web.bind;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * 对使用 {@code @Valid}注解的参数验证失败时抛出的异常.
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends Exception {

	private final MethodParameter parameter;

	private final BindingResult bindingResult;


	/**
	 * @param parameter 验证失败的参数
	 * @param bindingResult 验证的结果
	 */
	public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
		this.parameter = parameter;
		this.bindingResult = bindingResult;
	}

	/**
	 * 返回验证失败的方法参数.
	 */
	public MethodParameter getParameter() {
		return this.parameter;
	}

	/**
	 * 返回失败验证的结果.
	 */
	public BindingResult getBindingResult() {
		return this.bindingResult;
	}


	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder("Validation failed for argument at index ")
			.append(this.parameter.getParameterIndex()).append(" in method: ")
			.append(this.parameter.getMethod().toGenericString())
			.append(", with ").append(this.bindingResult.getErrorCount()).append(" error(s): ");
		for (ObjectError error : this.bindingResult.getAllErrors()) {
			sb.append("[").append(error).append("] ");
		}
		return sb.toString();
	}

}
