package org.springframework.jmx.export.metadata;

/**
 * 指示将给定bean属性公开为JMX属性的元数据.
 * 仅在JavaBean getter或setter上使用时有效.
 */
public class ManagedAttribute extends AbstractJmxAttribute {

	public static final ManagedAttribute EMPTY = new ManagedAttribute();


	private Object defaultValue;

	private String persistPolicy;

	private int persistPeriod = -1;


	/**
	 * 设置此属性的默认值.
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * 返回此属性的默认值.
	 */
	public Object getDefaultValue() {
		return this.defaultValue;
	}

	public void setPersistPolicy(String persistPolicy) {
		this.persistPolicy = persistPolicy;
	}

	public String getPersistPolicy() {
		return this.persistPolicy;
	}

	public void setPersistPeriod(int persistPeriod) {
		this.persistPeriod = persistPeriod;
	}

	public int getPersistPeriod() {
		return this.persistPeriod;
	}

}
