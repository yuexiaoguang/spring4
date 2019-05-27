package org.springframework.jmx.export.metadata;

import org.springframework.jmx.support.MetricType;
import org.springframework.util.Assert;

/**
 * 指示将给定bean属性公开为JMX属性的元数据, 使用额外的描述符属性来指示属性是指标.
 * 仅在JavaBean getter上使用时有效.
 */
public class ManagedMetric extends AbstractJmxAttribute {

	private String category = "";

	private String displayName = "";

	private MetricType metricType = MetricType.GAUGE;

	private int persistPeriod = -1;

	private String persistPolicy = "";

	private String unit = "";


	/**
	 * 此指标的类别 (ex. 吞吐量, 性能, 利用率).
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * 此指标的类别 (ex. 吞吐量, 性能, 利用率).
	 */
	public String getCategory() {
		return this.category;
	}

	/**
	 * 此指标的显示名称.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 此指标的显示名称.
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * 此指标值随时间变化的描述.
	 */
	public void setMetricType(MetricType metricType) {
		Assert.notNull(metricType, "MetricType must not be null");
		this.metricType = metricType;
	}

	/**
	 * 此指标值随时间变化的描述.
	 */
	public MetricType getMetricType() {
		return this.metricType;
	}

	/**
	 * 此指标的持续时间.
	 */
	public void setPersistPeriod(int persistPeriod) {
		this.persistPeriod = persistPeriod;
	}

	/**
	 * 此指标的持续时间.
	 */
	public int getPersistPeriod() {
		return this.persistPeriod;
	}

	/**
	 * 此指标的持久策略.
	 */
	public void setPersistPolicy(String persistPolicy) {
		this.persistPolicy = persistPolicy;
	}

	/**
	 * 此指标的持久策略.
	 */
	public String getPersistPolicy() {
		return this.persistPolicy;
	}

	/**
	 * 测量值的单位.
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * 测量值的单位.
	 */
	public String getUnit() {
		return this.unit;
	}

}
