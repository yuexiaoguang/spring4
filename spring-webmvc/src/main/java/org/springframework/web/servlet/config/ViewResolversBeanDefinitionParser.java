package org.springframework.web.servlet.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Ordered;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

/**
 * 解析{@code view-resolvers} MVC命名空间元素并注册{@link org.springframework.web.servlet.ViewResolver} bean定义.
 *
 * <p>所有已注册的解析器都包含在一个 (复合) ViewResolver中, 其order属性设置为0,
 * 因此可以在其之前或之后排序其他外部解析器.
 *
 * <p>启用内容协商时, order属性设置为最高优先级, 而ContentExgotiatingViewResolver则封装所有其他已注册的视图解析器实例.
 * 这样, 通过MVC命名空间注册的解析器形成自封装的解析器链.
 */
public class ViewResolversBeanDefinitionParser implements BeanDefinitionParser {

	public static final String VIEW_RESOLVER_BEAN_NAME = "mvcViewResolver";


	@SuppressWarnings("deprecation")
	public BeanDefinition parse(Element element, ParserContext context) {
		Object source = context.extractSource(element);
		context.pushContainingComponent(new CompositeComponentDefinition(element.getTagName(), source));

		ManagedList<Object> resolvers = new ManagedList<Object>(4);
		resolvers.setSource(context.extractSource(element));
		String[] names = new String[] {"jsp", "tiles", "bean-name", "freemarker", "velocity", "groovy", "script-template", "bean", "ref"};

		for (Element resolverElement : DomUtils.getChildElementsByTagName(element, names)) {
			String name = resolverElement.getLocalName();
			if ("bean".equals(name) || "ref".equals(name)) {
				resolvers.add(context.getDelegate().parsePropertySubElement(resolverElement, null));
				continue;
			}
			RootBeanDefinition resolverBeanDef;
			if ("jsp".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(InternalResourceViewResolver.class);
				resolverBeanDef.getPropertyValues().add("prefix", "/WEB-INF/");
				resolverBeanDef.getPropertyValues().add("suffix", ".jsp");
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("tiles".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(TilesViewResolver.class);
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("freemarker".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(FreeMarkerViewResolver.class);
				resolverBeanDef.getPropertyValues().add("suffix", ".ftl");
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("velocity".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(org.springframework.web.servlet.view.velocity.VelocityViewResolver.class);
				resolverBeanDef.getPropertyValues().add("suffix", ".vm");
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("groovy".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(GroovyMarkupViewResolver.class);
				resolverBeanDef.getPropertyValues().add("suffix", ".tpl");
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("script-template".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(ScriptTemplateViewResolver.class);
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			}
			else if ("bean-name".equals(name)) {
				resolverBeanDef = new RootBeanDefinition(BeanNameViewResolver.class);
			}
			else {
				// Should never happen
				throw new IllegalStateException("Unexpected element name: " + name);
			}
			resolverBeanDef.setSource(source);
			resolverBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			resolvers.add(resolverBeanDef);
		}

		String beanName = VIEW_RESOLVER_BEAN_NAME;
		RootBeanDefinition compositeResolverBeanDef = new RootBeanDefinition(ViewResolverComposite.class);
		compositeResolverBeanDef.setSource(source);
		compositeResolverBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		names = new String[] {"content-negotiation"};
		List<Element> contentNegotiationElements = DomUtils.getChildElementsByTagName(element, names);
		if (contentNegotiationElements.isEmpty()) {
			compositeResolverBeanDef.getPropertyValues().add("viewResolvers", resolvers);
		}
		else if (contentNegotiationElements.size() == 1) {
			BeanDefinition beanDef = createContentNegotiatingViewResolver(contentNegotiationElements.get(0), context);
			beanDef.getPropertyValues().add("viewResolvers", resolvers);
			ManagedList<Object> list = new ManagedList<Object>(1);
			list.add(beanDef);
			compositeResolverBeanDef.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
			compositeResolverBeanDef.getPropertyValues().add("viewResolvers", list);
		}
		else {
			throw new IllegalArgumentException("Only one <content-negotiation> element is allowed.");
		}

		if (element.hasAttribute("order")) {
			compositeResolverBeanDef.getPropertyValues().add("order", element.getAttribute("order"));
		}

		context.getReaderContext().getRegistry().registerBeanDefinition(beanName, compositeResolverBeanDef);
		context.registerComponent(new BeanComponentDefinition(compositeResolverBeanDef, beanName));
		context.popAndRegisterContainingComponent();
		return null;
	}

	private void addUrlBasedViewResolverProperties(Element element, RootBeanDefinition beanDefinition) {
		if (element.hasAttribute("prefix")) {
			beanDefinition.getPropertyValues().add("prefix", element.getAttribute("prefix"));
		}
		if (element.hasAttribute("suffix")) {
			beanDefinition.getPropertyValues().add("suffix", element.getAttribute("suffix"));
		}
		if (element.hasAttribute("cache-views")) {
			beanDefinition.getPropertyValues().add("cache", element.getAttribute("cache-views"));
		}
		if (element.hasAttribute("view-class")) {
			beanDefinition.getPropertyValues().add("viewClass", element.getAttribute("view-class"));
		}
		if (element.hasAttribute("view-names")) {
			beanDefinition.getPropertyValues().add("viewNames", element.getAttribute("view-names"));
		}
	}

	private BeanDefinition createContentNegotiatingViewResolver(Element resolverElement, ParserContext context) {
		RootBeanDefinition beanDef = new RootBeanDefinition(ContentNegotiatingViewResolver.class);
		beanDef.setSource(context.extractSource(resolverElement));
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		MutablePropertyValues values = beanDef.getPropertyValues();

		List<Element> elements = DomUtils.getChildElementsByTagName(resolverElement, "default-views");
		if (!elements.isEmpty()) {
			ManagedList<Object> list = new ManagedList<Object>();
			for (Element element : DomUtils.getChildElementsByTagName(elements.get(0), "bean", "ref")) {
				list.add(context.getDelegate().parsePropertySubElement(element, null));
			}
			values.add("defaultViews", list);
		}
		if (resolverElement.hasAttribute("use-not-acceptable")) {
			values.add("useNotAcceptableStatusCode", resolverElement.getAttribute("use-not-acceptable"));
		}
		Object manager = MvcNamespaceUtils.getContentNegotiationManager(context);
		if (manager != null) {
			values.add("contentNegotiationManager", manager);
		}
		return beanDef;
	}

}
