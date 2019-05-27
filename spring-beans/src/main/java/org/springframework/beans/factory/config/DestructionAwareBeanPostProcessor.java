package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * {@link BeanPostProcessor}的子接口, 添加了一个销毁前的回调.
 *
 * <p>一般的用法是调用特定bean类型的自定义销毁回调, 匹配相应的初始化回调.
 */
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 在销毁之前将此BeanPostProcessor应用于给定的bean实例, e.g. 调用自定义销毁回调.
	 * <p>像 DisposableBean的 {@code destroy} 和自定义destroy方法一样, 此回调仅适用于容器完全管理生命周期的bean.
	 * 通常就是单例和范围bean.
	 * 
	 * @param bean 要销毁的bean实例
	 * @param beanName bean的名称
	 * 
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

	/**
	 * 确定给定的b​​ean实例是否需要通过此后处理器进行销毁.
	 * <p><b>NOTE:</b> 即使作为后期添加, 此方法也已在{@code DestructionAwareBeanPostProcessor}本身上引入, 而不是在SmartDABPP子接口上引入.
	 * 这允许现有的{@code DestructionAwareBeanPostProcessor}实现轻松提供{@code requiresDestruction}逻辑, 同时保持与Spring的兼容性<4.3,
	 * 在Spring 5中将{@code requiresDestruction}声明为Java 8默认方法也是一个更容易的onramp.
	 * <p>如果{@code DestructionAwareBeanPostProcessor}的实现没有提供此方法的具体实现,
	 * Spring的调用机制假设一个方法返回{@code true} (4.3之前的有效默认值, 以及Spring 5中Java 8方法中的默认值).
	 * 
	 * @param bean 要检查的bean实例
	 * 
	 * @return {@code true}如果最终应该为这个bean实例调用{@link #postProcessBeforeDestruction}, 或{@code false}不需要
	 */
	boolean requiresDestruction(Object bean);

}
