package org.springframework.context.annotation;

/**
 * 配置应用程序上下文的注解的通用接口, 定义{@link #register}和{@link #scan}方法.
 */
public interface AnnotationConfigRegistry {

	/**
	 * 注册一个或多个要处理的带注解的类.
	 * <p>对{@code register}的调用是幂等的; 多次添加相同的带注解的类没有额外的效果.
	 * 
	 * @param annotatedClasses 一个或多个带注解的类, e.g. {@link Configuration @Configuration}类
	 */
	void register(Class<?>... annotatedClasses);

	/**
	 * 在指定的基础包中执行扫描.
	 * 
	 * @param basePackages 要检查带注解的类的包
	 */
	void scan(String... basePackages);

}
