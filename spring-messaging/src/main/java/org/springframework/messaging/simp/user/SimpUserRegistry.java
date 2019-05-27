package org.springframework.messaging.simp.user;

import java.util.Set;

/**
 * 当前连接的用户的注册表.
 */
public interface SimpUserRegistry {

	/**
	 * 获取给定名称的用户.
	 * 
	 * @param userName 要查找的用户的名称
	 * 
	 * @return 用户, 如果没有连接, 则为{@code null}
	 */
	SimpUser getUser(String userName);

	/**
	 * 返回所有已连接用户的快照.
	 * <p>返回的Set是一个副本, 不会反映进一步的更改.
	 * 
	 * @return 已连接的用户, 或空集合
	 */
	Set<SimpUser> getUsers();

	/**
	 * 返回所有连接用户的计数.
	 * 
	 * @return 已连接的用户的数量
	 */
	int getUserCount();

	/**
	 * 使用给定的匹配器查找订阅.
	 * 
	 * @param matcher 要使用的匹配器
	 * 
	 * @return 匹配的订阅, 或空集合
	 */
	Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher);

}
