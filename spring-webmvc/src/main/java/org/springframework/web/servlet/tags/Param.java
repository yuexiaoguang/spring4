package org.springframework.web.servlet.tags;

/**
 * 用于将name-value对参数从{@link ParamTag}传递到{@link ParamAware}标记.
 *
 * <p>属性是传递给 spring:param 标记的原始值, 尚未编码或转义.
 */
public class Param {

	private String name;

	private String value;


	/**
	 * 设置参数的原始名称.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回原始参数名称.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 设置参数的原始值.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * 返回原始参数值.
	 */
	public String getValue() {
		return this.value;
	}


	@Override
	public String toString() {
		return "JSP Tag Param: name '" + this.name + "', value '" + this.value + "'";
	}
}
