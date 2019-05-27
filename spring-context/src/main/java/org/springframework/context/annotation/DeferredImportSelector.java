package org.springframework.context.annotation;

/**
 * 在处理完所有{@code @Configuration} bean之后, 运行的{@link ImportSelector}的变体.
 * 当选择的导入为{@code @Conditional}时, 此类选择器特别有用.
 *
 * <p>实现还可以扩展 {@link org.springframework.core.Ordered} 接口
 * 或使用{@link org.springframework.core.annotation.Order}注解来指示其他{@link DeferredImportSelector}的优先级.
 */
public interface DeferredImportSelector extends ImportSelector {

}
