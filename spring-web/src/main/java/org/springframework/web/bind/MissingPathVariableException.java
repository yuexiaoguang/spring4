package org.springframework.web.bind;

import org.springframework.core.MethodParameter;

/**
 * {@link ServletRequestBindingException}子类,
 * 指示从URL中提取的URI变量中不存在{@code @RequestMapping}方法的方法参数中预期的路径变量.
 * 通常, 这意味着URI模板与方法参数上声明的路径变量名称不匹配.
 */
@SuppressWarnings("serial")
public class MissingPathVariableException extends ServletRequestBindingException {

	private final String variableName;

	private final MethodParameter parameter;


	/**
	 * @param variableName 缺少路径变量的名称
	 * @param parameter 方法参数
	 */
	public MissingPathVariableException(String variableName, MethodParameter parameter) {
		super("");
		this.variableName = variableName;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		return "Missing URI template variable '" + this.variableName +
				"' for method parameter of type " + this.parameter.getNestedParameterType().getSimpleName();
	}

	/**
	 * 返回路径变量的预期名称.
	 */
	public final String getVariableName() {
		return this.variableName;
	}

	/**
	 * 返回绑定到path变量的方法参数.
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
