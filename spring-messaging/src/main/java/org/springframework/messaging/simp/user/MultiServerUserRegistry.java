package org.springframework.messaging.simp.user;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@code SimpUserRegistry}, 查找"本地"用户注册表中的用户, 以及一组"远程"用户注册表.
 * 本地注册表作为构造函数参数提供, 而远程注册表通过 {@link UserRegistryMessageHandler}处理的广播进行更新,
 * 其在收到更新时会通知此注册表.
 */
@SuppressWarnings("serial")
public class MultiServerUserRegistry implements SimpUserRegistry, SmartApplicationListener {

	private final String id;

	private final SimpUserRegistry localRegistry;

	private final Map<String, UserRegistrySnapshot> remoteRegistries = new ConcurrentHashMap<String, UserRegistrySnapshot>();

	private final boolean delegateApplicationEvents;

	/* 跨服务器会话查找 (e.g. 连接到多个服务器的同一用户) */
	private final SessionLookup sessionLookup = new SessionLookup();


	/**
	 * 包装本地用户注册表.
	 */
	public MultiServerUserRegistry(SimpUserRegistry localRegistry) {
		Assert.notNull(localRegistry, "'localRegistry' is required.");
		this.id = generateId();
		this.localRegistry = localRegistry;
		this.delegateApplicationEvents = this.localRegistry instanceof SmartApplicationListener;
	}

	private static String generateId() {
		String host;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException ex) {
			host = "unknown";
		}
		return host + '-' + UUID.randomUUID();
	}


	@Override
	public int getOrder() {
		return (this.delegateApplicationEvents ?
				((SmartApplicationListener) this.localRegistry).getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	// SmartApplicationListener methods

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return (this.delegateApplicationEvents &&
				((SmartApplicationListener) this.localRegistry).supportsEventType(eventType));
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return (this.delegateApplicationEvents &&
				((SmartApplicationListener) this.localRegistry).supportsSourceType(sourceType));
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (this.delegateApplicationEvents) {
			((SmartApplicationListener) this.localRegistry).onApplicationEvent(event);
		}
	}


	// SimpUserRegistry methods

	@Override
	public SimpUser getUser(String userName) {
		// 由于跨服务器SessionLookup, 首选远程注册表
		for (UserRegistrySnapshot registry : this.remoteRegistries.values()) {
			SimpUser user = registry.getUserMap().get(userName);
			if (user != null) {
				return user;
			}
		}
		return this.localRegistry.getUser(userName);
	}

	@Override
	public Set<SimpUser> getUsers() {
		// 由于跨服务器SessionLookup, 首选远程注册表
		Set<SimpUser> result = new HashSet<SimpUser>();
		for (UserRegistrySnapshot registry : this.remoteRegistries.values()) {
			result.addAll(registry.getUserMap().values());
		}
		result.addAll(this.localRegistry.getUsers());
		return result;
	}

	@Override
	public int getUserCount() {
		int userCount = 0;
		for (UserRegistrySnapshot registry : this.remoteRegistries.values()) {
			userCount += registry.getUserMap().size();
		}
		userCount += this.localRegistry.getUserCount();
		return userCount;
	}

	@Override
	public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
		Set<SimpSubscription> result = new HashSet<SimpSubscription>();
		for (UserRegistrySnapshot registry : this.remoteRegistries.values()) {
			result.addAll(registry.findSubscriptions(matcher));
		}
		result.addAll(this.localRegistry.findSubscriptions(matcher));
		return result;
	}


	// Internal methods for UserRegistryMessageHandler to manage broadcasts

	Object getLocalRegistryDto() {
		return new UserRegistrySnapshot(this.id, this.localRegistry);
	}

	void addRemoteRegistryDto(Message<?> message, MessageConverter converter, long expirationPeriod) {
		UserRegistrySnapshot registry = (UserRegistrySnapshot) converter.fromMessage(message, UserRegistrySnapshot.class);
		if (registry != null && !registry.getId().equals(this.id)) {
			registry.init(expirationPeriod, this.sessionLookup);
			this.remoteRegistries.put(registry.getId(), registry);
		}
	}

	void purgeExpiredRegistries() {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<String, UserRegistrySnapshot>> iterator = this.remoteRegistries.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, UserRegistrySnapshot> entry = iterator.next();
			if (entry.getValue().isExpired(now)) {
				iterator.remove();
			}
		}
	}


	@Override
	public String toString() {
		return "local=[" + this.localRegistry +	"], remote=" + this.remoteRegistries;
	}


	/**
	 * 保存SimpUserRegistry的副本, 以便向其他应用程序服务器广播和接收广播.
	 */
	private static class UserRegistrySnapshot {

		private String id;

		private Map<String, TransferSimpUser> users;

		private long expirationTime;

		/**
		 * 用于JSON反序列化.
		 */
		@SuppressWarnings("unused")
		public UserRegistrySnapshot() {
		}

		/**
		 * 用于从本地用户注册表创建DTO.
		 */
		public UserRegistrySnapshot(String id, SimpUserRegistry registry) {
			this.id = id;
			Set<SimpUser> users = registry.getUsers();
			this.users = new HashMap<String, TransferSimpUser>(users.size());
			for (SimpUser user : users) {
				this.users.put(user.getName(), new TransferSimpUser(user));
			}
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

		public void setUserMap(Map<String, TransferSimpUser> users) {
			this.users = users;
		}

		public Map<String, TransferSimpUser> getUserMap() {
			return this.users;
		}

		public boolean isExpired(long now) {
			return (now > this.expirationTime);
		}

		public void init(long expirationPeriod, SessionLookup sessionLookup) {
			this.expirationTime = System.currentTimeMillis() + expirationPeriod;
			for (TransferSimpUser user : this.users.values()) {
				user.afterDeserialization(sessionLookup);
			}
		}

		public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
			Set<SimpSubscription> result = new HashSet<SimpSubscription>();
			for (TransferSimpUser user : this.users.values()) {
				for (TransferSimpSession session : user.sessions) {
					for (SimpSubscription subscription : session.subscriptions) {
						if (matcher.match(subscription)) {
							result.add(subscription);
						}
					}
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return "id=" + this.id + ", users=" + this.users;
		}
	}


	/**
	 * 可以(反)序列化并广播到其他服务器.
	 */
	private static class TransferSimpUser implements SimpUser {

		private String name;

		/* 仅来自"this"注册表的用户会话 (i.e. 一个服务器) */
		private Set<TransferSimpSession> sessions;

		/* 跨服务器会话查找 (e.g. 用户连接到多个服务器) */
		private SessionLookup sessionLookup;

		/**
		 * 用于JSON反序列化.
		 */
		@SuppressWarnings("unused")
		public TransferSimpUser() {
			this.sessions = new HashSet<TransferSimpSession>(1);
		}

		/**
		 * 用于从本地用户创建DTO.
		 */
		public TransferSimpUser(SimpUser user) {
			this.name = user.getName();
			Set<SimpSession> sessions = user.getSessions();
			this.sessions = new HashSet<TransferSimpSession>(sessions.size());
			for (SimpSession session : sessions) {
				this.sessions.add(new TransferSimpSession(session));
			}
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean hasSessions() {
			if (this.sessionLookup != null) {
				return !this.sessionLookup.findSessions(getName()).isEmpty();
			}
			return !this.sessions.isEmpty();
		}

		@Override
		public SimpSession getSession(String sessionId) {
			if (this.sessionLookup != null) {
				return this.sessionLookup.findSessions(getName()).get(sessionId);
			}
			for (TransferSimpSession session : this.sessions) {
				if (session.getId().equals(sessionId)) {
					return session;
				}
			}
			return null;
		}

		public void setSessions(Set<TransferSimpSession> sessions) {
			this.sessions.addAll(sessions);
		}

		@Override
		public Set<SimpSession> getSessions() {
			if (this.sessionLookup != null) {
				Map<String, SimpSession> sessions = this.sessionLookup.findSessions(getName());
				return new HashSet<SimpSession>(sessions.values());
			}
			return new HashSet<SimpSession>(this.sessions);
		}

		private void afterDeserialization(SessionLookup sessionLookup) {
			this.sessionLookup = sessionLookup;
			for (TransferSimpSession session : this.sessions) {
				session.setUser(this);
				session.afterDeserialization();
			}
		}

		private void addSessions(Map<String, SimpSession> map) {
			for (SimpSession session : this.sessions) {
				map.put(session.getId(), session);
			}
		}


		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof SimpUser && this.name.equals(((SimpUser) other).getName())));
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return "name=" + this.name + ", sessions=" + this.sessions;
		}
	}


	/**
	 * 可以(反)序列化并广播到其他服务器.
	 */
	private static class TransferSimpSession implements SimpSession {

		private String id;

		private TransferSimpUser user;

		private final Set<TransferSimpSubscription> subscriptions;

		/**
		 * 用于JSON反序列化.
		 */
		@SuppressWarnings("unused")
		public TransferSimpSession() {
			this.subscriptions = new HashSet<TransferSimpSubscription>(4);
		}

		/**
		 * 用于从本地用户会话创建DTO.
		 */
		public TransferSimpSession(SimpSession session) {
			this.id = session.getId();
			Set<SimpSubscription> subscriptions = session.getSubscriptions();
			this.subscriptions = new HashSet<TransferSimpSubscription>(subscriptions.size());
			for (SimpSubscription subscription : subscriptions) {
				this.subscriptions.add(new TransferSimpSubscription(subscription));
			}
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		public void setUser(TransferSimpUser user) {
			this.user = user;
		}

		@Override
		public TransferSimpUser getUser() {
			return this.user;
		}

		public void setSubscriptions(Set<TransferSimpSubscription> subscriptions) {
			this.subscriptions.addAll(subscriptions);
		}

		@Override
		public Set<SimpSubscription> getSubscriptions() {
			return new HashSet<SimpSubscription>(this.subscriptions);
		}

		private void afterDeserialization() {
			for (TransferSimpSubscription subscription : this.subscriptions) {
				subscription.setSession(this);
			}
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof SimpSession && this.id.equals(((SimpSession) other).getId())));
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public String toString() {
			return "id=" + this.id + ", subscriptions=" + this.subscriptions;
		}
	}


	/**
	 * 可以(反)序列化并广播到其他服务器.
	 */
	private static class TransferSimpSubscription implements SimpSubscription {

		private String id;

		private TransferSimpSession session;

		private String destination;

		/**
		 * 用于JSON反序列化.
		 */
		@SuppressWarnings("unused")
		public TransferSimpSubscription() {
		}

		/**
		 * 用于从本地用户订阅创建DTO.
		 */
		public TransferSimpSubscription(SimpSubscription subscription) {
			this.id = subscription.getId();
			this.destination = subscription.getDestination();
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return this.id;
		}

		public void setSession(TransferSimpSession session) {
			this.session = session;
		}

		@Override
		public TransferSimpSession getSession() {
			return this.session;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		@Override
		public String getDestination() {
			return this.destination;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof SimpSubscription)) {
				return false;
			}
			SimpSubscription otherSubscription = (SimpSubscription) other;
			return (ObjectUtils.nullSafeEquals(getSession(), otherSubscription.getSession()) &&
					this.id.equals(otherSubscription.getId()));
		}

		@Override
		public int hashCode() {
			return this.id.hashCode() * 31 + ObjectUtils.nullSafeHashCode(getSession());
		}

		@Override
		public String toString() {
			return "destination=" + this.destination;
		}
	}


	/**
	 * 用于查找所有服务器上的用户会话.
	 */
	private class SessionLookup {

		public Map<String, SimpSession> findSessions(String userName) {
			Map<String, SimpSession> map = new HashMap<String, SimpSession>(1);
			SimpUser user = localRegistry.getUser(userName);
			if (user != null) {
				for (SimpSession session : user.getSessions()) {
					map.put(session.getId(), session);
				}
			}
			for (UserRegistrySnapshot registry : remoteRegistries.values()) {
				TransferSimpUser transferUser = registry.getUserMap().get(userName);
				if (transferUser != null) {
					transferUser.addSessions(map);
				}
			}
			return map;
		}

	}

}
