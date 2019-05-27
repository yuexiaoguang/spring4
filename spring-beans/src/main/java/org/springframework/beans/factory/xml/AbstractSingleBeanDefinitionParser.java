package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * 那些需要解析和定义<i>单个</i> {@code BeanDefinition}的{@link BeanDefinitionParser}实现的基类.
 *
 * <p>如果要从复杂的XML元素创建单个bean定义, 请扩展此解析器类.
 * 当您想要从相对简单的自定义XML元素创建单个bean定义时, 您可能希望考虑扩展{@link AbstractSimpleBeanDefinitionParser}.
 *
 * <p>生成的{@code BeanDefinition}将自动注册到 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 * 您的工作只是 {@link #doParse 解析} 自定义XML {@link Element}合并为一个{@code BeanDefinition}.
 */
public abstract class AbstractSingleBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * 为 {@link #getBeanClass bean Class}创建一个{@link BeanDefinitionBuilder}实例, 并将其传递给{@link #doParse}策略方法.
	 * 
	 * @param element 要解析为单个BeanDefinition的元素
	 * @param parserContext 封装解析过程当前状态的对象
	 * 
	 * @return 解析所提供的{@link Element}产生的BeanDefinition
	 * @throws IllegalStateException 如果从 {@link #getBeanClass(org.w3c.dom.Element)}返回的bean {@link Class}是 {@code null}
	 */
	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		String parentName = getParentName(element);
		if (parentName != null) {
			builder.getRawBeanDefinition().setParentName(parentName);
		}
		Class<?> beanClass = getBeanClass(element);
		if (beanClass != null) {
			builder.getRawBeanDefinition().setBeanClass(beanClass);
		}
		else {
			String beanClassName = getBeanClassName(element);
			if (beanClassName != null) {
				builder.getRawBeanDefinition().setBeanClassName(beanClassName);
			}
		}
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		if (parserContext.isNested()) {
			// 内部bean定义必须与外部bean的作用域相同.
			builder.setScope(parserContext.getContainingBeanDefinition().getScope());
		}
		if (parserContext.isDefaultLazyInit()) {
			// Default-lazy-init也适用于自定义bean定义.
			builder.setLazyInit(true);
		}
		doParse(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

	/**
	 * 在当前bean被定义为子级bean的情况下, 确定当前解析的bean的父级的名称.
	 * <p>默认实现返回{@code null}, 表示根bean定义.
	 * 
	 * @param element 正在解析的{@code Element}
	 * 
	 * @return 当前解析的bean的父级bean的名称, 或{@code null}
	 */
	protected String getParentName(Element element) {
		return null;
	}

	/**
	 * 确定与提供的{@link Element}对应的bean类.
	 * <p>请注意, 对于应用程序类, 通常最好覆盖{@link #getBeanClassName}, 以避免直接依赖于bean实现类.
	 * BeanDefinitionParser及其NamespaceHandler可以在IDE插件中使用, 即使插件的类路径上没有应用程序类.
	 * 
	 * @param element 正在解析的{@code Element}
	 * 
	 * @return 通过解析提供的{@code Element}, 定义的bean的{@link Class}, 或{@code null}
	 */
	protected Class<?> getBeanClass(Element element) {
		return null;
	}

	/**
	 * 确定与提供的{@link Element}对应的bean类名称.
	 * 
	 * @param element 正在解析的{@code Element}
	 * 
	 * @return 通过解析提供的{@code Element}, 定义的bean的类名, 或{@code null}
	 */
	protected String getBeanClassName(Element element) {
		return null;
	}

	/**
	 * 解析提供的{@link Element}, 并根据需要填充提供的{@link BeanDefinitionBuilder}.
	 * <p>默认实现委托给没有ParserContext参数的{@code doParse}版本.
	 * 
	 * @param element 正在解析的XML元素
	 * @param parserContext 封装解析过程当前状态的对象
	 * @param builder 用于定义{@code BeanDefinition}
	 */
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		doParse(element, builder);
	}

	/**
	 * 解析提供的{@link Element}, 并根据需要填充提供的{@link BeanDefinitionBuilder}.
	 * <p>默认实现无操作.
	 * 
	 * @param element 正在解析的XML元素
	 * @param builder 用于定义 {@code BeanDefinition}
	 */
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
	}

}
