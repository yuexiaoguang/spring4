package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * 标准{@link ApplicationListener}接口的扩展变体, 公开了更多元数据, 例如支持的事件类型.
 *
 * <p><bold>强烈建议</bold>用户使用 {@link GenericApplicationListener}接口, 因为它提供了对基于泛型的事件类型的改进检测.
 */
public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

	/**
	 * 确定此监听器是否实际支持给定的事件类型.
	 */
	boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

	/**
	 * 确定此监听器是否实际支持给定的源类型.
	 */
	boolean supportsSourceType(Class<?> sourceType);

}
