package org.springframework.transaction.support;

/**
 * 由资源保存器实现的通用接口.
 * 允许Spring的事务基础结构在必要时内省并重置保存器.
 */
public interface ResourceHolder {

	/**
	 * 重置此保存器的事务状态.
	 */
	void reset();

	/**
	 * 通知此保存器, 它已从事务同步中解除绑定.
	 */
	void unbound();

	/**
	 * 确定此保存器是否被视为'void', i.e. 作为前一个线程的剩余部分.
	 */
	boolean isVoid();

}
