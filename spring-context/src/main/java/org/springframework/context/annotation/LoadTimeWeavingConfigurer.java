package org.springframework.context.annotation;

import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 要使用{@link EnableLoadTimeWeaving @EnableLoadTimeWeaving}注解的
 * {@link org.springframework.context.annotation.Configuration @Configuration}类实现的接口,
 * 希望自定义要使用的{@link LoadTimeWeaver}实例.
 *
 * <p>有关使用示例的信息, 请参阅{@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync},
 * 以及在未使用此接口时如何选择默认{@code LoadTimeWeaver} 的信息.
 */
public interface LoadTimeWeavingConfigurer {

	/**
	 * 创建, 配置并返回要使用的 {@code LoadTimeWeaver}实例.
	 * 请注意, 不必使用{@code @Bean}注解此方法, 因为返回的对象将通过{@link LoadTimeWeavingConfiguration#loadTimeWeaver()}自动注册为bean.
	 */
	LoadTimeWeaver getLoadTimeWeaver();

}
