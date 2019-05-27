package org.springframework.scripting.jruby;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyNil;
import org.jruby.ast.ClassNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于处理JRuby脚本对象的实用方法.
 *
 * <p>Note: Spring 4.0支持JRuby 1.5及更高版本, 建议使用1.7.x.
 * 从Spring 4.2开始, 也支持JRuby 9.0.0.0, 但主要是通过{@link org.springframework.scripting.support.StandardScriptFactory}.
 *
 * @deprecated in favor of JRuby support via the JSR-223 abstraction
 * ({@link org.springframework.scripting.support.StandardScriptFactory})
 */
@Deprecated
public abstract class JRubyScriptUtils {

	/**
	 * 使用默认的{@link ClassLoader}.
	 * 
	 * @param scriptSource 脚本源文本
	 * @param interfaces 脚本Java对象要实现的接口
	 * 
	 * @return 脚本Java对象
	 * @throws JumpException 在JRuby解析失败的情况下
	 */
	public static Object createJRubyObject(String scriptSource, Class<?>... interfaces) throws JumpException {
		return createJRubyObject(scriptSource, interfaces, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param scriptSource 脚本源文本
	 * @param interfaces 脚本Java对象要实现的接口
	 * @param classLoader 创建脚本代理使用的{@link ClassLoader}
	 * 
	 * @return 脚本Java对象
	 * @throws JumpException 在JRuby解析失败的情况下
	 */
	public static Object createJRubyObject(String scriptSource, Class<?>[] interfaces, ClassLoader classLoader) {
		Ruby ruby = initializeRuntime();

		Node scriptRootNode = ruby.parseEval(scriptSource, "", null, 0);
        IRubyObject rubyObject = ruby.runNormally(scriptRootNode);

		if (rubyObject instanceof RubyNil) {
			String className = findClassName(scriptRootNode);
			rubyObject = ruby.evalScriptlet("\n" + className + ".new");
		}
		// still null?
		if (rubyObject instanceof RubyNil) {
			throw new IllegalStateException("Compilation of JRuby script returned RubyNil: " + rubyObject);
		}

		return Proxy.newProxyInstance(classLoader, interfaces, new RubyObjectInvocationHandler(rubyObject, ruby));
	}

	/**
	 * 初始化{@link org.jruby.Ruby}运行时的实例.
	 */
	@SuppressWarnings("unchecked")
	private static Ruby initializeRuntime() {
		return JavaEmbedUtils.initialize(Collections.EMPTY_LIST);
	}

	/**
	 * 给定JRuby AST中的根{@link Node}将找到该AST定义的类的名称.
	 * 
	 * @throws IllegalArgumentException 如果提供的AST没有定义类
	 */
	private static String findClassName(Node rootNode) {
		ClassNode classNode = findClassNode(rootNode);
		if (classNode == null) {
			throw new IllegalArgumentException("Unable to determine class name for root node '" + rootNode + "'");
		}
		Colon2Node node = (Colon2Node) classNode.getCPath();
		return node.getName();
	}

	/**
	 * 在提供的{@link Node}下找到第一个{@link ClassNode}.
	 * 
	 * @return 对应的{@code ClassNode}, 或{@code null}
	 */
	private static ClassNode findClassNode(Node node) {
		if (node == null) {
			return null;
		}
		if (node instanceof ClassNode) {
			return (ClassNode) node;
		}
		List<Node> children = node.childNodes();
		for (Node child : children) {
			if (child instanceof ClassNode) {
				return (ClassNode) child;
			}
			else if (child instanceof NewlineNode) {
				NewlineNode nn = (NewlineNode) child;
				ClassNode found = findClassNode(nn.getNextNode());
				if (found != null) {
					return found;
				}
			}
		}
		for (Node child : children) {
			ClassNode found = findClassNode(child);
			if (found != null) {
				return found;
			}
		}
		return null;
	}


	/**
	 * 调用JRuby脚本方法的InvocationHandler.
	 */
	private static class RubyObjectInvocationHandler implements InvocationHandler {

		private final IRubyObject rubyObject;

		private final Ruby ruby;

		public RubyObjectInvocationHandler(IRubyObject rubyObject, Ruby ruby) {
			this.rubyObject = rubyObject;
			this.ruby = ruby;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				return (isProxyForSameRubyObject(args[0]));
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				return this.rubyObject.hashCode();
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				String toStringResult = this.rubyObject.toString();
				if (!StringUtils.hasText(toStringResult)) {
					toStringResult = ObjectUtils.identityToString(this.rubyObject);
				}
				return "JRuby object [" + toStringResult + "]";
			}
			try {
				IRubyObject[] rubyArgs = convertToRuby(args);
				IRubyObject rubyResult =
						this.rubyObject.callMethod(this.ruby.getCurrentContext(), method.getName(), rubyArgs);
				return convertFromRuby(rubyResult, method.getReturnType());
			}
			catch (RaiseException ex) {
				throw new JRubyExecutionException(ex);
			}
		}

		private boolean isProxyForSameRubyObject(Object other) {
			if (!Proxy.isProxyClass(other.getClass())) {
				return false;
			}
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			return (ih instanceof RubyObjectInvocationHandler &&
					this.rubyObject.equals(((RubyObjectInvocationHandler) ih).rubyObject));
		}

		private IRubyObject[] convertToRuby(Object[] javaArgs) {
			if (javaArgs == null || javaArgs.length == 0) {
				return new IRubyObject[0];
			}
			IRubyObject[] rubyArgs = new IRubyObject[javaArgs.length];
			for (int i = 0; i < javaArgs.length; ++i) {
				rubyArgs[i] = JavaEmbedUtils.javaToRuby(this.ruby, javaArgs[i]);
			}
			return rubyArgs;
		}

		private Object convertFromRuby(IRubyObject rubyResult, Class<?> returnType) {
			Object result = JavaEmbedUtils.rubyToJava(this.ruby, rubyResult, returnType);
			if (result instanceof RubyArray && returnType.isArray()) {
				result = convertFromRubyArray(((RubyArray) result).toJavaArray(), returnType);
			}
			return result;
		}

		private Object convertFromRubyArray(IRubyObject[] rubyArray, Class<?> returnType) {
			Class<?> targetType = returnType.getComponentType();
			Object javaArray = Array.newInstance(targetType, rubyArray.length);
			for (int i = 0; i < rubyArray.length; i++) {
				IRubyObject rubyObject = rubyArray[i];
				Array.set(javaArray, i, convertFromRuby(rubyObject, targetType));
			}
			return javaArray;
		}
	}


	/**
	 * 响应从JRuby方法调用抛出的JRuby {@link RaiseException}抛出的异常.
	 */
	@SuppressWarnings("serial")
	public static class JRubyExecutionException extends NestedRuntimeException {

		/**
		 * @param ex the cause (must not be {@code null})
		 */
		public JRubyExecutionException(RaiseException ex) {
			super(ex.getMessage(), ex);
		}
	}

}
