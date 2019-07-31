package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * 抽象{@link Scope}实现, 它从当前线程绑定的{@link RequestAttributes}对象中的特定范围读取.
 *
 * <p>子类只需要实现{@link #getScope()}来指示从中读取属性的此类的{@link RequestAttributes}范围.
 *
 * <p>子类可能希望覆盖{@link #get}和{@link #remove}方法, 以便将回调周围的同步添加到此超类中.
 */
public abstract class AbstractRequestAttributesScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		Object scopedObject = attributes.getAttribute(name, getScope());
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			attributes.setAttribute(name, scopedObject, getScope());
			// 再次检索对象, 将其注册为隐式会话属性更新.
			// 作为奖励, 还允许在getAttribute级别进行潜在装饰.
			Object retrievedObject = attributes.getAttribute(name, getScope());
			if (retrievedObject != null) {
				// 如果仍然存在, 则仅继续检索到的对象 (预期的情况).
				// 如果它同时消失, 返回本地创建的实例.
				scopedObject = retrievedObject;
			}
		}
		return scopedObject;
	}

	@Override
	public Object remove(String name) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		Object scopedObject = attributes.getAttribute(name, getScope());
		if (scopedObject != null) {
			attributes.removeAttribute(name, getScope());
			return scopedObject;
		}
		else {
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		attributes.registerDestructionCallback(name, callback, getScope());
	}

	@Override
	public Object resolveContextualObject(String key) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		return attributes.resolveReference(key);
	}


	/**
	 * 用于确定实际目标范围的模板方法.
	 * 
	 * @return 目标范围, 以适当的{@link RequestAttributes}常量的形式
	 */
	protected abstract int getScope();

}
