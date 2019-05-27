package org.springframework.beans;

/**
 * 使用{@link org.springframework.beans.PropertyEditorRegistry属性编辑器注册表}
 * 注册自定义{@link java.beans.PropertyEditor属性编辑器}的策略接口.
 *
 * <p>当需要在几种不同情况下使用同一组属性编辑器时, 这尤其有用:
 * 编写相应的注册器并在每种情况下重复使用.
 */
public interface PropertyEditorRegistrar {

	/**
	 * 使用给定的{@code PropertyEditorRegistry}注册自定义的 {@link java.beans.PropertyEditor PropertyEditors}.
	 * <p>传入的注册表通常是{@link BeanWrapper}或{@link org.springframework.validation.DataBinder DataBinder}.
	 * <p>预计实现将为每次调用创建新的{@code PropertyEditors}实例 (因为{@code PropertyEditors}不是线程安全的).
	 * 
	 * @param registry 注册自定义{@code PropertyEditors}使用的{@code PropertyEditorRegistry}
	 */
	void registerCustomEditors(PropertyEditorRegistry registry);

}
