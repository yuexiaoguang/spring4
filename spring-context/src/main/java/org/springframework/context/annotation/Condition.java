package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 单个{@code condition}必须按顺序 {@linkplain #matches matched}, 用于注册组件.
 *
 * <p>在bean定义注册之前立即检查条件, 并且可以根据此时可以确定的任何条件自由否决注册.
 *
 * <p>条件必须遵循与{@link BeanFactoryPostProcessor}相同的限制, 并注意永远不要与bean实例交互.
 * 要更好地控制与{@code @Configuration} bean交互的条件, 请考虑{@link ConfigurationCondition}接口.
 */
public interface Condition {

	/**
	 * 确定条件是否匹配.
	 * 
	 * @param context 条件上下文
	 * @param metadata 正在检查的{@link org.springframework.core.type.AnnotationMetadata class}
	 * 或{@link org.springframework.core.type.MethodMetadata method}的元数据.
	 * 
	 * @return {@code true} 如果条件匹配且组件可以注册; 或{@code false}否决注册.
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

}
