package org.springframework.web.bind;

/**
 * {@link ServletRequestBindingException}子类, 表示缺少参数.
 */
@SuppressWarnings("serial")
public class MissingServletRequestParameterException extends ServletRequestBindingException {

	private final String parameterName;

	private final String parameterType;


	/**
	 * @param parameterName 缺少的参数名称
	 * @param parameterType 缺少的参数的预期类型
	 */
	public MissingServletRequestParameterException(String parameterName, String parameterType) {
		super("");
		this.parameterName = parameterName;
		this.parameterType = parameterType;
	}


	@Override
	public String getMessage() {
		return "Required " + this.parameterType + " parameter '" + this.parameterName + "' is not present";
	}

	/**
	 * 返回有问题的参数的名称.
	 */
	public final String getParameterName() {
		return this.parameterName;
	}

	/**
	 * 返回有问题的参数的预期类型.
	 */
	public final String getParameterType() {
		return this.parameterType;
	}

}
