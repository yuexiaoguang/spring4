package org.aopalliance.intercept;

/**
 * 此接口表示程序中的调用.
 *
 * <p>调用是一个连接点，可以被拦截器拦截.
 */
public interface Invocation extends Joinpoint {

	/**
	 * 将参数作为数组对象获取.
	 * 可以更改此数组中的元素值以更改参数.
	 * 
	 * @return 调用的参数
	 */
	Object[] getArguments();

}
