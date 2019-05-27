package org.springframework.beans.factory.config;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;

/**
 * {@link BeanPostProcessor}的子接口, 它添加了一个实例化之前的回调,
 * 实例化后, 但在显式属性设置或自动装配发生之前的回调.
 *
 * <p>通常用于抑制特定目标bean的默认实例化,
 * 例如, 使用特殊的TargetSources创建代理 (池化目标, 延迟初始化目标, etc), 或实现其他的注入策略, 如字段注入.
 *
 * <p><b>NOTE:</b> 此接口是一个专用接口, 主要供框架内部使用.
 * 建议尽可能的实现普通的{@link BeanPostProcessor}接口,
 * 或者从{@link InstantiationAwareBeanPostProcessorAdapter}派生, 以便屏蔽对此接口的扩展.
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 在实例化目标bean之前应用此BeanPostProcessor.
	 * 返回的bean对象可能是代替目标bean的代理, 有效地抑制了目标bean的默认实例化.
	 * <p>如果此方法返回非null对象, 则bean创建过程将被短路.
	 * 应用的唯一进一步处理是来自配置的{@link BeanPostProcessor BeanPostProcessors}的{@link #postProcessAfterInitialization}回调.
	 * <p>此回调仅适用于具有bean类的bean定义. 特别是, 它不会应用于具有 "factory-method"的bean.
	 * <p>后处理器可以实现扩展的{@link SmartInstantiationAwareBeanPostProcessor}接口, 以便预测它们将在这里返回的bean对象的类型.
	 * 
	 * @param beanClass 要实例化的bean的类
	 * @param beanName bean的名称
	 * 
	 * @return 要公开的bean对象而不是目标bean的默认实例, 或{@code null}继续进行默认实例化
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException;

	/**
	 * 在实例化bean之后执行操作, 通过构造函数或工厂方法, 但在Spring属性填充 (显式属性或自动装配)发生之前.
	 * <p>这是在Spring的自动装配开始之前, 在给定的bean实例上执行自定义字段注入的理想回调.
	 * 
	 * @param bean 创建的bean实例, 其属性尚未设置
	 * @param beanName bean的名称
	 * 
	 * @return {@code true} 如果应该在bean上设置属性; {@code false} 如果应该跳过属性填充. 正常的实现应该返回 {@code true}.
	 * 返回{@code false}还将阻止在此Bean实例上调用任何后续的InstantiationAwareBeanPostProcessor实例.
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException;

	/**
	 * 在工厂将它们应用于给定bean之前对给定属性值进行后处理.
	 * 允许检查是否已满足所有依赖项, 例如，基于bean属性setter上的“Required”注解.
	 * <p>还允许替换要应用的属性值, 通常是通过基于原始PropertyValues创建的新的MutablePropertyValues实例, 添加或删除特定值.
	 * 
	 * @param pvs 工厂即将应用的属性值 (never {@code null})
	 * @param pds 目标bean的相关属性描述符(忽略依赖类型 - 工厂专门处理的 - 已经过滤出的)
	 * @param bean 已创建但尚未设置属性的bean实例
	 * @param beanName bean的名称
	 * 
	 * @return 要应用于给定bean的实际属性值 (可以是传入的PropertyValues实例), 或{@code null}跳过属性填充
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException;

}
