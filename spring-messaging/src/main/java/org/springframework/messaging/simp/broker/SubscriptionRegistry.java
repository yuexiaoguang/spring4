package org.springframework.messaging.simp.broker;

import org.springframework.messaging.Message;
import org.springframework.util.MultiValueMap;

/**
 * 按会话订阅的注册表, 允许查找订阅.
 */
public interface SubscriptionRegistry {

	/**
	 * 注册由给定消息表示的订阅.
	 * 
	 * @param subscribeMessage 订阅请求
	 */
	void registerSubscription(Message<?> subscribeMessage);

	/**
	 * 注销订阅.
	 * 
	 * @param unsubscribeMessage 要注销的请求
	 */
	void unregisterSubscription(Message<?> unsubscribeMessage);

	/**
	 * 删除与给定sessionId关联的所有订阅.
	 */
	void unregisterAllSubscriptions(String sessionId);

	/**
	 * 查找应接收给定消息的所有订阅.
	 * 返回的映射可以安全迭代, 永远不会被修改.
	 * 
	 * @param message 消息
	 * 
	 * @return 带有sessionId-subscriptionId对的{@code MultiValueMap} (可能为空)
	 */
	MultiValueMap<String, String> findSubscriptions(Message<?> message);

}
