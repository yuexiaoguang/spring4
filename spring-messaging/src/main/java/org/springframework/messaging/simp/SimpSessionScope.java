package org.springframework.messaging.simp;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * 公开SiMP会话属性的{@link Scope}实现 (e.g. WebSocket会话).
 *
 * <p>依赖{@link org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler}
 * 导出的线程绑定{@link SimpAttributes}实例.
 */
public class SimpSessionScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		SimpAttributes simpAttributes = SimpAttributesContextHolder.currentAttributes();
		Object scopedObject = simpAttributes.getAttribute(name);
		if (scopedObject != null) {
			return scopedObject;
		}
		synchronized (simpAttributes.getSessionMutex()) {
			scopedObject = simpAttributes.getAttribute(name);
			if (scopedObject == null) {
				scopedObject = objectFactory.getObject();
				simpAttributes.setAttribute(name, scopedObject);
			}
			return scopedObject;
		}
	}

	@Override
	public Object remove(String name) {
		SimpAttributes simpAttributes = SimpAttributesContextHolder.currentAttributes();
		synchronized (simpAttributes.getSessionMutex()) {
			Object value = simpAttributes.getAttribute(name);
			if (value != null) {
				simpAttributes.removeAttribute(name);
				return value;
			}
			else {
				return null;
			}
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		SimpAttributesContextHolder.currentAttributes().registerDestructionCallback(name, callback);
	}

	@Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return SimpAttributesContextHolder.currentAttributes().getSessionId();
	}

}
