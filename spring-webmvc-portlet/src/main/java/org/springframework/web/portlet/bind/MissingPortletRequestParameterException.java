package org.springframework.web.portlet.bind;

/**
 * {@link PortletRequestBindingException}子类, 表示缺少参数.
 */
@SuppressWarnings("serial")
public class MissingPortletRequestParameterException extends PortletRequestBindingException {

	private final String parameterName;

	private final String parameterType;


	/**
	 * @param parameterName 缺少的参数的名称
	 * @param parameterType 缺少的参数的预期类型
	 */
	public MissingPortletRequestParameterException(String parameterName, String parameterType) {
		super("");
		this.parameterName = parameterName;
		this.parameterType = parameterType;
	}


	@Override
	public String getMessage() {
		return "Required " + this.parameterType + " parameter '" + parameterName + "' is not present";
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
