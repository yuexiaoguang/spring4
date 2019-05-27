package org.springframework.cache.jcache.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.config.CacheManagementConfigUtils;
import org.springframework.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor;
import org.springframework.cache.jcache.interceptor.JCacheInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * {@code @Configuration}类, 用于注册启用基于代理的注解驱动的JSR-107缓存管理所必需的Spring基础结构bean.
 *
 * <p>可以安全地与Spring的缓存支持一起使用.
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyJCacheConfiguration extends AbstractJCacheConfiguration {

	@Bean(name = CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryJCacheOperationSourceAdvisor cacheAdvisor() {
		BeanFactoryJCacheOperationSourceAdvisor advisor =
				new BeanFactoryJCacheOperationSourceAdvisor();
		advisor.setCacheOperationSource(cacheOperationSource());
		advisor.setAdvice(cacheInterceptor());
		advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
		return advisor;
	}

	@Bean(name = "jCacheInterceptor")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JCacheInterceptor cacheInterceptor() {
		JCacheInterceptor interceptor = new JCacheInterceptor();
		interceptor.setCacheOperationSource(cacheOperationSource());
		if (this.errorHandler != null) {
			interceptor.setErrorHandler(this.errorHandler);
		}
		return interceptor;
	}

}
