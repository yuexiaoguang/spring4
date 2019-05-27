package org.springframework.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.util.Assert;

/**
 * 静态工厂, 隐藏了ControlFlow实现类的自动选择.
 *
 * <p>此实现始终使用高效的Java 1.4 StackTraceElement机制来分析控制流.
 *
 * @deprecated as of Spring Framework 4.3.6
 */
@Deprecated
public abstract class ControlFlowFactory {

	/**
	 * 返回适当的{@link ControlFlow}实例.
	 */
	public static ControlFlow createControlFlow() {
		return new Jdk14ControlFlow();
	}


	/**
	 * 用于cflow样式切点.
	 * 请注意, 这些切点比其他切点要贵5-10倍, 因为它们需要分析堆栈跟踪 (通过构建新的throwable).
	 * 但是, 它们在某些情况下很有用.
	 * <p>此实现使用Java 1.4中引入的StackTraceElement类.
	 */
	static class Jdk14ControlFlow implements ControlFlow {

		private StackTraceElement[] stack;

		public Jdk14ControlFlow() {
			this.stack = new Throwable().getStackTrace();
		}

		/**
		 * 在StackTraceElement中搜索匹配的类名.
		 */
		@Override
		public boolean under(Class<?> clazz) {
			Assert.notNull(clazz, "Class must not be null");
			String className = clazz.getName();
			for (StackTraceElement element : this.stack) {
				if (element.getClassName().equals(className)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * 在StackTraceElement中搜索类名匹配和方法名匹配.
		 */
		@Override
		public boolean under(Class<?> clazz, String methodName) {
			Assert.notNull(clazz, "Class must not be null");
			Assert.notNull(methodName, "Method name must not be null");
			String className = clazz.getName();
			for (StackTraceElement element : this.stack) {
				if (element.getClassName().equals(className) &&
						element.getMethodName().equals(methodName)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * 将其留给调用者来决定匹配的内容.
		 * 调用者必须了解堆栈跟踪格式, 因此抽象较少.
		 */
		@Override
		public boolean underToken(String token) {
			if (token == null) {
				return false;
			}
			StringWriter sw = new StringWriter();
			new Throwable().printStackTrace(new PrintWriter(sw));
			String stackTrace = sw.toString();
			return stackTrace.contains(token);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Jdk14ControlFlow: ");
			for (int i = 0; i < this.stack.length; i++) {
				if (i > 0) {
					sb.append("\n\t@");
				}
				sb.append(this.stack[i]);
			}
			return sb.toString();
		}
	}

}
