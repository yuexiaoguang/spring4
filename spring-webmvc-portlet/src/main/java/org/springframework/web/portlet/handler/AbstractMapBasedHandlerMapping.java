package org.springframework.web.portlet.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.portlet.HandlerMapping}实现的抽象基类, 它依赖于每个查找键缓存处理器对象的映射.
 * 支持任意查找键, 并自动将处理器bean名称解析为处理器bean实例.
 */
public abstract class AbstractMapBasedHandlerMapping<K> extends AbstractHandlerMapping {

	private boolean lazyInitHandlers = false;

	private final Map<K, Object> handlerMap = new HashMap<K, Object>();


	/**
	 * 设置是否延迟初始化处理器.
	 * 仅适用于单例处理器, 因为原型总是被延迟地初始化.
	 * 默认为false, 因为实时初始化通过直接引用处理器对象可以提高效率.
	 * <p>如果需要处理器被延迟地初始化, 将它们设置为"lazy-init"并将此标志设置为true.
	 * 只是设置为"lazy-init"是无效的, 因为在这种情况下它们是通过处理器映射的引用初始化的.
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}


	/**
	 * 确定给定请求的计算查找键的处理器.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object getHandlerInternal(PortletRequest request) throws Exception {
		K lookupKey = getLookupKey(request);
		Object handler = this.handlerMap.get(lookupKey);
		if (handler != null && logger.isDebugEnabled()) {
			logger.debug("Key [" + lookupKey + "] -> handler [" + handler + "]");
		}
		if (handler instanceof Map) {
			Map<PortletRequestMappingPredicate, Object> predicateMap =
					(Map<PortletRequestMappingPredicate, Object>) handler;
			List<PortletRequestMappingPredicate> filtered = new LinkedList<PortletRequestMappingPredicate>();
			for (PortletRequestMappingPredicate predicate : predicateMap.keySet()) {
				if (predicate.match(request)) {
					filtered.add(predicate);
				}
			}
			if (filtered.isEmpty()) {
				return null;
			}
			Collections.sort(filtered);
			PortletRequestMappingPredicate predicate = filtered.get(0);
			predicate.validate(request);
			return predicateMap.get(predicate);
		}
		return handler;
	}

	/**
	 * 为给定的请求构建查找键.
	 * 
	 * @param request 当前的portlet请求
	 * 
	 * @return 查找键 (never {@code null})
	 * @throws Exception 如果键计算失败
	 */
	protected abstract K getLookupKey(PortletRequest request) throws Exception;


	/**
	 * 注册Portlet模式映射中为相应模式指定的所有处理器.
	 * 
	 * @param handlerMap 将查找键作为键, 将处理器bean或bean名称作为值
	 * 
	 * @throws BeansException 如果无法注册处理器
	 */
	protected void registerHandlers(Map<K, ?> handlerMap) throws BeansException {
		Assert.notNull(handlerMap, "Handler Map must not be null");
		for (Map.Entry<K, ?> entry : handlerMap.entrySet()) {
			registerHandler(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 为给定的参数值注册给定的处理器实例.
	 * 
	 * @param lookupKey 将处理器映射到的键
	 * @param handler 处理器实例或处理器bean名称
	 * (bean名称将自动解析为相应的处理器bean)
	 * 
	 * @throws BeansException 如果无法注册处理器
	 * @throws IllegalStateException 如果注册处理器存在冲突
	 */
	protected void registerHandler(K lookupKey, Object handler) throws BeansException, IllegalStateException {
		registerHandler(lookupKey, handler, null);
	}

	/**
	 * 为给定的参数值注册给定的处理器实例.
	 * 
	 * @param lookupKey 将处理器映射到的键
	 * @param handler 处理器实例或处理器bean名称
	 * (bean名称将自动解析为相应的处理器bean)
	 * @param predicate 此处理器的谓词对象 (可能是{@code null}), 确定与主查找键的匹配
	 * 
	 * @throws BeansException 如果处理器无法注册
	 * @throws IllegalStateException 如果注册的处理器存在冲突
	 */
	@SuppressWarnings("unchecked")
	protected void registerHandler(K lookupKey, Object handler, PortletRequestMappingPredicate predicate)
			throws BeansException, IllegalStateException {

		Assert.notNull(lookupKey, "Lookup key must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// 如果通过名称引用singleton, 则实时地解析处理器.
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			if (getApplicationContext().isSingleton(handlerName)) {
				resolvedHandler = getApplicationContext().getBean(handlerName);
			}
		}

		// 检查重复的映射.
		Object mappedHandler = this.handlerMap.get(lookupKey);
		if (mappedHandler != null && !(mappedHandler instanceof Map)) {
			if (mappedHandler != resolvedHandler) {
				throw new IllegalStateException("Cannot map handler [" + handler + "] to key [" + lookupKey +
						"]: There's already handler [" + mappedHandler + "] mapped.");
			}
		}
		else {
			if (predicate != null) {
				// Add the handler to the predicate map.
				Map<PortletRequestMappingPredicate, Object> predicateMap =
						(Map<PortletRequestMappingPredicate, Object>) mappedHandler;
				if (predicateMap == null) {
					predicateMap = new LinkedHashMap<PortletRequestMappingPredicate, Object>();
					this.handlerMap.put(lookupKey, predicateMap);
				}
				predicateMap.put(predicate, resolvedHandler);
			}
			else {
				// Add the single handler to the map.
				this.handlerMap.put(lookupKey, resolvedHandler);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Mapped key [" + lookupKey + "] onto handler [" + resolvedHandler + "]");
			}
		}
	}


	/**
	 * 谓词接口, 用于确定与给定请求的匹配.
	 */
	protected interface PortletRequestMappingPredicate extends Comparable<PortletRequestMappingPredicate> {

		/**
		 * 确定给定的请求是否与此谓词匹配.
		 * 
		 * @param request 当前的portlet请求
		 */
		boolean match(PortletRequest request);

		/**
		 * 根据当前请求验证此谓词的映射.
		 * 
		 * @param request 当前的portlet请求
		 * 
		 * @throws PortletException 如果验证失败
		 */
		void validate(PortletRequest request) throws PortletException;
	}

}
