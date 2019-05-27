package org.springframework.cache.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.util.StringUtils;

/**
 * {@code NamespaceHandler}允许使用XML或使用注解配置声明性缓存管理.
 *
 * <p>此命名空间处理程序是Spring缓存管理工具中的核心功能.
 */
public class CacheNamespaceHandler extends NamespaceHandlerSupport {

	static final String CACHE_MANAGER_ATTRIBUTE = "cache-manager";

	static final String DEFAULT_CACHE_MANAGER_BEAN_NAME = "cacheManager";


	static String extractCacheManager(Element element) {
		return (element.hasAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE) ?
				element.getAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE) :
				CacheNamespaceHandler.DEFAULT_CACHE_MANAGER_BEAN_NAME);
	}

	static BeanDefinition parseKeyGenerator(Element element, BeanDefinition def) {
		String name = element.getAttribute("key-generator");
		if (StringUtils.hasText(name)) {
			def.getPropertyValues().add("keyGenerator", new RuntimeBeanReference(name.trim()));
		}
		return def;
	}


	@Override
	public void init() {
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenCacheBeanDefinitionParser());
		registerBeanDefinitionParser("advice", new CacheAdviceParser());
	}

}
