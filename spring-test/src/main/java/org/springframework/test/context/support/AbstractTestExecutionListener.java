package org.springframework.test.context.support;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * {@link TestExecutionListener}接口的抽象实现, 它提供空方法存根.
 * 子类可以扩展此类, 并仅覆盖适用于手头任务的那些方法.
 */
public abstract class AbstractTestExecutionListener implements TestExecutionListener, Ordered {

	/**
	 * 默认实现是<em>为空</em>. 必要时可以由子类重写.
	 */
	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * 默认实现是<em>为空</em>. 必要时可以由子类重写.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * 默认实现是<em>为空</em>. 必要时可以由子类重写.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * 默认实现是<em>为空</em>. 必要时可以由子类重写.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * 默认实现是<em>为空</em>. 必要时可以由子类重写.
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * 默认实现返回{@link Ordered#LOWEST_PRECEDENCE}, 从而确保在框架提供的默认监听器之后对自定义监听器进行排序.
	 * 必要时可以由子类重写.
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
