package org.springframework.util;

import java.util.UUID;

/**
 * 调用{@link java.util.UUID#randomUUID()}的 {@link IdGenerator}.
 */
public class JdkIdGenerator implements IdGenerator {

	@Override
	public UUID generateId() {
		return UUID.randomUUID();
	}
}
