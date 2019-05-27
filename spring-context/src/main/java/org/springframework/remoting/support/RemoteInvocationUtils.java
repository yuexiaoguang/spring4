package org.springframework.remoting.support;

import java.util.HashSet;
import java.util.Set;

/**
 * 用于处理远程调用的常规实用程序.
 *
 * <p>主要用于远程处理框架.
 */
public abstract class RemoteInvocationUtils {

	/**
	 * 将当前客户端堆栈跟踪填充到给定的异常中.
	 * <p>给定的异常通常在服务器上抛出并按原样序列化, 客户端希望它包含堆栈跟踪的客户端部分.
	 * 在这里可以做的是使用当前客户端堆栈跟踪更新{@code StackTraceElement}数组, 前提是运行在 JDK 1.4+.
	 * 
	 * @param ex 要更新的异常
	 */
	public static void fillInClientStackTraceIfPossible(Throwable ex) {
		if (ex != null) {
			StackTraceElement[] clientStack = new Throwable().getStackTrace();
			Set<Throwable> visitedExceptions = new HashSet<Throwable>();
			Throwable exToUpdate = ex;
			while (exToUpdate != null && !visitedExceptions.contains(exToUpdate)) {
				StackTraceElement[] serverStack = exToUpdate.getStackTrace();
				StackTraceElement[] combinedStack = new StackTraceElement[serverStack.length + clientStack.length];
				System.arraycopy(serverStack, 0, combinedStack, 0, serverStack.length);
				System.arraycopy(clientStack, 0, combinedStack, serverStack.length, clientStack.length);
				exToUpdate.setStackTrace(combinedStack);
				visitedExceptions.add(exToUpdate);
				exToUpdate = exToUpdate.getCause();
			}
		}
	}

}
