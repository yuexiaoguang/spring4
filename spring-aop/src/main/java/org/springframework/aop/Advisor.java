package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * 保持AOP <b>advice</b>的基础接口 (在连接点采取的动作), 以及过滤器确定增强的适用性 (例如切点).
 * <i>此接口不供Spring用户使用, 但允许支持不同类型的建议的共性.</i>
 *
 * <p>Spring AOP基于通过方法<b>interception</b>提供的<b>around advice</b>, 符合AOP联盟拦截API.
 * Advisor接口允许支持不同类型的增强, 例如 <b>before</b>和<b>after</b>增强, 不需要使用拦截来实现.
 */
public interface Advisor {

	/**
	 * 返回切面的增强部分. 增强可能是拦截器，前置增强，抛出增强等.
	 * 
	 * @return 切点匹配时应该应用的增强
	 */
	Advice getAdvice();

	/**
	 * 返回此增强是否与特定实例相关联（例如，创建mixin）或与从同一Spring bean工厂获取的增强类的所有实例共享.
	 * <p><b>请注意，框架当前不使用此方法.</b> 通常 Advisor实现类总是返回 {@code true}.
	 * 使用singleton/prototype bean定义或适当的编程代理创建来确保Advisor具有正确的生命周期模型.
	 * 
	 * @return 此增强是否与特定目标实例相关联
	 */
	boolean isPerInstance();

}
