package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * {@link DefaultBeanDefinitionDocumentReader}用于处理自定义顶级(直接在 {@code <beans/>}下)标签的接口.
 *
 * <p>实现可以自由地将自定义标记中的元数据转换为多个 {@link BeanDefinition BeanDefinitions}.
 *
 * <p>解析器从关联的{@link NamespaceHandler}中找到{@link BeanDefinitionParser}, 用于自定义标签所在的命名空间.
 */
public interface BeanDefinitionParser {

	/**
	 * 解析指定的 {@link Element}, 并使用提供的{@link ParserContext}中嵌入的 
	 * {@link org.springframework.beans.factory.xml.ParserContext#getRegistry() BeanDefinitionRegistry}
	 * 注册生成的{@link BeanDefinition}.
	 * <p>如果它们将以嵌套方式使用 (例如作为 {@code <property/>} 标签中的内部标签), 则实现必须返回由解析产生的主{@link BeanDefinition}.
	 * 如果它们不以嵌套方式使用, 则实现可能会返回{@code null}.
	 * 
	 * @param element 要解析为一个或多个 {@link BeanDefinition BeanDefinitions}的元素
	 * @param parserContext 封装解析过程当前状态的对象;
	 * 提供对 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的访问权限
	 * 
	 * @return 主要的{@link BeanDefinition}
	 */
	BeanDefinition parse(Element element, ParserContext parserContext);

}
