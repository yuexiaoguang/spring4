package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;

/**
 * 接口由类型实现, 这些类型根据给定的选择标准(通常是一个或多个注释属性), 确定导入哪个 @{@link Configuration}类.
 *
 * <p>{@link ImportSelector}可以实现以下任何{@link org.springframework.beans.factory.Aware Aware}接口,
 * 并在{@link #selectImports}之前调用它们各自的方法:
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}</li>
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}</li>
 * </ul>
 *
 * <p>ImportSelectors 的处理方式通常与常规{@code @Import}注解相同,
 * 但是, 在处理完所有{@code @Configuration}类之前, 也可以推迟导入的选择
 * (see {@link DeferredImportSelector} for details).
 */
public interface ImportSelector {

	/**
	 * 根据导入@{@link Configuration}类的{@link AnnotationMetadata}, 选择并返回应导入哪个类的名称.
	 */
	String[] selectImports(AnnotationMetadata importingClassMetadata);

}
