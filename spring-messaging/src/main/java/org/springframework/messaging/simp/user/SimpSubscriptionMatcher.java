package org.springframework.messaging.simp.user;

/**
 * 匹配订阅的策略.
 */
public interface SimpSubscriptionMatcher {

	/**
	 * 匹配给定的订阅.
	 * 
	 * @param subscription 要匹配的订阅
	 * 
	 * @return {@code true}匹配, 否则{@code false}
	 */
	boolean match(SimpSubscription subscription);

}
