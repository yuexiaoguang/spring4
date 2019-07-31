package org.springframework.web.context.request;

/**
 * 用于访问与请求关联的属性对象的抽象.
 * 支持访问请求范围的属性以及会话范围的属性, 具有"全局会话"的可选概念.
 *
 * <p>可以为任何类型的请求/会话机制实现, 特别是对于servlet请求和portlet请求.
 */
public interface RequestAttributes {

	/**
	 * 表示请求范围的常量.
	 */
	int SCOPE_REQUEST = 0;

	/**
	 * 表示会话范围的常量.
	 * <p>如果可以区分, 则优选地指本地隔离的会话 (例如, 在Portlet环境中).
	 * 否则, 它只是指普通会话.
	 */
	int SCOPE_SESSION = 1;

	/**
	 * 指示全局会话范围的常量.
	 * <p>如果可以使用此区别(例如, 在Portlet环境中), 则显式引用全局共享会话.
	 * 否则, 它只是指普通会话.
	 */
	int SCOPE_GLOBAL_SESSION = 2;


	/**
	 * 请求对象的标准引用的名称: "request".
	 */
	String REFERENCE_REQUEST = "request";

	/**
	 * 会话对象的标准引用的名称: "session".
	 */
	String REFERENCE_SESSION = "session";


	/**
	 * 返回给定名称的scoped属性的值.
	 * 
	 * @param name 属性的名称
	 * @param scope 范围标识符
	 * 
	 * @return 当前属性值, 或{@code null}
	 */
	Object getAttribute(String name, int scope);

	/**
	 * 设置给定名称的scoped属性的值, 替换现有值.
	 * 
	 * @param name 属性的名称
	 * @param scope 范围标识符
	 * @param value 属性值
	 */
	void setAttribute(String name, Object value, int scope);

	/**
	 * 删除给定名称的scoped属性.
	 * <p>请注意, 实现还应删除指定属性的已注册销毁回调.
	 * 但是, 在这种情况下, 它确实<i>不</i>需要<i>执行</i>已注册的销毁回调, 因为对象将被调用者销毁.
	 * 
	 * @param name 属性的名称
	 * @param scope 范围标识符
	 */
	void removeAttribute(String name, int scope);

	/**
	 * 检索范围中所有属性的名称.
	 * 
	 * @param scope 范围标识符
	 * 
	 * @return 属性名称
	 */
	String[] getAttributeNames(int scope);

	/**
	 * 注册要在销毁给定范围内的指定属性时执行的回调.
	 * <p>实现应该尽力在适当的时候执行回调:
	 * 也就是说, 分别在请求完成或会话终止时.
	 * 如果底层运行时环境不支持这样的回调, 则必须<i>忽略</i>回调, 并记录相应的警告.
	 * <p>请注意, '销毁'通常对应于整个范围的销毁, 而不是应用程序已明确删除的单个属性.
	 * 如果通过此facade的{@link #removeAttribute(String, int)}方法删除属性, 则应禁用任何已注册的销毁回调,
	 * 假设已删除的对象将被重用或手动销毁.
	 * <p><b>NOTE:</b> 如果要为会话范围注册回调对象, 则它们通常应该是可序列化的.
	 * 否则, 回调(甚至整个会话)可能无法在Web应用程序重新启动后继续存在.
	 * 
	 * @param name 注册回调的属性的名称
	 * @param callback 要执行的销毁回调
	 * @param scope 范围标识符
	 */
	void registerDestructionCallback(String name, Runnable callback, int scope);

	/**
	 * 解析给定键的上下文引用.
	 * <p>至少: 键"request"的 HttpServletRequest/PortletRequest引用, 以及键"session"的HttpSession/PortletSession引用.
	 * 
	 * @param key 上下文键
	 * 
	 * @return 相应的对象, 或{@code null}
	 */
	Object resolveReference(String key);

	/**
	 * 返回当前底层会话的id.
	 * 
	 * @return 会话的id (never {@code null})
	 */
	String getSessionId();

	/**
	 * 为底层会话公开最佳可用互斥锁: 即, 用于底层会话同步的对象.
	 * 
	 * @return 要使用的会话互斥锁(never {@code null})
	 */
	Object getSessionMutex();

}
