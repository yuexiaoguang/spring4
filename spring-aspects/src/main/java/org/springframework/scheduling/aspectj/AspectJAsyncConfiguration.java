package org.springframework.scheduling.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.AbstractAsyncConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * {@code @Configuration}类，用于注册启用基于AspectJ的异步方法执行所必需的Spring基础结构bean.
 */
@Configuration
public class AspectJAsyncConfiguration extends AbstractAsyncConfiguration {

	@Bean(name = TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationAsyncExecutionAspect asyncAdvisor() {
		AnnotationAsyncExecutionAspect asyncAspect = AnnotationAsyncExecutionAspect.aspectOf();
		if (this.executor != null) {
			asyncAspect.setExecutor(this.executor);
		}
		if (this.exceptionHandler != null) {
			asyncAspect.setExceptionHandler(this.exceptionHandler);
		}
		return asyncAspect;
	}

}
