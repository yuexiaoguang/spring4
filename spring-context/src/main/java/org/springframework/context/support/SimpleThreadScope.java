package org.springframework.context.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.NamedThreadLocal;

/**
 * 一个简单的线程支持的{@link Scope}实现.
 *
 * <p><b>NOTE:</b> 默认情况下, 此线程作用域未在常见上下文中注册.
 * 相反, 需要将其明确地分配给设置中的作用域Key,
 * 无论是通过{@link org.springframework.beans.factory.config.ConfigurableBeanFactory#registerScope},
 * 还是通过{@link org.springframework.beans.factory.config.CustomScopeConfigurer} bean.
 *
 * <p>{@code SimpleThreadScope} <em>不会清除与之关联的任何对象</em>.
 * 因此, 通常最好在Web环境中使用 {@link org.springframework.web.context.request.RequestScope RequestScope}.
 *
 * <p>对于基于线程的{@code Scope}的实现, 支持销毁回调, 参考
 * <a href="http://www.springbyexample.org/examples/custom-thread-scope-module.html"> Spring by Example Custom Thread Scope Module</a>.
 *
 * <p>感谢Eugene Kuleshov为线程作用域提交原始原型!
 */
public class SimpleThreadScope implements Scope {

	private static final Log logger = LogFactory.getLog(SimpleThreadScope.class);

	private final ThreadLocal<Map<String, Object>> threadScope =
			new NamedThreadLocal<Map<String, Object>>("SimpleThreadScope") {
				@Override
				protected Map<String, Object> initialValue() {
					return new HashMap<String, Object>();
				}
			};


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Map<String, Object> scope = this.threadScope.get();
		Object scopedObject = scope.get(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			scope.put(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	public Object remove(String name) {
		Map<String, Object> scope = this.threadScope.get();
		return scope.remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		logger.warn("SimpleThreadScope does not support destruction callbacks. " +
				"Consider using RequestScope in a web environment.");
	}

	@Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return Thread.currentThread().getName();
	}

}
