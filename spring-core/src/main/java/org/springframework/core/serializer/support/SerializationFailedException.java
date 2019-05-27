package org.springframework.core.serializer.support;

import org.springframework.core.NestedRuntimeException;

/**
 * 当{@link org.springframework.core.serializer.Serializer}或{@link org.springframework.core.serializer.Deserializer}失败时,
 * 本机IOException (或类似)的包装器.
 * 由{@link SerializingConverter}和{@link DeserializingConverter}抛出.
 */
@SuppressWarnings("serial")
public class SerializationFailedException extends NestedRuntimeException {

	public SerializationFailedException(String message) {
		super(message);
	}

	public SerializationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
