package org.springframework.jmx.export.metadata;

/**
 * 有关JMX操作参数的元数据.
 * 与{@link ManagedOperation}属性一起使用.
 */
public class ManagedOperationParameter {

	private int index = 0;

	private String name = "";

	private String description = "";


	/**
	 * 设置操作签名中此参数的索引.
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * 返回操作签名中此参数的索引.
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * 设置操作签名中此参数的名称.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回操作签名中此参数的名称.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 设置此参数的描述.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 返回此参数的描述.
	 */
	public String getDescription() {
		return this.description;
	}

}
