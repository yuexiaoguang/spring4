package org.springframework.web.bind.support;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingErrorProcessor;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.WebRequest;

/**
 * 用于Spring应用程序上下文中的声明性配置的{@link WebBindingInitializer}.
 * 允许使用多个控制器/处理器重用预配置的初始化器.
 */
public class ConfigurableWebBindingInitializer implements WebBindingInitializer {

	private boolean autoGrowNestedPaths = true;

	private boolean directFieldAccess = false;

	private MessageCodesResolver messageCodesResolver;

	private BindingErrorProcessor bindingErrorProcessor;

	private Validator validator;

	private ConversionService conversionService;

	private PropertyEditorRegistrar[] propertyEditorRegistrars;


	/**
	 * 设置绑定器是否应尝试"auto-grow"包含null值的嵌套路径.
	 * <p>如果为"true", 则将使用默认对象值填充null路径位置并遍历, 而不是导致异常.
	 * 此标志还允许在访问越界索引时自动增长集合元素.
	 * <p>标准DataBinder上的默认值为"true". 请注意, 此功能仅支持bean属性访问 (DataBinder的默认模式), 而不支持字段访问.
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * 返回绑定器是否应尝试"auto-grow"包含null值的嵌套路径.
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * 设置是否使用直接字段访问而不是bean属性访问.
	 * <p>默认{@code false}, 使用bean属性访问.
	 * 将其切换为{@code true}以强制使用直接字段访问.
	 */
	public final void setDirectFieldAccess(boolean directFieldAccess) {
		this.directFieldAccess = directFieldAccess;
	}

	/**
	 * 返回是否使用直接字段访问而不是bean属性访问.
	 */
	public boolean isDirectFieldAccess() {
		return directFieldAccess;
	}

	/**
	 * 设置用于将错误解析为消息码的策略.
	 * 将给定策略应用于此控制器使用的所有数据绑定器.
	 * <p>默认{@code null}, i.e. 使用数据绑定器的默认策略.
	 */
	public final void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * 返回用于将错误解析为消息码的策略.
	 */
	public final MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}

	/**
	 * 设置用于处理绑定错误的策略, 即必需的字段错误和{@code PropertyAccessException}.
	 * <p>默认{@code null}, 即使用数据绑定器的默认策略.
	 */
	public final void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * 返回用于处理绑定错误的策略.
	 */
	public final BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * 设置在每个绑定步骤后应用的Validator.
	 */
	public final void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * 返回在每个绑定步骤后应用的Validator.
	 */
	public final Validator getValidator() {
		return this.validator;
	}

	/**
	 * 指定将应用于每个DataBinder的ConversionService.
	 */
	public final void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回将应用于每个DataBinder的ConversionService.
	 */
	public final ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 指定要应用于每个DataBinder的单个PropertyEditorRegistrar.
	 */
	public final void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
		this.propertyEditorRegistrars = new PropertyEditorRegistrar[] {propertyEditorRegistrar};
	}

	/**
	 * 指定要应用于每个DataBinder的多个PropertyEditorRegistrar.
	 */
	public final void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * 返回要应用于每个DataBinder的PropertyEditorRegistrar.
	 */
	public final PropertyEditorRegistrar[] getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}


	@Override
	public void initBinder(WebDataBinder binder, WebRequest request) {
		binder.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
		if (this.directFieldAccess) {
			binder.initDirectFieldAccess();
		}
		if (this.messageCodesResolver != null) {
			binder.setMessageCodesResolver(this.messageCodesResolver);
		}
		if (this.bindingErrorProcessor != null) {
			binder.setBindingErrorProcessor(this.bindingErrorProcessor);
		}
		if (this.validator != null && binder.getTarget() != null &&
				this.validator.supports(binder.getTarget().getClass())) {
			binder.setValidator(this.validator);
		}
		if (this.conversionService != null) {
			binder.setConversionService(this.conversionService);
		}
		if (this.propertyEditorRegistrars != null) {
			for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
				propertyEditorRegistrar.registerCustomEditors(binder);
			}
		}
	}

}
