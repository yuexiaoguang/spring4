package org.springframework.messaging.simp.user;

import java.util.Set;

import org.springframework.util.Assert;

/**
 * 包含从源消息解析"user"目标并将其转换为目标 (每个活动用户会话一个)的结果.
 */
public class UserDestinationResult {

	private final String sourceDestination;

	private final Set<String> targetDestinations;

	private final String subscribeDestination;

	private final String user;


	public UserDestinationResult(String sourceDestination, Set<String> targetDestinations,
			String subscribeDestination, String user) {

		Assert.notNull(sourceDestination, "'sourceDestination' must not be null");
		Assert.notNull(targetDestinations, "'targetDestinations' must not be null");
		Assert.notNull(subscribeDestination, "'subscribeDestination' must not be null");

		this.sourceDestination = sourceDestination;
		this.targetDestinations = targetDestinations;
		this.subscribeDestination = subscribeDestination;
		this.user = user;
	}


	/**
	 * 来自源消息的"user"目标.
	 * 订阅时可能看起来像"/user/queue/position-updates", 或发送消息时看起来像"/user/{username}/queue/position-updates".
	 * 
	 * @return "user"目标, never {@code null}.
	 */
	public String getSourceDestination() {
		return this.sourceDestination;
	}

	/**
	 * 源目标转换为的目标, 每个活动用户会话一个, e.g. "/queue/position-updates-useri9oqdfzo".
	 * 
	 * @return 目标, never {@code null}, 但如果用户没有活动会话, 则可能是空集.
	 */
	public Set<String> getTargetDestinations() {
		return this.targetDestinations;
	}

	/**
	 * 客户订阅时预期形式的用户目标, e.g. "/user/queue/position-updates".
	 * 
	 * @return "user"目标的订阅形式, never {@code null}.
	 */
	public String getSubscribeDestination() {
		return this.subscribeDestination;
	}

	/**
	 * 此用户目标的用户.
	 * 
	 * @return 用户名或 {@code null}, 如果只有会话ID, 例如用户未经过身份验证;
	 * 在这种情况下, 可以使用sessionId代替用户名, 从而无需通过{@link SimpUserRegistry}进行用户到会话的查找.
	 */
	public String getUser() {
		return this.user;
	}


	@Override
	public String toString() {
		return "UserDestinationResult [source=" + this.sourceDestination + ", target=" + this.targetDestinations +
				", subscribeDestination=" + this.subscribeDestination + ", user=" + this.user + "]";
	}

}
