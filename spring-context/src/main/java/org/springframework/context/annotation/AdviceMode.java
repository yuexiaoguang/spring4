package org.springframework.context.annotation;

/**
 * 用于确定是否应该应用基于JDK代理或基于AspectJ织入的增强.
 */
public enum AdviceMode {

	/**
	 * JDK基于代理的增强.
	 */
	PROXY,

	/**
	 * AspectJ基于织入的增强.
	 */
	ASPECTJ

}
