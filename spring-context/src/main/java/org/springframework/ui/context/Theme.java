package org.springframework.ui.context;

import org.springframework.context.MessageSource;

/**
 * 主题, 可以解析特定于主题的消息, 代码, 文件路径等 (e&#46;g&#46; Web环境中的CSS和图像文件).
 * 公开的{@link org.springframework.context.MessageSource}, 支持特定于主题的参数化和国际化.
 */
public interface Theme {

	/**
	 * 返回主题的名称.
	 * 
	 * @return 主题的名称 (never {@code null})
	 */
	String getName();

	/**
	 * 返回解析与此主题相关的消息的特定MessageSource.
	 * 
	 * @return 特定于主题的MessageSource (never {@code null})
	 */
	MessageSource getMessageSource();

}
