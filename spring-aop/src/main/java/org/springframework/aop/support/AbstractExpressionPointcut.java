package org.springframework.aop.support;

import java.io.Serializable;

/**
 * 表达切点的抽象超类, 提供位置和表达属性.
 */
@SuppressWarnings("serial")
public abstract class AbstractExpressionPointcut implements ExpressionPointcut, Serializable {

	private String location;

	private String expression;


	/**
	 * 设置调试位置.
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * 返回有关切点表达式的位置信息. 这在调试中很有用.
	 * 
	 * @return location 信息作为人类可读的字符串, 或{@code null}
	 */
	public String getLocation() {
		return this.location;
	}

	public void setExpression(String expression) {
		this.expression = expression;
		try {
			onSetExpression(expression);
		}
		catch (IllegalArgumentException ex) {
			// 如果可能，请填写位置信息.
			if (this.location != null) {
				throw new IllegalArgumentException("Invalid expression at location [" + this.location + "]: " + ex);
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * 设置新的切点表达式时调用. 如果可能, 应在此时解析表达式.
	 * <p>这个实现是空的.
	 * 
	 * @param expression 要设置的表达式
	 * 
	 * @throws IllegalArgumentException 如果表达式无效
	 */
	protected void onSetExpression(String expression) throws IllegalArgumentException {
	}

	/**
	 * 返回此切点的表达式.
	 */
	@Override
	public String getExpression() {
		return this.expression;
	}

}
