package org.springframework.context.annotation;

/**
 * 一个{@link Condition}, 与{@code @Configuration}一起使用时提供更细粒度的控制.
 * 允许某些{@link Condition}在匹配时根据配置阶段进行调整.
 * 例如, 检查已经注册的bean可能选择仅在
 * {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} {@link ConfigurationPhase}期间进行评估的条件.
 */
public interface ConfigurationCondition extends Condition {

	/**
	 * 返回应评估条件的{@link ConfigurationPhase}.
	 */
	ConfigurationPhase getConfigurationPhase();


	/**
	 * 可以评估条件的各种配置阶段.
	 */
	enum ConfigurationPhase {

		/**
		 * {@link Condition}应该被评估为正在解析的{@code @Configuration}类.
		 * <p>如果此时条件不匹配, 则不会添加{@code @Configuration}类.
		 */
		PARSE_CONFIGURATION,

		/**
		 * 添加常规 (non {@code @Configuration}) bean时, 应评估{@link Condition}.
		 * 该条件不会阻止添加{@code @Configuration}类.
		 * <p>在评估条件时, 将解析所有{@code @Configuration}.
		 */
		REGISTER_BEAN
	}

}
