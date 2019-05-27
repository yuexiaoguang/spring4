package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

/**
 * 抽象{@link BeanDefinitionParser}实现,
 * 提供了许多便捷方法和一个{@link AbstractBeanDefinitionParser#parseInternal模板方法},
 * 子类必须重写以提供实际的解析逻辑.
 *
 * <p>如果要将一些任意复杂的XML解析为一个或多个{@link BeanDefinition BeanDefinitions},
 * 请使用此{@link BeanDefinitionParser}实现.
 * 如果您只想将一些XML解析为单个 {@code BeanDefinition}, 考虑这个类的更简单的便利扩展,
 * {@link AbstractSingleBeanDefinitionParser}和{@link AbstractSimpleBeanDefinitionParser}.
 */
public abstract class AbstractBeanDefinitionParser implements BeanDefinitionParser {

	/** "id"属性 */
	public static final String ID_ATTRIBUTE = "id";

	/** "name"属性 */
	public static final String NAME_ATTRIBUTE = "name";


	@Override
	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		AbstractBeanDefinition definition = parseInternal(element, parserContext);
		if (definition != null && !parserContext.isNested()) {
			try {
				String id = resolveId(element, definition, parserContext);
				if (!StringUtils.hasText(id)) {
					parserContext.getReaderContext().error(
							"Id is required for element '" + parserContext.getDelegate().getLocalName(element)
									+ "' when used as a top-level tag", element);
				}
				String[] aliases = null;
				if (shouldParseNameAsAliases()) {
					String name = element.getAttribute(NAME_ATTRIBUTE);
					if (StringUtils.hasLength(name)) {
						aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
					}
				}
				BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id, aliases);
				registerBeanDefinition(holder, parserContext.getRegistry());
				if (shouldFireEvents()) {
					BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
					postProcessComponentDefinition(componentDefinition);
					parserContext.registerComponent(componentDefinition);
				}
			}
			catch (BeanDefinitionStoreException ex) {
				parserContext.getReaderContext().error(ex.getMessage(), element);
				return null;
			}
		}
		return definition;
	}

	/**
	 * 解析所提供的{@link BeanDefinition}的ID.
	 * <p>使用{@link #shouldGenerateId generation}时, 会自动生成一个名称.
	 * 否则, ID会从 "id"属性中提取, 可能会使用 {@link #shouldGenerateIdAsFallback() fallback}生成ID.
	 * 
	 * @param element 构造bean定义的元素
	 * @param definition 要注册的bean定义
	 * @param parserContext 封装解析过程的当前状态的对象;
	 * 提供对 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的访问权限
	 * 
	 * @return 解析后的id
	 * @throws BeanDefinitionStoreException 如果没有为给定的bean定义生成唯一的名称
	 */
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		if (shouldGenerateId()) {
			return parserContext.getReaderContext().generateBeanName(definition);
		}
		else {
			String id = element.getAttribute(ID_ATTRIBUTE);
			if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback()) {
				id = parserContext.getReaderContext().generateBeanName(definition);
			}
			return id;
		}
	}

	/**
	 * 使用提供的{@link BeanDefinitionRegistry注册表}注册提供的{@link BeanDefinitionHolder bean}.
	 * <p>子类可以重写此方法来控制提供的{@link BeanDefinitionHolder bean}是否实际上已注册, 或者是否注册更多的bean.
	 * <p>仅当{@code isNested}参数为{@code false}时,
	 * 默认实现才会将提供的{@link BeanDefinitionHolder bean}与提供的{@link BeanDefinitionRegistry注册表}一起注册,
	 * 因为通常不希望内部bean被注册为顶级bean.
	 * 
	 * @param definition 要注册的bean定义
	 * @param registry 要注册bean的注册表
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definition, registry);
	}


	/**
	 * 实际将提供的{@link Element}解析为一个或多个{@link BeanDefinition BeanDefinitions}的中央模板方法.
	 * 
	 * @param element 要解析为一个或多个{@link BeanDefinition BeanDefinitions}的元素
	 * @param parserContext 封装解析过程的当前状态的对象;
	 * 提供对 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的访问权限
	 * 
	 * @return 解析所提供的{@link Element}产生的主{@link BeanDefinition}
	 */
	protected abstract AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext);

	/**
	 * 是否应生成ID, 而不是从传入的{@link Element}中读取?
	 * <p>默认情况下禁用; 子类可以覆盖它以启用ID生成.
	 * 请注意, 此标志始终生成ID; 在这种情况下, 解析器甚至不会检查“id”属性.
	 * 
	 * @return 解析器是否应始终生成id
	 */
	protected boolean shouldGenerateId() {
		return false;
	}

	/**
	 * 如果传入的{@link Element}未明确指定“id”属性, 是否生成ID?
	 * <p>默认情况下禁用; 子类可以覆盖它以启用ID生成作为回退:
	 * 在这种情况下, 解析器将首先检查“id”属性; 如果没有指定值, 则仅返回生成的ID.
	 * 
	 * @return 如果没有指定id, 解析器是否应生成id
	 */
	protected boolean shouldGenerateIdAsFallback() {
		return false;
	}

	/**
	 * 确定元素的“name”属性是否应该被解析为bean定义别名, i.e. 替代bean定义名称.
	 * <p>默认实现返回 {@code true}.
	 * 
	 * @return 解析器是否应将“name”属性解析为别名
	 */
	protected boolean shouldParseNameAsAliases() {
		return true;
	}

	/**
	 * 在解析bean定义之后, 确定此解析器是否应该触发 {@link org.springframework.beans.factory.parsing.BeanComponentDefinition}事件.
	 * <p>默认情况下, 此实现返回{@code true}; 也就是说, 在完全解析bean定义时将触发事件.
	 * 覆盖此项返回{@code false}以抑制事件.
	 * 
	 * @return {@code true} 在解析bean定义后触发组件注册事件; {@code false}抑制事件
	 */
	protected boolean shouldFireEvents() {
		return true;
	}

	/**
	 * 在{@link BeanComponentDefinition}的主要解析之后,
	 * 但在使用{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}注册{@link BeanComponentDefinition}之前,
	 * 调用的钩子方法.
	 * <p>派生类可以覆盖此方法, 以提供在完成所有解析后要执行的任何自定义逻辑.
	 * <p>默认实现无操作.
	 * 
	 * @param componentDefinition 要处理的{@link BeanComponentDefinition}
	 */
	protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
	}

}
