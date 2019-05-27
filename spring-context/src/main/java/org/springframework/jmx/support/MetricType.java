package org.springframework.jmx.support;

/**
 * 表示{@code ManagedMetric}的测量值将如何随时间变化.
 */
public enum MetricType {

	/**
	 * 测量值可能随时间上升或下降.
	 */
	GAUGE,

	/**
	 * 测量值将始终增加.
	 */
	COUNTER

}
