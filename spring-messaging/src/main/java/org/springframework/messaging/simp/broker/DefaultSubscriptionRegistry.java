package org.springframework.messaging.simp.broker;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link SubscriptionRegistry}的实现, 它将订阅存储在内存中,
 * 并使用{@link org.springframework.util.PathMatcher PathMatcher}来匹配目标.
 *
 * <p>从4.2开始, 此类支持订阅消息上的{@link #setSelectorHeaderName selector} header,
 * 并根据header评估Spring EL表达式以过滤除目标匹配之外的消息.
 */
public class DefaultSubscriptionRegistry extends AbstractSubscriptionRegistry {

	/** 目标缓存的默认最大条目数: 1024 */
	public static final int DEFAULT_CACHE_LIMIT = 1024;

	/** 要重用的静态评估上下文 */
	private static EvaluationContext messageEvalContext =
			SimpleEvaluationContext.forPropertyAccessors(new SimpMessageHeaderPropertyAccessor()).build();


	private PathMatcher pathMatcher = new AntPathMatcher();

	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	private String selectorHeaderName = "selector";

	private volatile boolean selectorHeaderInUse = false;

	private final ExpressionParser expressionParser = new SpelExpressionParser();

	private final DestinationCache destinationCache = new DestinationCache();

	private final SessionSubscriptionRegistry subscriptionRegistry = new SessionSubscriptionRegistry();


	/**
	 * 指定要使用的{@link PathMatcher}.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回配置的{@link PathMatcher}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 指定已解析的目标缓存的最大条目数.
	 * 默认 1024.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * 返回已解析的目标缓存的最大条目数.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * 配置订阅消息可以具有的header名称, 以便过滤与订阅匹配的消息.
	 * header值应该是一个Spring EL布尔表达式, 应用于与订阅匹配的消息的header.
	 * <p>例如:
	 * <pre>
	 * headers.foo == 'bar'
	 * </pre>
	 * <p>默认设置为"selector". 可以将其设置为其他名称, 或{@code null}以关闭对选择器header的支持.
	 * 
	 * @param selectorHeaderName 用于选择器header的名称
	 */
	public void setSelectorHeaderName(String selectorHeaderName) {
		this.selectorHeaderName = StringUtils.hasText(selectorHeaderName) ? selectorHeaderName : null;
	}

	/**
	 * 返回选择器header名称的名称.
	 */
	public String getSelectorHeaderName() {
		return this.selectorHeaderName;
	}


	@Override
	protected void addSubscriptionInternal(
			String sessionId, String subsId, String destination, Message<?> message) {

		Expression expression = getSelectorExpression(message.getHeaders());
		this.subscriptionRegistry.addSubscription(sessionId, subsId, destination, expression);
		this.destinationCache.updateAfterNewSubscription(destination, sessionId, subsId);
	}

	private Expression getSelectorExpression(MessageHeaders headers) {
		Expression expression = null;
		if (getSelectorHeaderName() != null) {
			String selector = SimpMessageHeaderAccessor.getFirstNativeHeader(getSelectorHeaderName(), headers);
			if (selector != null) {
				try {
					expression = this.expressionParser.parseExpression(selector);
					this.selectorHeaderInUse = true;
					if (logger.isTraceEnabled()) {
						logger.trace("Subscription selector: [" + selector + "]");
					}
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to parse selector: " + selector, ex);
					}
				}
			}
		}
		return expression;
	}

	@Override
	protected void removeSubscriptionInternal(String sessionId, String subsId, Message<?> message) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
		if (info != null) {
			String destination = info.removeSubscription(subsId);
			if (destination != null) {
				this.destinationCache.updateAfterRemovedSubscription(sessionId, subsId);
			}
		}
	}

	@Override
	public void unregisterAllSubscriptions(String sessionId) {
		SessionSubscriptionInfo info = this.subscriptionRegistry.removeSubscriptions(sessionId);
		if (info != null) {
			this.destinationCache.updateAfterRemovedSession(info);
		}
	}

	@Override
	protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
		MultiValueMap<String, String> result = this.destinationCache.getSubscriptions(destination, message);
		return filterSubscriptions(result, message);
	}

	private MultiValueMap<String, String> filterSubscriptions(
			MultiValueMap<String, String> allMatches, Message<?> message) {

		if (!this.selectorHeaderInUse) {
			return allMatches;
		}
		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(allMatches.size());
		for (String sessionId : allMatches.keySet()) {
			for (String subId : allMatches.get(sessionId)) {
				SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
				if (info == null) {
					continue;
				}
				Subscription sub = info.getSubscription(subId);
				if (sub == null) {
					continue;
				}
				Expression expression = sub.getSelectorExpression();
				if (expression == null) {
					result.add(sessionId, subId);
					continue;
				}
				try {
					if (Boolean.TRUE.equals(expression.getValue(messageEvalContext, message, Boolean.class))) {
						result.add(sessionId, subId);
					}
				}
				catch (SpelEvaluationException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to evaluate selector: " + ex.getMessage());
					}
				}
				catch (Throwable ex) {
					logger.debug("Failed to evaluate selector", ex);
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "DefaultSubscriptionRegistry[" + this.destinationCache + ", " + this.subscriptionRegistry + "]";
	}


	/**
	 * 以前通过{@link DefaultSubscriptionRegistry#findSubscriptionsInternal(String, Message)}解析的目标缓存
	 */
	private class DestinationCache {

		/** Map from destination -> <sessionId, subscriptionId> for fast look-ups */
		private final Map<String, LinkedMultiValueMap<String, String>> accessCache =
				new ConcurrentHashMap<String, LinkedMultiValueMap<String, String>>(DEFAULT_CACHE_LIMIT);

		/** Map from destination -> <sessionId, subscriptionId> with locking */
		@SuppressWarnings("serial")
		private final Map<String, LinkedMultiValueMap<String, String>> updateCache =
				new LinkedHashMap<String, LinkedMultiValueMap<String, String>>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
					@Override
					protected boolean removeEldestEntry(Map.Entry<String, LinkedMultiValueMap<String, String>> eldest) {
						if (size() > getCacheLimit()) {
							accessCache.remove(eldest.getKey());
							return true;
						}
						else {
							return false;
						}
					}
				};


		public LinkedMultiValueMap<String, String> getSubscriptions(String destination, Message<?> message) {
			LinkedMultiValueMap<String, String> result = this.accessCache.get(destination);
			if (result == null) {
				synchronized (this.updateCache) {
					result = new LinkedMultiValueMap<String, String>();
					for (SessionSubscriptionInfo info : subscriptionRegistry.getAllSubscriptions()) {
						for (String destinationPattern : info.getDestinations()) {
							if (getPathMatcher().match(destinationPattern, destination)) {
								for (Subscription subscription : info.getSubscriptions(destinationPattern)) {
									result.add(info.sessionId, subscription.getId());
								}
							}
						}
					}
					if (!result.isEmpty()) {
						this.updateCache.put(destination, result.deepCopy());
						this.accessCache.put(destination, result);
					}
				}
			}
			return result;
		}

		public void updateAfterNewSubscription(String destination, String sessionId, String subsId) {
			synchronized (this.updateCache) {
				for (Map.Entry<String, LinkedMultiValueMap<String, String>> entry : this.updateCache.entrySet()) {
					String cachedDestination = entry.getKey();
					if (getPathMatcher().match(destination, cachedDestination)) {
						LinkedMultiValueMap<String, String> subs = entry.getValue();
						// 订阅ID也可以通过 getSubscriptions()填充
						List<String> subsForSession = subs.get(sessionId);
						if (subsForSession == null || !subsForSession.contains(subsId)) {
							subs.add(sessionId, subsId);
							this.accessCache.put(cachedDestination, subs.deepCopy());
						}
					}
				}
			}
		}

		public void updateAfterRemovedSubscription(String sessionId, String subsId) {
			synchronized (this.updateCache) {
				Set<String> destinationsToRemove = new HashSet<String>();
				for (Map.Entry<String, LinkedMultiValueMap<String, String>> entry : this.updateCache.entrySet()) {
					String destination = entry.getKey();
					LinkedMultiValueMap<String, String> sessionMap = entry.getValue();
					List<String> subscriptions = sessionMap.get(sessionId);
					if (subscriptions != null) {
						subscriptions.remove(subsId);
						if (subscriptions.isEmpty()) {
							sessionMap.remove(sessionId);
						}
						if (sessionMap.isEmpty()) {
							destinationsToRemove.add(destination);
						}
						else {
							this.accessCache.put(destination, sessionMap.deepCopy());
						}
					}
				}
				for (String destination : destinationsToRemove) {
					this.updateCache.remove(destination);
					this.accessCache.remove(destination);
				}
			}
		}

		public void updateAfterRemovedSession(SessionSubscriptionInfo info) {
			synchronized (this.updateCache) {
				Set<String> destinationsToRemove = new HashSet<String>();
				for (Map.Entry<String, LinkedMultiValueMap<String, String>> entry : this.updateCache.entrySet()) {
					String destination = entry.getKey();
					LinkedMultiValueMap<String, String> sessionMap = entry.getValue();
					if (sessionMap.remove(info.getSessionId()) != null) {
						if (sessionMap.isEmpty()) {
							destinationsToRemove.add(destination);
						}
						else {
							this.accessCache.put(destination, sessionMap.deepCopy());
						}
					}
				}
				for (String destination : destinationsToRemove) {
					this.updateCache.remove(destination);
					this.accessCache.remove(destination);
				}
			}
		}

		@Override
		public String toString() {
			return "cache[" + this.accessCache.size() + " destination(s)]";
		}
	}


	/**
	 * 通过sessionId提供对会话订阅的访问.
	 */
	private static class SessionSubscriptionRegistry {

		// sessionId -> SessionSubscriptionInfo
		private final ConcurrentMap<String, SessionSubscriptionInfo> sessions =
				new ConcurrentHashMap<String, SessionSubscriptionInfo>();

		public SessionSubscriptionInfo getSubscriptions(String sessionId) {
			return this.sessions.get(sessionId);
		}

		public Collection<SessionSubscriptionInfo> getAllSubscriptions() {
			return this.sessions.values();
		}

		public SessionSubscriptionInfo addSubscription(String sessionId, String subscriptionId,
				String destination, Expression selectorExpression) {

			SessionSubscriptionInfo info = this.sessions.get(sessionId);
			if (info == null) {
				info = new SessionSubscriptionInfo(sessionId);
				SessionSubscriptionInfo value = this.sessions.putIfAbsent(sessionId, info);
				if (value != null) {
					info = value;
				}
			}
			info.addSubscription(destination, subscriptionId, selectorExpression);
			return info;
		}

		public SessionSubscriptionInfo removeSubscriptions(String sessionId) {
			return this.sessions.remove(sessionId);
		}

		@Override
		public String toString() {
			return "registry[" + this.sessions.size() + " sessions]";
		}
	}


	/**
	 * 保留会话的订阅.
	 */
	private static class SessionSubscriptionInfo {

		private final String sessionId;

		// destination -> subscriptions
		private final Map<String, Set<Subscription>> destinationLookup =
				new ConcurrentHashMap<String, Set<Subscription>>(4);

		public SessionSubscriptionInfo(String sessionId) {
			Assert.notNull(sessionId, "'sessionId' must not be null");
			this.sessionId = sessionId;
		}

		public String getSessionId() {
			return this.sessionId;
		}

		public Set<String> getDestinations() {
			return this.destinationLookup.keySet();
		}

		public Set<Subscription> getSubscriptions(String destination) {
			return this.destinationLookup.get(destination);
		}

		public Subscription getSubscription(String subscriptionId) {
			for (Map.Entry<String, Set<DefaultSubscriptionRegistry.Subscription>> destinationEntry : this.destinationLookup.entrySet()) {
				Set<Subscription> subs = destinationEntry.getValue();
				if (subs != null) {
					for (Subscription sub : subs) {
						if (sub.getId().equalsIgnoreCase(subscriptionId)) {
							return sub;
						}
					}
				}
			}
			return null;
		}

		public void addSubscription(String destination, String subscriptionId, Expression selectorExpression) {
			Set<Subscription> subs = this.destinationLookup.get(destination);
			if (subs == null) {
				synchronized (this.destinationLookup) {
					subs = this.destinationLookup.get(destination);
					if (subs == null) {
						subs = new CopyOnWriteArraySet<Subscription>();
						this.destinationLookup.put(destination, subs);
					}
				}
			}
			subs.add(new Subscription(subscriptionId, selectorExpression));
		}

		public String removeSubscription(String subscriptionId) {
			for (Map.Entry<String, Set<DefaultSubscriptionRegistry.Subscription>> destinationEntry : this.destinationLookup.entrySet()) {
				Set<Subscription> subs = destinationEntry.getValue();
				if (subs != null) {
					for (Subscription sub : subs) {
						if (sub.getId().equals(subscriptionId) && subs.remove(sub)) {
							synchronized (this.destinationLookup) {
								if (subs.isEmpty()) {
									this.destinationLookup.remove(destinationEntry.getKey());
								}
							}
							return destinationEntry.getKey();
						}
					}
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return "[sessionId=" + this.sessionId + ", subscriptions=" + this.destinationLookup + "]";
		}
	}


	private static final class Subscription {

		private final String id;

		private final Expression selectorExpression;

		public Subscription(String id, Expression selector) {
			Assert.notNull(id, "Subscription id must not be null");
			this.id = id;
			this.selectorExpression = selector;
		}

		public String getId() {
			return this.id;
		}

		public Expression getSelectorExpression() {
			return this.selectorExpression;
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof Subscription && this.id.equals(((Subscription) other).id)));
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public String toString() {
			return "subscription(id=" + this.id + ")";
		}
	}


	private static class SimpMessageHeaderPropertyAccessor implements PropertyAccessor {

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Message.class, MessageHeaders.class};
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return true;
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			Object value;
			if (target instanceof Message) {
				value = name.equals("headers") ? ((Message) target).getHeaders() : null;
			}
			else if (target instanceof MessageHeaders) {
				MessageHeaders headers = (MessageHeaders) target;
				SimpMessageHeaderAccessor accessor =
						MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
				Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
				if ("destination".equalsIgnoreCase(name)) {
					value = accessor.getDestination();
				}
				else {
					value = accessor.getFirstNativeHeader(name);
					if (value == null) {
						value = headers.get(name);
					}
				}
			}
			else {
				// Should never happen...
				throw new IllegalStateException("Expected Message or MessageHeaders.");
			}
			return new TypedValue(value);
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object value) {
		}
	}

}
