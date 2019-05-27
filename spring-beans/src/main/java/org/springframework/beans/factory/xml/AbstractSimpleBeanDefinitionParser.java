package org.springframework.beans.factory.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 方便的基类, 在要解析的元素上的属性名称, 与要配置的{@link Class}上的属性名称之间存在一对一映射时使用.
 *
 * <p>如果要从相对简单的自定义XML元素创建单个bean定义, 扩展此解析器类.
 * 生成的{@code BeanDefinition}将自动注册到相关的 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 *
 * <p>能够直接清楚地使用这个特定的解析器类的例子. 考虑以下类定义:
 *
 * <pre class="code">public class SimpleCache implements Cache {
 *
 *     public void setName(String name) {...}
 *     public void setTimeout(int timeout) {...}
 *     public void setEvictionPolicy(EvictionPolicy policy) {...}
 *
 *     // remaining class definition elided for clarity...
 * }</pre>
 *
 * <p>然后假设已经定义了以下XML标签, 以允许轻松配置上述类的实例;
 *
 * <pre class="code">&lt;caching:cache name="..." timeout="..." eviction-policy="..."/&gt;</pre>
 *
 * <p>所有需要编写解析器, 以将上述XML标签解析为实际的{@code SimpleCache} bean定义的Java开发人员, 都需要以下内容:
 *
 * <pre class="code">public class SimpleCacheBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {
 *
 *     protected Class getBeanClass(Element element) {
 *         return SimpleCache.class;
 *     }
 * }</pre>
 *
 * <p>请注意, {@code AbstractSimpleBeanDefinitionParser}仅限于使用属性值填充创建的bean定义.
 * 如果要从提供的XML元素解析构造函数参数和嵌套的元素, 那么你必须实现
 * {@link #postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.w3c.dom.Element)}方法, 并自己做这样的解析,
 * 或者(更有可能)直接对 {@link AbstractSingleBeanDefinitionParser}或{@link AbstractBeanDefinitionParser}类进行子类化.
 *
 * <p>Spring Framework参考文档中描述了使用Spring XML解析基础结构实际注册 {@code SimpleCacheBeanDefinitionParser}的过程
 * (在其中一个附录中).
 *
 * <p>有关此解析器的示例 (可以这么说), 查看
 * {@link org.springframework.beans.factory.xml.UtilNamespaceHandler.PropertiesBeanDefinitionParser}的源码;
 * 细心 (甚至不那么细心) 的读者会立即注意到实现中没有代码.
 * {@code PropertiesBeanDefinitionParser}从一个看起来像这样的XML元素填充
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean}:
 *
 * <pre class="code">&lt;util:properties location="jdbc.properties"/&gt;</pre>
 *
 * <p>细心的读者会注意到 {@code <util:properties/>}元素的唯一属性与{@code PropertiesFactoryBean}上的
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean#setLocation(org.springframework.core.io.Resource)}方法名称相匹配
 * (这样说明的一般用法适用于任何数量的属性).
 * {@code PropertiesBeanDefinitionParser}实际需要做的就是提供
 * {@link #getBeanClass(org.w3c.dom.Element)}方法的实现来返回 {@code PropertiesFactoryBean}类型.
 */
public abstract class AbstractSimpleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * 解析提供的{@link Element}, 并根据需要填充提供的{@link BeanDefinitionBuilder}.
	 * <p>此实现将提供的元素上存在的任何属性映射到{@link org.springframework.beans.PropertyValue}实例,
	 * 并
	 * {@link BeanDefinitionBuilder#addPropertyValue(String, Object)添加它们}
	 * 到
	 * {@link org.springframework.beans.factory.config.BeanDefinition builder}.
	 * <p>{@link #extractPropertyName(String)}方法用于将属性的名称与JavaBean属性的名称进行协调.
	 * 
	 * @param element 正在解析的XML元素
	 * @param builder 用于定义{@code BeanDefinition}
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		NamedNodeMap attributes = element.getAttributes();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			if (isEligibleAttribute(attribute, parserContext)) {
				String propertyName = extractPropertyName(attribute.getLocalName());
				Assert.state(StringUtils.hasText(propertyName),
						"Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
				builder.addPropertyValue(propertyName, attribute.getValue());
			}
		}
		postProcess(builder, element);
	}

	/**
	 * 确定给定属性是否有资格转换为相应的bean属性值.
	 * <p>除“id”属性和命名空间声明属性外, 默认实现将任何属性视为合格.
	 * 
	 * @param attribute 要检查的XML属性
	 * @param parserContext {@code ParserContext}
	 */
	protected boolean isEligibleAttribute(Attr attribute, ParserContext parserContext) {
		String fullName = attribute.getName();
		return (!fullName.equals("xmlns") && !fullName.startsWith("xmlns:") &&
				isEligibleAttribute(parserContext.getDelegate().getLocalName(attribute)));
	}

	/**
	 * 确定给定属性是否能够转换为相应的bean属性值.
	 * <p>除“id”属性外, 默认实现将任何属性视为可以.
	 * 
	 * @param attributeName 直接从正在解析的XML元素获取的属性名称 (never {@code null})
	 */
	protected boolean isEligibleAttribute(String attributeName) {
		return !ID_ATTRIBUTE.equals(attributeName);
	}

	/**
	 * 从提供的属性名称中提取JavaBean属性名称.
	 * <p>默认实现使用 {@link Conventions#attributeNameToPropertyName(String)}方法来执行提取.
	 * <p>返回的名称必须遵守标准的JavaBean属性名称约定.
	 * 例如, 对于具有setter方法 '{@code setBingoHallFavourite(String)}'的类, 返回的名称最好是 '{@code bingoHallFavourite}' (正确的大小写).
	 * 
	 * @param attributeName 直接从正在解析的XML元素获取的属性名称 (never {@code null})
	 * 
	 * @return 提取的JavaBean属性名称 (must never be {@code null})
	 */
	protected String extractPropertyName(String attributeName) {
		return Conventions.attributeNameToPropertyName(attributeName);
	}

	/**
	 * 派生类可以实现的Hook方法, 用于在解析完成后检查/更改bean定义.
	 * <p>默认实现无操作.
	 * 
	 * @param beanDefinition 正在构建的解析后的(可能是完全定义的)bean定义
	 * @param element 作为bean定义元数据源的XML元素
	 */
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
	}

}
