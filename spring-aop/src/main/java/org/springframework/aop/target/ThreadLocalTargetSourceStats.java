package org.springframework.aop.target;

/**
 * ThreadLocal TargetSource的统计信息.
 */
public interface ThreadLocalTargetSourceStats {

	/**
	 * 返回客户端调用的数量.
	 */
	int getInvocationCount();

	/**
	 * 返回线程绑定对象满足的命中数.
	 */
	int getHitCount();

	/**
	 * 返回创建的线程绑定对象的数量.
	 */
	int getObjectCount();

}
