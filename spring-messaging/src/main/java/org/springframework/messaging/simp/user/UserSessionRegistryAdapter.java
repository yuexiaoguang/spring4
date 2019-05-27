package org.springframework.messaging.simp.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.CollectionUtils;

/**
 * 适配器, 允许{@code UserSessionRegistry}用作{@code SimpUserRegistry}, 不推荐使用{@code SimpUserRegistry}.
 * 由于可用信息有限, 不支持{@link #getUsers()} 和 {@link #findSubscriptions}等方法.
 *
 * <p>从4.2开始, 此适配器仅用于通过重写
 * {@link org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration#userSessionRegistry()}
 * 在应用程序中显式注册自定义{@code UserSessionRegistry} bean.
 */
@SuppressWarnings("deprecation")
public class UserSessionRegistryAdapter implements SimpUserRegistry {

	private final UserSessionRegistry userSessionRegistry;


	public UserSessionRegistryAdapter(UserSessionRegistry registry) {
		this.userSessionRegistry = registry;
	}


	@Override
	public SimpUser getUser(String userName) {
		Set<String> sessionIds = this.userSessionRegistry.getSessionIds(userName);
		return (!CollectionUtils.isEmpty(sessionIds) ? new SimpUserAdapter(userName, sessionIds) : null);
	}

	@Override
	public Set<SimpUser> getUsers() {
		throw new UnsupportedOperationException("UserSessionRegistry does not expose a listing of users");
	}

	@Override
	public int getUserCount() {
		throw new UnsupportedOperationException("UserSessionRegistry does not expose a user count");
	}

	@Override
	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		throw new UnsupportedOperationException("UserSessionRegistry does not support operations across users");
	}


	/**
	 * 将UserSessionRegistry (名称和会话ID)中提供的唯一信息公开为{@code SimpUser}.
	 */
	private static class SimpUserAdapter implements SimpUser {

		private final String name;

		private final Map<String, SimpSession> sessions;

		public SimpUserAdapter(String name, Set<String> sessionIds) {
			this.name = name;
			this.sessions = new HashMap<String, SimpSession>(sessionIds.size());
			for (String sessionId : sessionIds) {
				this.sessions.put(sessionId, new SimpSessionAdapter(sessionId));
			}
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean hasSessions() {
			return !this.sessions.isEmpty();
		}

		@Override
		public SimpSession getSession(String sessionId) {
			return this.sessions.get(sessionId);
		}

		@Override
		public Set<SimpSession> getSessions() {
			return new HashSet<SimpSession>(this.sessions.values());
		}
	}


	/**
	 * 将来自UserSessionRegistry (会话ID, 但没有订阅)的信息公开为{@code SimpSession}.
	 */
	private static class SimpSessionAdapter implements SimpSession {

		private final String id;

		public SimpSessionAdapter(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public SimpUser getUser() {
			return null;
		}

		@Override
		public Set<SimpSubscription> getSubscriptions() {
			return Collections.<SimpSubscription>emptySet();
		}
	}

}
