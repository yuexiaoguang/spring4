package org.springframework.web.servlet.tags;

/**
 * Bean used to pass name-value pair parameters from a {@link ParamTag} to a
 * {@link ParamAware} tag.
 *
 * <p>Attributes are the raw values passed to the spring:param tag and have not
 * been encoded or escaped.
 */
public class Param {

	private String name;

	private String value;


	/**
	 * Set the raw name of the parameter.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the raw parameter name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Set the raw value of the parameter.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Return the raw parameter value.
	 */
	public String getValue() {
		return this.value;
	}


	@Override
	public String toString() {
		return "JSP Tag Param: name '" + this.name + "', value '" + this.value + "'";
	}

}
