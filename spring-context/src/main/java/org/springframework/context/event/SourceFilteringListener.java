package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

/**
 * {@link org.springframework.context.ApplicationListener}装饰器, 用于过滤来自指定事件源的事件,
 * 调用其委托监听器, 仅用于匹配 {@link org.springframework.context.ApplicationEvent}对象.
 *
 * <p>也可以用作基类, 覆盖{@link #onApplicationEventInternal}方法, 而不是指定委托监听器.
 */
public class SourceFilteringListener implements GenericApplicationListener, SmartApplicationListener {

	private final Object source;

	private GenericApplicationListener delegate;


	/**
	 * @param source 此监听器过滤的事件源, 仅处理来自此源的事件
	 * @param delegate 委托监听器使用指定源中的事件进行调用
	 */
	public SourceFilteringListener(Object source, ApplicationListener<?> delegate) {
		this.source = source;
		this.delegate = (delegate instanceof GenericApplicationListener ?
				(GenericApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
	}

	/**
	 * 为给定的事件源创建SourceFilteringListener, 期望子类覆盖 {@link #onApplicationEventInternal}方法 (而不是指定委托监听器).
	 * 
	 * @param source 此监听器过滤的事件源, 仅处理来自此源的事件
	 */
	protected SourceFilteringListener(Object source) {
		this.source = source;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event.getSource() == this.source) {
			onApplicationEventInternal(event);
		}
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		return (this.delegate == null || this.delegate.supportsEventType(eventType));
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return supportsEventType(ResolvableType.forType(eventType));
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return (sourceType != null && sourceType.isInstance(this.source));
	}

	@Override
	public int getOrder() {
		return (this.delegate != null ? this.delegate.getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	/**
	 * 实际处理事件, 在已经根据所需的事件源进行过滤后.
	 * <p>默认实现调用指定的委托.
	 * 
	 * @param event 要处理的事件 (匹配指定的源)
	 */
	protected void onApplicationEventInternal(ApplicationEvent event) {
		if (this.delegate == null) {
			throw new IllegalStateException(
					"Must specify a delegate object or override the onApplicationEventInternal method");
		}
		this.delegate.onApplicationEvent(event);
	}

}
