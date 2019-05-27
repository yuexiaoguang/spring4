package org.springframework.scheduling.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * 注册{@link ScheduledAnnotationBeanPostProcessor} bean的{@code @Configuration}类, 能够处理Spring的 @{@link Scheduled}注解.
 *
 * <p>使用{@link EnableScheduling @EnableScheduling}注解时, 将自动导入此配置类.
 * 有关完整的使用详细信息, 请参阅{@code @EnableScheduling}的javadoc.
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class SchedulingConfiguration {

	@Bean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public ScheduledAnnotationBeanPostProcessor scheduledAnnotationProcessor() {
		return new ScheduledAnnotationBeanPostProcessor();
	}

}
