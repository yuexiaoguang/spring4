package org.springframework.mock.jndi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.NamingException;

import org.springframework.jndi.JndiTemplate;

/**
 * JndiTemplate类的简单扩展, 它总是返回给定的对象.
 *
 * <p>对测试非常有用. 实际上是一个模拟对象.
 */
public class ExpectedLookupTemplate extends JndiTemplate {

	private final Map<String, Object> jndiObjects = new ConcurrentHashMap<String, Object>(16);


	/**
	 * 构造一个新的JndiTemplate, 它总是返回给定名称的给定对象.
	 * 要通过{@code addObject}调用填充.
	 */
	public ExpectedLookupTemplate() {
	}

	/**
	 * 构造一个始终返回给定对象的新JndiTemplate, 但仅承认给定名称的请求.
	 * 
	 * @param name 客户端应该查找的名称
	 * @param object 将返回的对象
	 */
	public ExpectedLookupTemplate(String name, Object object) {
		addObject(name, object);
	}

	/**
	 * 将给定对象添加到此模板将公开的JNDI对象列表中.
	 * 
	 * @param name 客户端应该查找的名称
	 * @param object 将返回的对象
	 */
	public void addObject(String name, Object object) {
		this.jndiObjects.put(name, object);
	}

	/**
	 * 如果名称是构造函数中指定的预期名称, 则返回构造函数中提供的对象.
	 * 如果名称是意外的, 则会抛出相应的NamingException.
	 */
	@Override
	public Object lookup(String name) throws NamingException {
		Object object = this.jndiObjects.get(name);
		if (object == null) {
			throw new NamingException("Unexpected JNDI name '" + name + "': expecting " + this.jndiObjects.keySet());
		}
		return object;
	}

}
