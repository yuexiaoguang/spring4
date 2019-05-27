package org.springframework.beans.factory.access;

import org.springframework.beans.factory.BeanFactory;

/**
 * 用于跟踪对通过{@link BeanFactoryLocator}获得的{@link BeanFactory}的引用.
 *
 * <p>多次调用{@link #release()}是安全的, 但是在调用release之后, 不能调用{@link #getFactory()}.
 */
public interface BeanFactoryReference {

	/**
	 * 返回此引用持有的{@link BeanFactory}实例.
	 * 
	 * @throws IllegalStateException 如果在调用{@code release()}后调用
	 */
	BeanFactory getFactory();

	/**
	 * 获取{@link BeanFactoryReference}的客户端代码不再需要此{@link BeanFactory}实例.
	 * <p>取决于{@link BeanFactoryLocator}的实际实现, 以及{@code BeanFactory}的实际类型, 这实际上可能没有做任何事情;
	 * 'closeable' {@code BeanFactory}或派生类 (例如{@link org.springframework.context.ApplicationContext}) 可以关闭它,
	 * 或者在不再需要保持引用的时候可以关闭它.
	 * <p>在EJB使用场景中, 通常会从{@code ejbRemove()}和{@code ejbPassivate()}调用.
	 * <p>多次调用是安全的.
	 */
	void release();

}
