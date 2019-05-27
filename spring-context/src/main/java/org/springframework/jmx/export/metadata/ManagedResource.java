package org.springframework.jmx.export.metadata;

/**
 * 元数据, 指示要向JMX服务器注册带注解的类的实例.
 * 仅在{@code Class}上使用时有效.
 */
public class ManagedResource extends AbstractJmxAttribute {

	private String objectName;

	private boolean log = false;

	private String logFile;

	private String persistPolicy;

	private int persistPeriod = -1;

	private String persistName;

	private String persistLocation;


	/**
	 * 设置此受管资源的JMX ObjectName.
	 */
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	/**
	 * 返回此受管资源的JMX ObjectName.
	 */
	public String getObjectName() {
		return this.objectName;
	}

	public void setLog(boolean log) {
		this.log = log;
	}

	public boolean isLog() {
		return this.log;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public String getLogFile() {
		return this.logFile;
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

	public void setPersistName(String persistName) {
		this.persistName = persistName;
	}

	public String getPersistName() {
		return this.persistName;
	}

	public void setPersistLocation(String persistLocation) {
		this.persistLocation = persistLocation;
	}

	public String getPersistLocation() {
		return this.persistLocation;
	}

}
