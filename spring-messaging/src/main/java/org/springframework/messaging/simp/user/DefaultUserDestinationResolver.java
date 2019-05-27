package org.springframework.messaging.simp.user;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * {@code UserDestinationResolver}的默认实现, 依赖于{@link SimpUserRegistry}来查找用户的活动会话.
 *
 * <p>当用户试图订阅时, e.g. 对于"/user/queue/position-updates", 删除"/user"前缀, 并根据会话ID添加唯一后缀,
 * e.g. "/queue/position-updates-useri9oqdfzo"确保不同的用户可以订阅相同的逻辑目标而不会发生冲突.
 *
 * <p>当发送给用户时, e.g. "/user/{username}/queue/position-updates", 删除"/user/{username}"前缀,
 * 并添加基于活动会话ID的后缀, e.g. "/queue/position-updates-useri9oqdfzo".
 */
public class DefaultUserDestinationResolver implements UserDestinationResolver {

	private static final Log logger = LogFactory.getLog(DefaultUserDestinationResolver.class);


	private final SimpUserRegistry userRegistry;

	private String prefix = "/user/";

	private boolean removeLeadingSlash = false;


	/**
	 * 创建一个将通过提供的注册表访问用户会话ID信息的实例.
	 * 
	 * @param userRegistry 注册表, never {@code null}
	 */
	public DefaultUserDestinationResolver(SimpUserRegistry userRegistry) {
		Assert.notNull(userRegistry, "SimpUserRegistry must not be null");
		this.userRegistry = userRegistry;
	}


	/**
	 * 返回配置的{@link SimpUserRegistry}.
	 */
	public SimpUserRegistry getSimpUserRegistry() {
		return this.userRegistry;
	}

	/**
	 * 用于标识用户目标的前缀.
	 * 不解析任何不以给定前缀开头的目标.
	 * <p>默认前缀为 "/user/".
	 * 
	 * @param prefix 要使用的前缀
	 */
	public void setUserDestinationPrefix(String prefix) {
		Assert.hasText(prefix, "Prefix must not be empty");
		this.prefix = (prefix.endsWith("/") ? prefix : prefix + "/");
	}

	/**
	 * 返回用户目标的已配置前缀.
	 */
	public String getDestinationPrefix() {
		return this.prefix;
	}

	/**
	 * 使用此属性指示, 是否应删除已转换用户目标的前导斜杠.
	 * 这取决于消息代理配置的目标前缀.
	 * <p>默认{@code false}, i.e. "不要更改目标",
	 * 虽然{@link org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration AbstractMessageBrokerConfiguration}
	 * 可以将其更改为{@code true}, 如果配置的目标没有前导斜杠.
	 * 
	 * @param remove 是否删除前导斜杠
	 */
	public void setRemoveLeadingSlash(boolean remove) {
		this.removeLeadingSlash = remove;
	}

	/**
	 * 是否从目标目标中删除前导斜杠.
	 */
	public boolean isRemoveLeadingSlash() {
		return this.removeLeadingSlash;
	}

	/**
	 * 提供用于处理目标的{@code PathMatcher},
	 * 这有助于确定在删除{@link #setUserDestinationPrefix userDestinationPrefix}后, 是否应将前导斜杠保留在实际目标中.
	 * <p>默认实际目标有一个前导斜杠, e.g. {@code /queue/position-updates} 对于支持使用斜杠作为分隔符的目标的代理来说是有意义的.
	 * 当提供支持备用分隔符的{@code PathMatcher}时, 结果目标将不具有前导斜杠, e.g. {@code jms.queue.position-updates}.
	 * 
	 * @param pathMatcher 用于处理目标的PathMatcher
	 * 
	 * @deprecated 从4.3.14开始, 这个属性不再被使用, 并被{@link #setRemoveLeadingSlash(boolean)}取代,
	 * 它更明确地表明是否保留前导斜杠, 无论{@code PathMatcher}如何配置.
	 */
	@Deprecated
	public void setPathMatcher(PathMatcher pathMatcher) {
		// Do nothing
	}


	@Override
	public UserDestinationResult resolveDestination(Message<?> message) {
		ParseResult parseResult = parse(message);
		if (parseResult == null) {
			return null;
		}
		String user = parseResult.getUser();
		String sourceDestination = parseResult.getSourceDestination();
		Set<String> targetSet = new HashSet<String>();
		for (String sessionId : parseResult.getSessionIds()) {
			String actualDestination = parseResult.getActualDestination();
			String targetDestination = getTargetDestination(
					sourceDestination, actualDestination, sessionId, user);
			if (targetDestination != null) {
				targetSet.add(targetDestination);
			}
		}
		String subscribeDestination = parseResult.getSubscribeDestination();
		return new UserDestinationResult(sourceDestination, targetSet, subscribeDestination, user);
	}

	private ParseResult parse(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		String sourceDestination = SimpMessageHeaderAccessor.getDestination(headers);
		if (sourceDestination == null || !checkDestination(sourceDestination, this.prefix)) {
			return null;
		}
		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		switch (messageType) {
			case SUBSCRIBE:
			case UNSUBSCRIBE:
				return parseSubscriptionMessage(message, sourceDestination);
			case MESSAGE:
				return parseMessage(headers, sourceDestination);
			default:
				return null;
		}
	}

	private ParseResult parseSubscriptionMessage(Message<?> message, String sourceDestination) {
		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (sessionId == null) {
			logger.error("No session id. Ignoring " + message);
			return null;
		}
		int prefixEnd = this.prefix.length() - 1;
		String actualDestination = sourceDestination.substring(prefixEnd);
		if (isRemoveLeadingSlash()) {
			actualDestination = actualDestination.substring(1);
		}
		Principal principal = SimpMessageHeaderAccessor.getUser(headers);
		String user = (principal != null ? principal.getName() : null);
		Set<String> sessionIds = Collections.singleton(sessionId);
		return new ParseResult(sourceDestination, actualDestination, sourceDestination, sessionIds, user);
	}

	private ParseResult parseMessage(MessageHeaders headers, String sourceDest) {
		int prefixEnd = this.prefix.length();
		int userEnd = sourceDest.indexOf('/', prefixEnd);
		Assert.isTrue(userEnd > 0, "Expected destination pattern \"/user/{userId}/**\"");
		String actualDest = sourceDest.substring(userEnd);
		String subscribeDest = this.prefix.substring(0, prefixEnd - 1) + actualDest;
		String userName = sourceDest.substring(prefixEnd, userEnd);
		userName = StringUtils.replace(userName, "%2F", "/");

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		Set<String> sessionIds;
		if (userName.equals(sessionId)) {
			userName = null;
			sessionIds = Collections.singleton(sessionId);
		}
		else {
			sessionIds = getSessionIdsByUser(userName, sessionId);
		}

		if (isRemoveLeadingSlash()) {
			actualDest = actualDest.substring(1);
		}
		return new ParseResult(sourceDest, actualDest, subscribeDest, sessionIds, userName);
	}

	private Set<String> getSessionIdsByUser(String userName, String sessionId) {
		Set<String> sessionIds;
		SimpUser user = this.userRegistry.getUser(userName);
		if (user != null) {
			if (user.getSession(sessionId) != null) {
				sessionIds = Collections.singleton(sessionId);
			}
			else {
				Set<SimpSession> sessions = user.getSessions();
				sessionIds = new HashSet<String>(sessions.size());
				for (SimpSession session : sessions) {
					sessionIds.add(session.getId());
				}
			}
		}
		else {
			sessionIds = Collections.emptySet();
		}
		return sessionIds;
	}

	protected boolean checkDestination(String destination, String requiredPrefix) {
		return destination.startsWith(requiredPrefix);
	}

	/**
	 * 此方法确定如何将源"user"目标转换为给定活动用户会话的实际目标.
	 * 
	 * @param sourceDestination 来自输入消息的源目标.
	 * @param actualDestination 目标的子集, 没有任何用户前缀.
	 * @param sessionId 活动用户会话的ID, never {@code null}.
	 * @param user 目标用户, 可能是{@code null}, e.g 如果未经过身份验证.
	 * 
	 * @return 目标, 或{@code null}
	 */
	@SuppressWarnings("unused")
	protected String getTargetDestination(String sourceDestination, String actualDestination,
			String sessionId, String user) {

		return actualDestination + "-user" + sessionId;
	}

	@Override
	public String toString() {
		return "DefaultUserDestinationResolver[prefix=" + this.prefix + "]";
	}


	/**
	 * 已解析的源"user"目标的临时占位符.
	 */
	private static class ParseResult {

		private final String sourceDestination;

		private final String actualDestination;

		private final String subscribeDestination;

		private final Set<String> sessionIds;

		private final String user;

		public ParseResult(String sourceDest, String actualDest, String subscribeDest,
				Set<String> sessionIds, String user) {

			this.sourceDestination = sourceDest;
			this.actualDestination = actualDest;
			this.subscribeDestination = subscribeDest;
			this.sessionIds = sessionIds;
			this.user = user;
		}

		public String getSourceDestination() {
			return this.sourceDestination;
		}

		public String getActualDestination() {
			return this.actualDestination;
		}

		public String getSubscribeDestination() {
			return this.subscribeDestination;
		}

		public Set<String> getSessionIds() {
			return this.sessionIds;
		}

		public String getUser() {
			return this.user;
		}
	}

}
