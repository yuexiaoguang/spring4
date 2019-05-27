package org.springframework.jms.support.destination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.NamingException;

import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.util.Assert;

/**
 * {@link DestinationResolver}实现, 它将目标名称解释为JNDI位置 (具有可配置的回退策略).
 *
 * <p>允许在必要时自定义JNDI环境, 例如指定适当的JNDI环境属性.
 *
 * <p>动态队列和主题按目标名称缓存.
 * 因此, 需要在队列和主题中使用唯一的目标名称.
 * 可以通过{@link #setCache "cache"}标志关闭缓存.
 *
 * <p>请注意, 默认情况下, 动态目标的解析回退<i>关闭</i>.
 * 切换{@link #setFallbackToDynamicDestination "fallbackToDynamicDestination"}标志以启用此功能.
 */
public class JndiDestinationResolver extends JndiLocatorSupport implements CachingDestinationResolver {

	private boolean cache = true;

	private boolean fallbackToDynamicDestination = false;

	private DestinationResolver dynamicDestinationResolver = new DynamicDestinationResolver();

	private final Map<String, Destination> destinationCache = new ConcurrentHashMap<String, Destination>(16);


	/**
	 * 设置是否缓存已解析的目标. 默认"true".
	 * <p>可以关闭此标志以重新查找每个操作的目标, 从而允许热重新启动目标.
	 * 这在开发过程中非常有用.
	 * <p>请注意, 动态队列和主题将按目标名称进行缓存.
	 * 因此, 需要在队列和主题中使用唯一的目标名称.
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * 设置如果在JNDI中找不到目标名称, 是否应该创建动态目标. 默认"false".
	 * <p>打开此标志以启用透明回退到动态目标.
	 */
	public void setFallbackToDynamicDestination(boolean fallbackToDynamicDestination) {
		this.fallbackToDynamicDestination = fallbackToDynamicDestination;
	}

	/**
	 * 设置要在回退到动态目标时使用的{@link DestinationResolver}.
	 * <p>默认值是Spring的标准{@link DynamicDestinationResolver}.
	 */
	public void setDynamicDestinationResolver(DestinationResolver dynamicDestinationResolver) {
		this.dynamicDestinationResolver = dynamicDestinationResolver;
	}


	@Override
	public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.notNull(destinationName, "Destination name must not be null");
		Destination dest = this.destinationCache.get(destinationName);
		if (dest != null) {
			validateDestination(dest, destinationName, pubSubDomain);
		}
		else {
			try {
				dest = lookup(destinationName, Destination.class);
				validateDestination(dest, destinationName, pubSubDomain);
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Destination [" + destinationName + "] not found in JNDI", ex);
				}
				if (this.fallbackToDynamicDestination) {
					dest = this.dynamicDestinationResolver.resolveDestinationName(session, destinationName, pubSubDomain);
				}
				else {
					throw new DestinationResolutionException(
							"Destination [" + destinationName + "] not found in JNDI", ex);
				}
			}
			if (this.cache) {
				this.destinationCache.put(destinationName, dest);
			}
		}
		return dest;
	}

	/**
	 * 验证给定的Destination对象, 检查它是否与预期的类型匹配.
	 * 
	 * @param destination 要验证的Destination对象
	 * @param destinationName 目标名称
	 * @param pubSubDomain {@code true}如果期望Topic, {@code false}如果是Queue
	 */
	protected void validateDestination(Destination destination, String destinationName, boolean pubSubDomain) {
		Class<?> targetClass = Queue.class;
		if (pubSubDomain) {
			targetClass = Topic.class;
		}
		if (!targetClass.isInstance(destination)) {
			throw new DestinationResolutionException(
					"Destination [" + destinationName + "] is not of expected type [" + targetClass.getName() + "]");
		}
	}


	@Override
	public void removeFromCache(String destinationName) {
		this.destinationCache.remove(destinationName);
	}

	@Override
	public void clearCache() {
		this.destinationCache.clear();
	}

}
