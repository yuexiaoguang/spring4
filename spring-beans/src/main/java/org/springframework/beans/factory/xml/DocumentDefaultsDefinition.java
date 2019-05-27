package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.parsing.DefaultsDefinition;

/**
 * 简单JavaBean, 它包含标准Spring XML bean定义文档中{@code <beans>}级别指定的缺省值:
 * {@code default-lazy-init}, {@code default-autowire}, etc.
 */
public class DocumentDefaultsDefinition implements DefaultsDefinition {

	private String lazyInit;

	private String merge;

	private String autowire;

	private String dependencyCheck;

	private String autowireCandidates;

	private String initMethod;

	private String destroyMethod;

	private Object source;


	/**
	 * 设置当前解析的文档的默认lazy-init标志.
	 */
	public void setLazyInit(String lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回当前解析的文档的默认lazy-init标志.
	 */
	public String getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 设置当前解析的文档的默认合并设置.
	 */
	public void setMerge(String merge) {
		this.merge = merge;
	}

	/**
	 * 返回当前解析的文档的默认合并设置.
	 */
	public String getMerge() {
		return this.merge;
	}

	/**
	 * 设置当前解析的文档的默认自动装配设置.
	 */
	public void setAutowire(String autowire) {
		this.autowire = autowire;
	}

	/**
	 * 返回当前解析的文档的默认自动装配设置.
	 */
	public String getAutowire() {
		return this.autowire;
	}

	/**
	 * 设置当前解析的文档的默认依赖项检查设置.
	 */
	public void setDependencyCheck(String dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * 返回当前解析的文档的默认依赖项检查设置.
	 */
	public String getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * 设置当前解析的文档的默认autowire-candidate模式.
	 * 还接受以逗号分隔的模式列表.
	 */
	public void setAutowireCandidates(String autowireCandidates) {
		this.autowireCandidates = autowireCandidates;
	}

	/**
	 * 返回当前解析的文档的默认autowire-candidate模式.
	 * 也可以返回以逗号分隔的模式列表.
	 */
	public String getAutowireCandidates() {
		return this.autowireCandidates;
	}

	/**
	 * 设置当前解析的文档的默认init-method设置.
	 */
	public void setInitMethod(String initMethod) {
		this.initMethod = initMethod;
	}

	/**
	 * 返回当前解析的文档的默认init-method设置.
	 */
	public String getInitMethod() {
		return this.initMethod;
	}

	/**
	 * 设置当前解析的文档的默认destroy-method设置.
	 */
	public void setDestroyMethod(String destroyMethod) {
		this.destroyMethod = destroyMethod;
	}

	/**
	 * 返回当前解析的文档的默认destroy-method设置.
	 */
	public String getDestroyMethod() {
		return this.destroyMethod;
	}

	/**
	 * 设置此元数据元素的配置源{@code Object}.
	 * <p>对象的精确类型取决于所使用的配置机制.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

}
