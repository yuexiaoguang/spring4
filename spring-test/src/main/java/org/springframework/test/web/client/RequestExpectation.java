package org.springframework.test.web.client;

/**
 * {@code ResponseActions}的扩展, 它还实现了{@code RequestMatcher}和{@code ResponseCreator}
 *
 * <p>虽然{@code ResponseActions}是用于定义期望的API, 但此子接口是内部SPI, 用于将这些期望与实际请求相匹配并创建响应.
 */
public interface RequestExpectation extends ResponseActions, RequestMatcher, ResponseCreator {

	/**
	 * 是否存在针对此期望的剩余调用次数.
	 */
	boolean hasRemainingCount();

	/**
	 * 是否满足此请求期望的要求.
	 */
	boolean isSatisfied();

}
