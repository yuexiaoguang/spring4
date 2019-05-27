package org.springframework.context.annotation;

import org.springframework.beans.factory.Aware;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 希望注入导入它的{@link AnnotationMetadata}的@{@link Configuration}类实现的接口.
 * 与使用 @{@link Import}作为元注解的注解结合使用.
 */
public interface ImportAware extends Aware {

	/**
	 * 设置导入@{@code Configuration}类的注解元数据.
	 */
	void setImportMetadata(AnnotationMetadata importMetadata);

}
