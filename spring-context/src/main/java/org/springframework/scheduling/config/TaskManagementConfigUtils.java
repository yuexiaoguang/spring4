package org.springframework.scheduling.config;

/**
 * 跨子包进行内部共享的配置常量.
 */
public class TaskManagementConfigUtils {

	/**
	 * 内部管理的Scheduled注解处理器的bean名称.
	 */
	public static final String SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalScheduledAnnotationProcessor";

	/**
	 * 内部管理的Async注解处理器的bean名称.
	 */
	public static final String ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalAsyncAnnotationProcessor";

	/**
	 * 内部管理的AspectJ异步执行切面的bean名称.
	 */
	public static final String ASYNC_EXECUTION_ASPECT_BEAN_NAME =
			"org.springframework.scheduling.config.internalAsyncExecutionAspect";

}
