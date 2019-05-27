package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ObjectFactory;

/**
 * {@link ConfigurableBeanFactory}使用的策略接口, 表示用于保存bean实例的目标作用域.
 * 这允许使用自定义的范围扩展BeanFactory的标准范围 {@link ConfigurableBeanFactory#SCOPE_SINGLETON "singleton"}和
 * {@link ConfigurableBeanFactory#SCOPE_PROTOTYPE "prototype"},
 * 使用 {@link ConfigurableBeanFactory#registerScope(String, Scope)特定的key}注册.
 *
 * <p>{@link org.springframework.context.ApplicationContext}实现,
 * 例如{@link org.springframework.web.context.WebApplicationContext}
 * 可以注册特定于其环境的其他标准范围,
 * e.g. {@link org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST "request"}
 * 和{@link org.springframework.web.context.WebApplicationContext#SCOPE_SESSION "session"},
 * 基于这个Scope SPI.
 *
 * <p>即使它主要用于Web环境中的扩展范围, 该SPI也是完全通用的:
 * 它提供了从任何底层存储机制获取和放置对象的能力, 例如HTTP会话或自定义会话机制.
 * 传递给此类的{@code get}和{@code remove}方法的名称将标识当前范围中的目标对象.
 *
 * <p>{@code Scope}实现应该是线程安全的.
 * 如果需要, 一个{@code Scope}实例可以同时与多个bean工厂一起使用 (除非它明确地想知道包含BeanFactory),
 * 任意数量的线程同时从任意数量的工厂访问{@code Scope}.
 */
public interface Scope {

	/**
	 * 从底层范围返回具有给定名称的对象,
	 * {@link org.springframework.beans.factory.ObjectFactory#getObject() creating it}
	 * 如果在底层存储机制中找不到.
	 * <p>这是Scope的核心操作, 也是唯一绝对需要的操作.
	 * 
	 * @param name 要检索的对象的名称
	 * @param objectFactory 用于创建作用域对象的{@link ObjectFactory}, 如果它不存在于底层存储机制中
	 * 
	 * @return 期望的对象 (never {@code null})
	 * @throws IllegalStateException 如果底层范围当前不可用
	 */
	Object get(String name, ObjectFactory<?> objectFactory);

	/**
	 * 从底层范围中删除具有给定{@code name}的对象.
	 * <p>如果没有找到对象, 则返回{@code null}; 否则返回删除的 {@code Object}.
	 * <p>请注意, 实现还应删除指定对象的已注册的销毁回调.
	 * 但是, 在这种情况下, 它确实不需要执行已注册的销毁回调, 因为对象将被调用者销毁.
	 * <p><b>Note: 这是一项可选的操作.</b> 如果实现不支持显式地删除对象, 则可能会抛出{@link UnsupportedOperationException}.
	 * 
	 * @param name 要删除的对象的名称
	 * 
	 * @return 删除的对象, 如果没有对象, 则为{@code null}
	 * @throws IllegalStateException 如果底层范围当前不可用
	 */
	Object remove(String name);

	/**
	 * 注册要在范围内销毁指定对象时执行的回调
	 * (或者在销毁整个范围时, 不销毁单个对象, 而只是完全终止).
	 * <p><b>Note: 这是一项可选的操作.</b>
	 * 仅对具有实际销毁配置的作用域bean调用此方法 (DisposableBean, destroy-method, DestructionAwareBeanPostProcessor).
	 * 实现应该尽力在适当的时间执行给定的回调.
	 * 如果底层运行时环境根本不支持这样的回调, 则必须忽略回调并记录相应的警告.
	 * <p>请注意, “销毁”是指对象的自动销毁作为作用域自身生命周期的一部分, 而不是应用程序显式地删除单个作用域对象.
	 * 如果通过此Facade的{@link #remove(String)}方法删除了作用域对象, 则应删除任何已注册的销毁回调, 假设删除的对象将被重用或手动销毁.
	 * 
	 * @param name 要执行销毁回调的对象的名称
	 * @param callback 要执行的销毁回调.
	 * 请注意, 传入的Runnable永远不会抛出异常, 因此可以安全地执行它而无需封闭的try-catch块.
	 * 此外, Runnable通常是可序列化的, 前提是它的目标对象也是可序列化的.
	 * 
	 * @throws IllegalStateException 如果底层范围当前不可用
	 */
	void registerDestructionCallback(String name, Runnable callback);

	/**
	 * 解析给定键的上下文对象. E.g. the HttpServletRequest object for key "request".
	 * 
	 * @param key 上下文 key
	 * 
	 * @return 对应的对象, 或{@code null}
	 * @throws IllegalStateException 如果底层范围当前不可用
	 */
	Object resolveContextualObject(String key);

	/**
	 * 返回当前底层范围的会话ID.
	 * <p>会话ID的确切含义取决于底层存储机制.
	 * 对于会话范围的对象, 会话ID通常等于 (或派生自) {@link javax.servlet.http.HttpSession#getId() 会话ID};
	 * 对于位于整个会话中的自定义会话, 当前会话的特定ID将是合适的.
	 * <p><b>Note: 这是一项可选的操作.</b>
	 * 如果底层存储机制没有这种ID的明显候选者, 那么在此方法的实现中返回{@code null}是完全有效的.
	 * 
	 * @return 会话ID; 如果当前范围没有会话ID, 则为{@code null}
	 * @throws IllegalStateException 如果底层范围当前不可用
	 */
	String getConversationId();

}
