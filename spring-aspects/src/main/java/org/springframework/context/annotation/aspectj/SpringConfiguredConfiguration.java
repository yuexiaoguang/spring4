package org.springframework.context.annotation.aspectj;

import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * 注册{@code AnnotationBeanConfigurerAspect}的{@code @Configuration}类, 
 * 能够为使用@ {@ link org.springframework.beans.factory.annotation.Configurable Configurable}注解的非Spring托管对象执行依赖注入服务.
 *
 * <p>使用{@link EnableSpringConfigured @EnableSpringConfigured}注解时，将自动导入此配置类.
 * See {@code @EnableSpringConfigured}'s javadoc for complete usage details.
 */
@Configuration
public class SpringConfiguredConfiguration {

	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME =
			"org.springframework.context.config.internalBeanConfigurerAspect";

	@Bean(name = BEAN_CONFIGURER_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationBeanConfigurerAspect beanConfigurerAspect() {
		return AnnotationBeanConfigurerAspect.aspectOf();
	}

}
