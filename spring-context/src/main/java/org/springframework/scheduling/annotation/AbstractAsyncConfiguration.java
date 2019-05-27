package org.springframework.scheduling.annotation;

import java.util.Collection;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;

/**
 * 抽象基础{@code Configuration}类, 提供用于启用Spring异步方法执行功能的通用结构.
 */
@Configuration
public abstract class AbstractAsyncConfiguration implements ImportAware {

	protected AnnotationAttributes enableAsync;

	protected Executor executor;

	protected AsyncUncaughtExceptionHandler exceptionHandler;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableAsync = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableAsync.class.getName(), false));
		if (this.enableAsync == null) {
			throw new IllegalArgumentException(
					"@EnableAsync is not present on importing class " + importMetadata.getClassName());
		}
	}

	/**
	 * 通过自动装配收集任何{@link AsyncConfigurer} bean.
	 */
	@Autowired(required = false)
	void setConfigurers(Collection<AsyncConfigurer> configurers) {
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}
		if (configurers.size() > 1) {
			throw new IllegalStateException("Only one AsyncConfigurer may exist");
		}
		AsyncConfigurer configurer = configurers.iterator().next();
		this.executor = configurer.getAsyncExecutor();
		this.exceptionHandler = configurer.getAsyncUncaughtExceptionHandler();
	}

}
