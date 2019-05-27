package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;

/**
 * {@link DefaultBeanDefinitionDocumentReader}用于处理Spring XML配置文件中的自定义命名空间的基础接口.
 *
 * <p>实现有望为自定义顶级标签返回{@link BeanDefinitionParser}接口的实现,
 * 并为自定义嵌套标签返回{@link BeanDefinitionDecorator}接口的实现.
 *
 * <p>解析器在{@code <beans>}标签下直接遇到自定义标签时会调用{@link #parse};
 * 而在{@code <bean>}下直接遇到自定义标签时, 会调用{@link #decorate}.
 *
 * <p>编写自己的自定义元素扩展的开发人员通常不会直接实现此接口, 而是使用提供的{@link NamespaceHandlerSupport}类.
 */
public interface NamespaceHandler {

	/**
	 * 在构造之后, 但在解析自定义元素之前, 由{@link DefaultBeanDefinitionDocumentReader}调用.
	 */
	void init();

	/**
	 * 解析指定的{@link Element}, 
	 * 并将所有生成的{@link BeanDefinition BeanDefinitions}注册到所提供的
	 * {@link ParserContext}中的 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
	 * <p>实现应返回由解析阶段产生的主要{@code BeanDefinition}, 如果它们希望嵌套在内部的(例如){@code <property>}标签.
	 * <p>如果它们不会在嵌套场景中使用, 则实现可能会返回{@code null}.
	 * 
	 * @param element 要解析为一个或多个{@code BeanDefinitions}的元素
	 * @param parserContext 封装解析过程当前状态的对象
	 * 
	 * @return 主要{@code BeanDefinition} (可以是{@code null}, 如上所述)
	 */
	BeanDefinition parse(Element element, ParserContext parserContext);

	/**
	 * 解析指定的{@link Node}, 并装饰提供的{@link BeanDefinitionHolder}, 返回装饰后的定义.
	 * <p>{@link Node}可以是{@link org.w3c.dom.Attr}或{@link Element}, 具体取决于是否正在解析自定义属性或元素.
	 * <p>实现可以选择返回一个全新的定义, 它将替换生成的{@link org.springframework.beans.factory.BeanFactory}中的原始定义.
	 * <p>提供的{@link ParserContext}可用于注册支持主要定义所需的任何其他的bean.
	 * 
	 * @param source 要解析的源元素或属性
	 * @param definition 当前的bean定义
	 * @param parserContext 封装解析过程当前状态的对象
	 * 
	 * @return 装饰后的定义 (要在BeanFactory中注册); 或者只是原始bean定义, 如果不需要装饰.
	 * 严格说来, {@code null}值无效, 但会像返回原始bean定义的情况一样被宽大处理.
	 */
	BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);

}
