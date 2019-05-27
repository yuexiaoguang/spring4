package org.springframework.web.portlet.bind;

/**
 * {@link PortletRequestBindingException} subclass that indicates a missing parameter.
 */
@SuppressWarnings("serial")
public class MissingPortletRequestParameterException extends PortletRequestBindingException {

	private final String parameterName;

	private final String parameterType;


	/**
	 * Constructor for MissingPortletRequestParameterException.
	 * @param parameterName the name of the missing parameter
	 * @param parameterType the expected type of the missing parameter
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
	 * Return the name of the offending parameter.
	 */
	public final String getParameterName() {
		return this.parameterName;
	}

	/**
	 * Return the expected type of the offending parameter.
	 */
	public final String getParameterType() {
		return this.parameterType;
	}

}
