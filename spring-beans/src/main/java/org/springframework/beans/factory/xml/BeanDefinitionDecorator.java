package org.springframework.beans.factory.xml;

import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinitionHolder;

/**
 * {@link DefaultBeanDefinitionDocumentReader}用于处理自定义嵌套 (直接在 {@code <bean>}下)标签的接口.
 *
 * <p>装饰也可能基于{@code <bean>}标签的自定义属性而发生.
 * 实现可以根据需要自由地将自定义标签中的元数据转换为多个 {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions},
 * 并转换封闭的{@code <bean>}标签的 {@link org.springframework.beans.factory.config.BeanDefinition},
 * 甚至可能返回一个完全不同的 {@link org.springframework.beans.factory.config.BeanDefinition}来替换原来的.
 *
 * <p>{@link BeanDefinitionDecorator BeanDefinitionDecorators}应该知道它们可能是链的一部分.
 * 特别是, {@link BeanDefinitionDecorator}应该知道之前的{@link BeanDefinitionDecorator},
 * 可能用允许添加自定义{@link org.aopalliance.intercept.MethodInterceptor 拦截器}的
 * {@link org.springframework.aop.framework.ProxyFactoryBean}定义,
 * 替换原来的 {@link org.springframework.beans.factory.config.BeanDefinition}.
 *
 * <p>希望向封闭的bean添加拦截器的{@link BeanDefinitionDecorator BeanDefinitionDecorators}
 * 应该扩展{@link org.springframework.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator},
 * 它处理链, 确保只创建一个代理, 并且包含来自链的所有拦截器.
 *
 * <p>解析器从{@link NamespaceHandler}中找到{@link BeanDefinitionDecorator}, 用于自定义标签所在的命名空间.
 */
public interface BeanDefinitionDecorator {

	/**
	 * 解析指定的 {@link Node} (元素或属性), 并修饰提供的 {@link org.springframework.beans.factory.config.BeanDefinition},
	 * 返回装饰后的定义.
	 * <p>实现可以选择返回一个全新的定义, 它将替换生成的 {@link org.springframework.beans.factory.BeanFactory}中的原始定义.
	 * <p>提供的{@link ParserContext}可用于注册支持主要定义所需的其他bean.
	 */
	BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext);

}
