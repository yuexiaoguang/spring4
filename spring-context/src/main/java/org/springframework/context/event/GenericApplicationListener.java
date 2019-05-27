package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

/**
 * 标准{@link ApplicationListener}接口的扩展变体, 公开了更多元数据, 例如支持的事件类型.
 *
 * <p>从Spring Framework 4.2开始, 取代{@link SmartApplicationListener}, 并正确处理基于泛型的事件.
 */
public interface GenericApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

	/**
	 * 确定此监听器是否实际支持给定的事件类型.
	 */
	boolean supportsEventType(ResolvableType eventType);

	/**
	 * 确定此监听器是否实际支持给定的源类型.
	 */
	boolean supportsSourceType(Class<?> sourceType);

}
