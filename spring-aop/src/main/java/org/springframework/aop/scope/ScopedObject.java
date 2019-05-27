package org.springframework.aop.scope;

import org.springframework.aop.RawTargetAccess;

/**
 * 用于作用域对象的AOP引入接口.
 *
 * <p>从{@link ScopedProxyFactoryBean}创建的对象可以强制转换为此接口, 允许访问原始目标对象并以编程方式删除目标对象.
 */
public interface ScopedObject extends RawTargetAccess {

	/**
	 * 返回此作用域对象代理后面的当前目标对象, 以他原生的格式 (存储在目标作用域内).
	 * <p>例如，原始目标对象可以传递给无法处理作用域代理对象的持久性提供者.
	 * 
	 * @return 此作用域对象代理后面的当前目标对象
	 */
	Object getTargetObject();

	/**
	 * 从目标作用域中删除此对象, 例如来自后端的会话.
	 * <p>请注意，之后不能再对作用域对象进行调用 (至少在当前线程内，即目标作用域中完全相同的目标对象).
	 */
	void removeFromScope();

}
