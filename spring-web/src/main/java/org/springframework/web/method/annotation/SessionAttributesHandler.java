package org.springframework.web.method.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * 管理通过{@link SessionAttributes @SessionAttributes}声明的特定于控制器的会话属性.
 * 实际存储委托给{@link SessionAttributeStore}实例.
 *
 * <p>当使用{@code @SessionAttributes}注解的控制器向其模型添加属性时,
 * 将根据通过{@code @SessionAttributes}指定的名称和类型检查这些属性.
 * 匹配的模型属性保存在HTTP会话中并保持不变, 直到控制器调用{@link SessionStatus#setComplete()}.
 */
public class SessionAttributesHandler {

	private final Set<String> attributeNames = new HashSet<String>();

	private final Set<Class<?>> attributeTypes = new HashSet<Class<?>>();

	private final Set<String> knownAttributeNames =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(4));

	private final SessionAttributeStore sessionAttributeStore;


	/**
	 * 会话属性名称和类型是从{@code @SessionAttributes}注解中提取的, 如果存在, 则在给定类型上.
	 * 
	 * @param handlerType 控制器类型
	 * @param sessionAttributeStore 用于会话访问
	 */
	public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore may not be null");
		this.sessionAttributeStore = sessionAttributeStore;

		SessionAttributes ann = AnnotatedElementUtils.findMergedAnnotation(handlerType, SessionAttributes.class);
		if (ann != null) {
			this.attributeNames.addAll(Arrays.asList(ann.names()));
			this.attributeTypes.addAll(Arrays.asList(ann.types()));
		}
		this.knownAttributeNames.addAll(this.attributeNames);
	}


	/**
	 * 此实例表示的控制器是否通过{@link SessionAttributes}注解声明了任何会话属性.
	 */
	public boolean hasSessionAttributes() {
		return (!this.attributeNames.isEmpty() || !this.attributeTypes.isEmpty());
	}

	/**
	 * 属性名称或类型是否与底层控制器上的{@code @SessionAttributes}指定的名称和类型相匹配.
	 * <p>通过此方法成功解析的属性被"记住",
	 * 随后在{@link #retrieveAttributes(WebRequest)}和{@link #cleanupAttributes(WebRequest)}中使用.
	 * 
	 * @param attributeName 要检查的属性名称
	 * @param attributeType 属性的类型
	 */
	public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
		Assert.notNull(attributeName, "Attribute name must not be null");
		if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
			this.knownAttributeNames.add(attributeName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 在会话中存储给定属性的子集.
	 * 未通过{@code @SessionAttributes}声明为会话属性的属性将被忽略.
	 * 
	 * @param request 当前的请求
	 * @param attributes 会话存储的候选属性
	 */
	public void storeAttributes(WebRequest request, Map<String, ?> attributes) {
		for (String name : attributes.keySet()) {
			Object value = attributes.get(name);
			Class<?> attrType = (value != null ? value.getClass() : null);
			if (isHandlerSessionAttribute(name, attrType)) {
				this.sessionAttributeStore.storeAttribute(request, name, value);
			}
		}
	}

	/**
	 * 从会话中检索"已知"属性, i.e. {@code @SessionAttributes}中按名称列出的属性, 或先前存储在模型中的类型匹配的属性.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 具有处理器会话属性的Map, 可能为空
	 */
	public Map<String, Object> retrieveAttributes(WebRequest request) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		for (String name : this.knownAttributeNames) {
			Object value = this.sessionAttributeStore.retrieveAttribute(request, name);
			if (value != null) {
				attributes.put(name, value);
			}
		}
		return attributes;
	}

	/**
	 * 从会话中删除"已知"属性, i.e. {@code @SessionAttributes}中按名称列出的属性, 或先前存储在模型中按类型匹配的属性.
	 * 
	 * @param request 当前的请求
	 */
	public void cleanupAttributes(WebRequest request) {
		for (String attributeName : this.knownAttributeNames) {
			this.sessionAttributeStore.cleanupAttribute(request, attributeName);
		}
	}

	/**
	 * 对底层{@link SessionAttributeStore}的传递调用.
	 * 
	 * @param request 当前的请求
	 * @param attributeName 感兴趣的属性的名称
	 * 
	 * @return 属性值, 或{@code null}
	 */
	Object retrieveAttribute(WebRequest request, String attributeName) {
		return this.sessionAttributeStore.retrieveAttribute(request, attributeName);
	}

}
