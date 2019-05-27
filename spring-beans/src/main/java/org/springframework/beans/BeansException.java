package org.springframework.beans;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.ObjectUtils;

/**
 * bean包和子包中抛出的所有异常的抽象超类.
 *
 * <p>请注意，这是一个运行时（未受检）的异常. Bean类异常通常是致命的; 没有理由是受检异常.
 */
@SuppressWarnings("serial")
public abstract class BeansException extends NestedRuntimeException {

	public BeansException(String msg) {
		super(msg);
	}

	public BeansException(String msg, Throwable cause) {
		super(msg, cause);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BeansException)) {
			return false;
		}
		BeansException otherBe = (BeansException) other;
		return (getMessage().equals(otherBe.getMessage()) &&
				ObjectUtils.nullSafeEquals(getCause(), otherBe.getCause()));
	}

	@Override
	public int hashCode() {
		return getMessage().hashCode();
	}
}
