package org.springframework.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个简单的{@link IdGenerator}, 从1开始, 每次调用递增1.
 */
public class SimpleIdGenerator implements IdGenerator {

	private final AtomicLong mostSigBits = new AtomicLong(0);

	private final AtomicLong leastSigBits = new AtomicLong(0);


	@Override
	public UUID generateId() {
		long leastSigBits = this.leastSigBits.incrementAndGet();
		if (leastSigBits == 0) {
			this.mostSigBits.incrementAndGet();
		}
		return new UUID(this.mostSigBits.get(), leastSigBits);
	}

}
