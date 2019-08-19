package org.springframework.web.servlet.config;

import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}解析以下MVC命名空间元素:
 * <ul>
 * <li>{@code <view-controller>}
 * <li>{@code <redirect-view-controller>}
 * <li>{@code <status-controller>}
 * </ul>
 *
 * <p>所有元素都导致
 * {@link org.springframework.web.servlet.mvc.ParameterizableViewController ParameterizableViewController}的注册,
 * 所有控制器都使用单个
 * {@link org.springframework.web.servlet.handler.SimpleUrlHandlerMapping SimpleUrlHandlerMapping}进行映射.
 */
class ViewControllerBeanDefinitionParser implements BeanDefinitionParser {

	private static final String HANDLER_MAPPING_BEAN_NAME =
			"org.springframework.web.servlet.config.viewControllerHandlerMapping";


	@Override
	@SuppressWarnings("unchecked")
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		// 为视图控制器注册SimpleUrlHandlerMapping
		BeanDefinition hm = registerHandlerMapping(parserContext, source);

		// 确保BeanNameUrlHandlerMapping (SPR-8289) 和默认的HandlerAdapter没有"关闭"
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);

		// 创建视图控制器bean定义
		RootBeanDefinition controller = new RootBeanDefinition(ParameterizableViewController.class);
		controller.setSource(source);

		HttpStatus statusCode = null;
		if (element.hasAttribute("status-code")) {
			int statusValue = Integer.parseInt(element.getAttribute("status-code"));
			statusCode = HttpStatus.valueOf(statusValue);
		}

		String name = element.getLocalName();
		if (name.equals("view-controller")) {
			if (element.hasAttribute("view-name")) {
				controller.getPropertyValues().add("viewName", element.getAttribute("view-name"));
			}
			if (statusCode != null) {
				controller.getPropertyValues().add("statusCode", statusCode);
			}
		}
		else if (name.equals("redirect-view-controller")) {
			controller.getPropertyValues().add("view", getRedirectView(element, statusCode, source));
		}
		else if (name.equals("status-controller")) {
			controller.getPropertyValues().add("statusCode", statusCode);
			controller.getPropertyValues().add("statusOnly", true);
		}
		else {
			// Should never happen...
			throw new IllegalStateException("Unexpected tag name: " + name);
		}

		Map<String, BeanDefinition> urlMap;
		if (hm.getPropertyValues().contains("urlMap")) {
			urlMap = (Map<String, BeanDefinition>) hm.getPropertyValues().getPropertyValue("urlMap").getValue();
		}
		else {
			urlMap = new ManagedMap<String, BeanDefinition>();
			hm.getPropertyValues().add("urlMap", urlMap);
		}
		urlMap.put(element.getAttribute("path"), controller);

		return null;
	}

	private BeanDefinition registerHandlerMapping(ParserContext context, Object source) {
		if (context.getRegistry().containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
			return context.getRegistry().getBeanDefinition(HANDLER_MAPPING_BEAN_NAME);
		}
		RootBeanDefinition beanDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		context.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, beanDef);
		context.registerComponent(new BeanComponentDefinition(beanDef, HANDLER_MAPPING_BEAN_NAME));

		beanDef.setSource(source);
		beanDef.getPropertyValues().add("order", "1");
		beanDef.getPropertyValues().add("pathMatcher", MvcNamespaceUtils.registerPathMatcher(null, context, source));
		beanDef.getPropertyValues().add("urlPathHelper", MvcNamespaceUtils.registerUrlPathHelper(null, context, source));
		RuntimeBeanReference corsConfigurationsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
		beanDef.getPropertyValues().add("corsConfigurations", corsConfigurationsRef);

		return beanDef;
	}

	private RootBeanDefinition getRedirectView(Element element, HttpStatus status, Object source) {
		RootBeanDefinition redirectView = new RootBeanDefinition(RedirectView.class);
		redirectView.setSource(source);
		redirectView.getConstructorArgumentValues().addIndexedArgumentValue(0, element.getAttribute("redirect-url"));

		if (status != null) {
			redirectView.getPropertyValues().add("statusCode", status);
		}

		if (element.hasAttribute("context-relative")) {
			redirectView.getPropertyValues().add("contextRelative", element.getAttribute("context-relative"));
		}
		else {
			redirectView.getPropertyValues().add("contextRelative", true);
		}

		if (element.hasAttribute("keep-query-params")) {
			redirectView.getPropertyValues().add("propagateQueryParams", element.getAttribute("keep-query-params"));
		}

		return redirectView;
	}

}
