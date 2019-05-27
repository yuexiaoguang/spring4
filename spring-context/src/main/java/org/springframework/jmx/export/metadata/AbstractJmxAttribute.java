package org.springframework.jmx.export.metadata;

/**
 * 所有JMX元数据类的基类.
 */
public class AbstractJmxAttribute {

	private String description = "";

	private int currencyTimeLimit = -1;


	/**
	 * 设置此属性的说明.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 返回此属性的说明.
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * 为此属性设置货币时间限制.
	 */
	public void setCurrencyTimeLimit(int currencyTimeLimit) {
		this.currencyTimeLimit = currencyTimeLimit;
	}

	/**
	 * 返回此属性的货币时间限制.
	 */
	public int getCurrencyTimeLimit() {
		return this.currencyTimeLimit;
	}

}
