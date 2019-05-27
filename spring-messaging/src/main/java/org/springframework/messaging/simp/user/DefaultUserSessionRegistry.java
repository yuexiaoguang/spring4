package org.springframework.messaging.simp.user;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.util.Assert;

/**
 * {@link UserSessionRegistry}的默认线程安全实现.
 *
 * @deprecated as of 4.2 this class is no longer used, see deprecation notes
 * on {@link UserSessionRegistry} for more details.
 */
@Deprecated
@SuppressWarnings({"deprecation", "unused"})
public class DefaultUserSessionRegistry implements UserSessionRegistry {

	// userId -> sessionId
	private final ConcurrentMap<String, Set<String>> userSessionIds = new ConcurrentHashMap<String, Set<String>>();

	private final Object lock = new Object();


	@Override
	public Set<String> getSessionIds(String user) {
		Set<String> set = this.userSessionIds.get(user);
		return (set != null ? set : Collections.<String>emptySet());
	}

	@Override
	public void registerSessionId(String user, String sessionId) {
		Assert.notNull(user, "User must not be null");
		Assert.notNull(sessionId, "Session ID must not be null");
		synchronized (this.lock) {
			Set<String> set = this.userSessionIds.get(user);
			if (set == null) {
				set = new CopyOnWriteArraySet<String>();
				this.userSessionIds.put(user, set);
			}
			set.add(sessionId);
		}
	}

	@Override
	public void unregisterSessionId(String userName, String sessionId) {
		Assert.notNull(userName, "User Name must not be null");
		Assert.notNull(sessionId, "Session ID must not be null");
		synchronized (lock) {
			Set<String> set = this.userSessionIds.get(userName);
			if (set != null) {
				if (set.remove(sessionId) && set.isEmpty()) {
					this.userSessionIds.remove(userName);
				}
			}
		}
	}

}