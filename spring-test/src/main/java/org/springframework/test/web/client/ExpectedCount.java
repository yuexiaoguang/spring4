package org.springframework.test.web.client;

import org.springframework.util.Assert;

/**
 * 表示预期计数范围的简单类型.
 *
 * <p>示例:
 * <pre>
 * import static org.springframework.test.web.client.ExpectedCount.*
 *
 * once()
 * manyTimes()
 * times(5)
 * min(2)
 * max(4)
 * between(2, 4)
 * never()
 * </pre>
 */
public class ExpectedCount {

	private final int minCount;

	private final int maxCount;


	private ExpectedCount(int minCount, int maxCount) {
		Assert.isTrue(minCount >= 0, "minCount >= 0 is required");
		Assert.isTrue(maxCount >= minCount, "maxCount >= minCount is required");
		this.minCount = minCount;
		this.maxCount = maxCount;
	}


	/**
	 * 返回预期计数范围的{@code min}边界.
	 */
	public int getMinCount() {
		return this.minCount;
	}

	/**
	 * 返回预期计数范围的{@code max}边界.
	 */
	public int getMaxCount() {
		return this.maxCount;
	}


	/**
	 * 就一次.
	 */
	public static ExpectedCount once() {
		return new ExpectedCount(1, 1);
	}

	/**
	 * 多次 (1..Integer.MAX_VALUE).
	 */
	public static ExpectedCount manyTimes() {
		return new ExpectedCount(1, Integer.MAX_VALUE);
	}

	/**
	 * N 次.
	 */
	public static ExpectedCount times(int count) {
		Assert.isTrue(count >= 1, "'count' must be >= 1");
		return new ExpectedCount(count, count);
	}

	/**
	 * 至少{@code min}次.
	 */
	public static ExpectedCount min(int min) {
		Assert.isTrue(min >= 1, "'min' must be >= 1");
		return new ExpectedCount(min, Integer.MAX_VALUE);
	}

	/**
	 * 最多{@code max}次.
	 */
	public static ExpectedCount max(int max) {
		Assert.isTrue(max >= 1, "'max' must be >= 1");
		return new ExpectedCount(1, max);
	}

	/**
	 * 根本没有预期的调用, i.e. min=0 和 max=0.
	 */
	public static ExpectedCount never() {
		return new ExpectedCount(0, 0);
	}

	/**
	 * 在{@code min}和{@code max}次之间.
	 */
	public static ExpectedCount between(int min, int max) {
		return new ExpectedCount(min, max);
	}

}
