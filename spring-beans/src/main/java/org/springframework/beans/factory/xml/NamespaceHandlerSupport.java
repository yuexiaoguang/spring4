package org.springframework.beans.factory.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;

/**
 * 用于实现自定义{@link NamespaceHandler NamespaceHandlers}的支持类.
 * 单个{@link Node Nodes}的解析和装饰, 分别通过{@link BeanDefinitionParser}和{@link BeanDefinitionDecorator}策略接口完成.
 *
 * <p>提供{@link #registerBeanDefinitionParser}和{@link #registerBeanDefinitionDecorator}方法,
 * 用于注册{@link BeanDefinitionParser}或{@link BeanDefinitionDecorator}, 以处理特定元素.
 */
public abstract class NamespaceHandlerSupport implements NamespaceHandler {

	/**
	 * 存储{@link BeanDefinitionParser}实现, 以他们处理的 {@link Element Elements}的本地名称作为Key.
	 */
	private final Map<String, BeanDefinitionParser> parsers =
			new HashMap<String, BeanDefinitionParser>();

	/**
	 * 存储{@link BeanDefinitionDecorator}实现, 以他们处理的{@link Element Elements}的本地名称作为Key.
	 */
	private final Map<String, BeanDefinitionDecorator> decorators =
			new HashMap<String, BeanDefinitionDecorator>();

	/**
	 * 存储{@link BeanDefinitionDecorator}实现, 以他们处理的{@link Attr Attrs}的本地名称作为Key.
	 */
	private final Map<String, BeanDefinitionDecorator> attributeDecorators =
			new HashMap<String, BeanDefinitionDecorator>();


	/**
	 * 通过委托给为{@link Element}注册的{@link BeanDefinitionParser}, 解析提供的{@link Element}.
	 */
	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		return findParserForElement(element, parserContext).parse(element, parserContext);
	}

	/**
	 * 使用提供的{@link Element}的本地名称, 从注册的实现中找到{@link BeanDefinitionParser}.
	 */
	private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
		String localName = parserContext.getDelegate().getLocalName(element);
		BeanDefinitionParser parser = this.parsers.get(localName);
		if (parser == null) {
			parserContext.getReaderContext().fatal(
					"Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
		}
		return parser;
	}

	/**
	 * 通过委托给注册以处理{@link Node}的{@link BeanDefinitionDecorator}, 来装饰提供的{@link Node}.
	 */
	@Override
	public BeanDefinitionHolder decorate(
			Node node, BeanDefinitionHolder definition, ParserContext parserContext) {

		return findDecoratorForNode(node, parserContext).decorate(node, definition, parserContext);
	}

	/**
	 * 使用提供的{@link Node}的本地名称, 从注册的实现中找到{@link BeanDefinitionParser}.
	 * 支持{@link Element Elements}和{@link Attr Attrs}.
	 */
	private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
		BeanDefinitionDecorator decorator = null;
		String localName = parserContext.getDelegate().getLocalName(node);
		if (node instanceof Element) {
			decorator = this.decorators.get(localName);
		}
		else if (node instanceof Attr) {
			decorator = this.attributeDecorators.get(localName);
		}
		else {
			parserContext.getReaderContext().fatal(
					"Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
		}
		if (decorator == null) {
			parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " +
					(node instanceof Element ? "element" : "attribute") + " [" + localName + "]", node);
		}
		return decorator;
	}


	/**
	 * 子类可以调用它, 来注册提供的{@link BeanDefinitionParser}, 以处理指定的元素.
	 * 元素名称是本地(非命名空间限定)名称.
	 */
	protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
		this.parsers.put(elementName, parser);
	}

	/**
	 * 子类可以调用它来注册提供的{@link BeanDefinitionDecorator}, 以处理指定的元素.
	 * 元素名称是本地(非命名空间限定)名称.
	 */
	protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator dec) {
		this.decorators.put(elementName, dec);
	}

	/**
	 * 子类可以调用它来注册提供的{@link BeanDefinitionDecorator}, 以处理指定的属性.
	 * 属性名称是本地(非命名空间限定)名称.
	 */
	protected final void registerBeanDefinitionDecoratorForAttribute(String attrName, BeanDefinitionDecorator dec) {
		this.attributeDecorators.put(attrName, dec);
	}

}
