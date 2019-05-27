package org.springframework.remoting;

import org.springframework.core.NestedRuntimeException;

/**
 * 通用远程访问异常. 任何远程处理协议的服务代理都应抛出此异常或其子类, 以透明地公开纯Java业务接口.
 *
 * <p>使用一致性代理时, 切换实际的远程协议 e.g. 从Hessian到Burlap不会影响客户端代码.
 * 客户端使用服务公开的简单自然Java业务接口.
 * 客户端对象只是通过bean引用接收它所需的接口实现, 就像它对本地bean一样.
 *
 * <p>客户端可能会捕获RemoteAccessException, 但由于远程访问错误通常是不可恢复的,
 * 它可能会让这些异常传播到更高级别, 用以处理它们.
 * 在这种情况下, 客户端代码没有显示任何涉及远程访问的迹象, 因为没有任何特定于远程处理的依赖项.
 *
 * <p>即使从远程服务代理切换到同一接口的本地实现, 这也仅仅是配置问题.
 * 显然, 客户端代码应该有点意识到<i>可能正在对远程服务工作</i>, 例如在重复的方法调用方面导致不必要的往返等.
 * 但是, 它不必知道它的<i>实际工作</i>是针对远程服务还是本地实现, 或者它是在未知覆盖下工作的远程协议.
 */
public class RemoteAccessException extends NestedRuntimeException {

	/** Use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -4906825139312227864L;


	public RemoteAccessException(String msg) {
		super(msg);
	}

	/**
	 * @param msg the detail message
	 * @param cause the root cause (通常使用底层的远程处理API, 如RMI)
	 */
	public RemoteAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
