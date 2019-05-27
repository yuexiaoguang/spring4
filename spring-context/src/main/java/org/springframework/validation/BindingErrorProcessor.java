package org.springframework.validation;

import org.springframework.beans.PropertyAccessException;

/**
 * 处理{@code DataBinder}的丢失字段错误, 以及将{@code PropertyAccessException}转换为{@code FieldError}的策略.
 *
 * <p>错误处理器是可插拔的, 因此可以根据需要以不同方式处理错误.
 * 针对典型需求提供默认实现.
 *
 * <p>Note: 从Spring 2.0开始, 此接口在给定的BindingResult上运行, 以与任何绑定策略 (bean属性, 直接字段访问, etc)兼容.
 * 它仍然可以接收BindException作为参数 (因为BindException也实现了BindingResult接口), 但不再直接对它进行操作.
 */
public interface BindingErrorProcessor {

	/**
	 * 将缺少的字段错误应用于给定的BindException.
	 * <p>通常, 会为缺少的必填字段创建字段错误.
	 * 
	 * @param missingField 绑定期间丢失的字段
	 * @param bindingResult 要添加错误的错误对象.
	 * 可以添加多个错误, 甚至可以忽略它.
	 * {@code BindingResult}对象具有便利工具, 例如{@code resolveMessageCodes}方法来解析错误代码.
	 */
	void processMissingFieldError(String missingField, BindingResult bindingResult);

	/**
	 * 将给定的{@code PropertyAccessException}转换为在给定的{@code Errors}实例上注册的相应错误.
	 * <p>请注意, 有两种错误类型可用: {@code FieldError} 和 {@code ObjectError}.
	 * 通常, 会创建字段错误, 但在某些情况下, 可能需要创建全局{@code ObjectError}.
	 * 
	 * @param ex 要翻译的{@code PropertyAccessException}
	 * @param bindingResult 要添加错误的错误对象.
	 * 可以添加多个错误, 甚至可以忽略它.
	 * {@code BindingResult}对象具有便利工具, 例如{@code resolveMessageCodes}方法来解析错误代码.
	 */
	void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult);

}
