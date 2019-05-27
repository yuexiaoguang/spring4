package org.springframework.context;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.util.Assert;

/**
 * 带有任意负载的{@link ApplicationEvent}.
 *
 * <p>主要用于框架内部使用.
 * 
 * @param <T> 事件的有效负载类型
 */
@SuppressWarnings("serial")
public class PayloadApplicationEvent<T> extends ApplicationEvent implements ResolvableTypeProvider {

	private final T payload;


	/**
	 * @param source 事件最初发生的对象 (never {@code null})
	 * @param payload 有效负载对象 (never {@code null})
	 */
	public PayloadApplicationEvent(Object source, T payload) {
		super(source);
		Assert.notNull(payload, "Payload must not be null");
		this.payload = payload;
	}


	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getPayload()));
	}

	/**
	 * 返回事件的有效负载.
	 */
	public T getPayload() {
		return this.payload;
	}

}
