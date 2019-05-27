package org.springframework.context;

import java.util.Locale;

/**
 * 用于解析消息的策略接口, 支持此类消息的参数化和国际化.
 *
 * <p>Spring为生产提供了两种开箱即用的实现:
 * <ul>
 * <li>{@link org.springframework.context.support.ResourceBundleMessageSource}, 建立在标准 {@link java.util.ResourceBundle}之上
 * <li>{@link org.springframework.context.support.ReloadableResourceBundleMessageSource}, 能够在不重新启动VM的情况下重新加载消息定义
 * </ul>
 */
public interface MessageSource {

	/**
	 * 尝试解析该消息. 如果未找到消息, 则返回默认消息.
	 * 
	 * @param code 要查找的代码, 例如 'calculator.noRateSet'.
	 * 鼓励此类用户在相关的完全限定类名上建立消息名称, 从而避免冲突, 并确保最大程度的清晰度.
	 * @param args 一个参数数组, 将填充消息中的参数 (params在消息中看起来像 "{0}", "{1,date}", "{2,time}"), 或{@code null}.
	 * @param defaultMessage 查找失败时返回的默认消息
	 * @param locale 要在其中执行查找的区域设置
	 * 
	 * @return 如果查找成功, 则解析消息; 否则默认消息作为参数传递
	 */
	String getMessage(String code, Object[] args, String defaultMessage, Locale locale);

	/**
	 * 尝试解析该消息. 如果无法找到消息, 则视为错误.
	 * 
	 * @param code 要查找的代码, 例如 'calculator.noRateSet'
	 * @param args 一个参数数组, 将填充消息中的参数 (params在消息中看起来像 "{0}", "{1,date}", "{2,time}"), 或{@code null}.
	 * @param locale 要在其中执行查找的区域设置
	 * 
	 * @return 已解析的消息
	 * @throws NoSuchMessageException 如果没有找到该消息
	 */
	String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException;

	/**
	 * 尝试使用传入的{@code MessageSourceResolvable}参数中包含的所有属性来解析消息.
	 * <p>NOTE: 必须在此方法上抛出{@code NoSuchMessageException},
	 * 因为在调用此方法时, 无法确定resolvable的{@code defaultMessage}属性是否为{@code null}.
	 * 
	 * @param resolvable 值对象, 用于存储解析消息所需的属性
	 * @param locale 要在其中执行查找的区域设置
	 * 
	 * @return 已解析的消息
	 * @throws NoSuchMessageException 如果没有找到该消息
	 */
	String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;

}
