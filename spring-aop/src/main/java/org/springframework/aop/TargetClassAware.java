package org.springframework.aop;

/**
 * 用于在代理后面公开目标类的最小接口.
 *
 * <p>由AOP代理对象和代理工厂(通过 {@link org.springframework.aop.framework.Advised})
 * 以及{@link TargetSource TargetSources}实现.
 */
public interface TargetClassAware {

	/**
	 * 返回实现对象后面的目标类 (通常是代理配置或实际代理).
	 * 
	 * @return 目标类的Class, 或{@code null}
	 */
	Class<?> getTargetClass();

}
