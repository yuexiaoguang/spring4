package org.springframework.core;

import java.io.IOException;

/**
 * {@link IOException}的子类, 正确处理根本原因, 暴露根本原因就像NestedChecked/RuntimeException一样.
 *
 * <p>在Java 6之前, 没有将正确的根本原因处理添加到标准IOException, 这就是我们为了Java 5兼容性目的而需要自己做的原因.
 *
 * <p>此类与NestedChecked/RuntimeException类之间的相似性是不可避免的, 因为此类需要从IOException派生.
 */
@SuppressWarnings("serial")
public class NestedIOException extends IOException {

	static {
		// 在调用getMessage()时, 实时地加载NestedExceptionUtils类以避免OSGi上的类加载器死锁问题. Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	public NestedIOException(String msg) {
		super(msg);
	}

	public NestedIOException(String msg, Throwable cause) {
		super(msg, cause);
	}


	@Override
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}

}
